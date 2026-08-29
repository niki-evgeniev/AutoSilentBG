package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Dto.CartItemOrderDto;
import nevg.autosilent.Models.Dto.CheckoutCustomerDto;
import nevg.autosilent.Models.Dto.QuickOrderDto;
import nevg.autosilent.Models.Entity.*;
import nevg.autosilent.Models.Enums.DeliveryType;
import nevg.autosilent.Models.Enums.OrderStatus;
import nevg.autosilent.Models.Enums.PaymentMethod;
import nevg.autosilent.Models.Enums.PaymentStatus;
import nevg.autosilent.Repository.OrderItemRepository;
import nevg.autosilent.Repository.OrderRepository;
import nevg.autosilent.Repository.OrderStatusHistoryRepository;
import nevg.autosilent.Repository.ProductRepository;
import nevg.autosilent.Repository.UserRepository;
import nevg.autosilent.Service.Exception.OrderCreationException;
import nevg.autosilent.Service.OrderService;
import nevg.autosilent.Service.PromoCodeService;
import org.springframework.stereotype.Service;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PromoCodeService promoCodeService;
    private final MessageSource messageSource;

    public OrderServiceImpl(OrderRepository orderRepository,
                            OrderItemRepository orderItemRepository,
                            OrderStatusHistoryRepository statusHistoryRepository,
                            ProductRepository productRepository,
                            UserRepository userRepository,
                            PromoCodeService promoCodeService,
                            MessageSource messageSource) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.promoCodeService = promoCodeService;
        this.messageSource = messageSource;
    }

    @Override
    @Transactional(readOnly = true)
    public CheckoutCustomerDto getCheckoutCustomer(String userEmail) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new OrderCreationException(message("order.error.userNotFound")));
        return new CheckoutCustomerDto(
                user.getFirstName(), user.getLastName(), user.getEmail(), user.getPhoneNumber(),
                user.getDiscountPercent() == null ? BigDecimal.ZERO : user.getDiscountPercent());
    }

    @Override
    @Transactional
    public String createRegisteredOrder(String userEmail, String firstName, String lastName,
                                        String phone, String customerNote,
                                        List<CartItemOrderDto> items,
                                        String promoCode) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new OrderCreationException(message("order.error.userNotFound")));
        if (items == null || items.isEmpty()) {
            throw new OrderCreationException(message("order.error.emptyCart"));
        }

        OrderEntity order = newOrder();
        order.setUser(user);
        order.setCustomerFirstName(required(firstName, "order.error.firstNameRequired"));
        order.setCustomerLastName(required(lastName, "order.error.lastNameRequired"));
        order.setCustomerEmail(user.getEmail());
        order.setCustomerPhone(required(phone, "order.error.phoneRequired"));
        order.setCustomerNote(customerNote == null || customerNote.isBlank() ? null : customerNote.trim());
        return persistOrder(order, items, user.getDiscountPercent(), promoCode);
    }

    @Override
    @Transactional
    public String createGuestOrder(String email, String firstName, String lastName,
                                   String phone, String customerNote,
                                   List<CartItemOrderDto> items,
                                   String promoCode) {
        if (items == null || items.isEmpty()) {
            throw new OrderCreationException(message("order.error.emptyCart"));
        }

        OrderEntity order = newOrder();
        order.setCustomerFirstName(required(firstName, "order.error.firstNameRequired"));
        order.setCustomerLastName(required(lastName, "order.error.lastNameRequired"));
        order.setCustomerEmail(required(email, "order.error.emailRequired"));
        order.setCustomerPhone(required(phone, "order.error.phoneRequired"));
        order.setCustomerNote(customerNote == null || customerNote.isBlank() ? null : customerNote.trim());
        return persistOrder(order, items, BigDecimal.ZERO, promoCode);
    }

    @Override
    @Transactional
    public String createQuickOrder(QuickOrderDto request) {
        String[] names = request.names().trim().split("\\s+", 2);
        if (names.length < 2) {
            throw new OrderCreationException(message("order.error.twoNamesRequired"));
        }

        OrderEntity order = newOrder();
        order.setCustomerFirstName(names[0]);
        order.setCustomerLastName(names[1]);
        order.setCustomerEmail(request.email().trim());
        order.setCustomerPhone(request.phone().trim());
        return persistOrder(order, List.of(new CartItemOrderDto(request.productId(), request.quantity())),
                BigDecimal.ZERO, null);
    }

    private String persistOrder(OrderEntity order, List<CartItemOrderDto> requests,
                                BigDecimal customerDiscountPercent,
                                String promoCode) {
        List<PreparedItem> preparedItems = normalize(requests).stream().map(this::prepareItem).toList();
        BigDecimal subtotal = preparedItems.stream()
                .map(item -> item.product().getPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal promoPercent = promoCodeService.discountPercent(promoCode);
        String normalizedPromoCode = promoCodeService.normalizeCode(promoCode);
        BigDecimal percent = totalDiscountPercent(customerDiscountPercent, promoPercent);
        BigDecimal discount = subtotal.multiply(percent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        order.setSubtotalPrice(subtotal);
        order.setDiscountPrice(discount);
        order.setPromoCode(normalizedPromoCode.isBlank() ? null : normalizedPromoCode);
        order.setPromoDiscountPercent(promoPercent);
        order.setTotalPrice(subtotal.add(order.getDeliveryPrice()).subtract(order.getDiscountPrice()));
        orderRepository.save(order);

        List<OrderItemEntity> orderItems = preparedItems.stream()
                .map(item -> toOrderItem(order, item))
                .toList();
        orderItemRepository.saveAll(orderItems);

        preparedItems.forEach(item -> item.product().setStock(item.product().getStock() - item.quantity()));
        productRepository.saveAll(preparedItems.stream().map(PreparedItem::product).toList());

        OrderStatusHistoryEntity history = new OrderStatusHistoryEntity();
        history.setOrder(order);
        history.setNewStatus(OrderStatus.NEW);
        statusHistoryRepository.save(history);
        printNewOrder(order, orderItems);
        return order.getOrderNumber();
    }

    private void printNewOrder(OrderEntity order, List<OrderItemEntity> orderItems) {
        StringBuilder products = new StringBuilder();
        for (OrderItemEntity item : orderItems) {
            products.append(System.lineSeparator())
                    .append("  - ").append(item.getProductName())
                    .append(" | SKU: ").append(item.getProductSku())
                    .append(" | количество: ").append(item.getQuantity())
                    .append(" | единична цена: ").append(item.getUnitPrice())
                    .append(" | общо: ").append(item.getTotalPrice());
        }

        System.out.printf("""
                %n========== НОВА ПОРЪЧКА ==========%n
                Номер: %s
                Клиент: %s %s
                Имейл: %s
                Телефон: %s
                Тип клиент: %s
                Доставка: %s (цена: %s)
                Плащане: %s
                Статус на плащането: %s
                Статус на поръчката: %s
                Междинна сума: %s
                Отстъпка: %s
                Промокод: %s
                Процент от промокод: %s
                Обща сума: %s
                Бележка от клиента: %s
                Продукти:%s
                ==================================%n%n""",
                order.getOrderNumber(),
                order.getCustomerFirstName(), order.getCustomerLastName(),
                order.getCustomerEmail(), order.getCustomerPhone(),
                order.isGuestOrder() ? "гост" : "регистриран",
                order.getDeliveryType(), order.getDeliveryPrice(),
                order.getPaymentMethod(), order.getPaymentStatus(), order.getOrderStatus(),
                order.getSubtotalPrice(), order.getDiscountPrice(),
                order.getPromoCode(), order.getPromoDiscountPercent(), order.getTotalPrice(),
                order.getCustomerNote(), products);
    }

    private BigDecimal totalDiscountPercent(BigDecimal customerDiscountPercent, BigDecimal promoDiscountPercent) {
        BigDecimal customerPercent = customerDiscountPercent == null ? BigDecimal.ZERO : customerDiscountPercent;
        BigDecimal promoPercent = promoDiscountPercent == null ? BigDecimal.ZERO : promoDiscountPercent;
        if (promoPercent.signum() > 0) {
            return promoPercent;
        }
        return customerPercent.min(BigDecimal.valueOf(100));
    }

    private PreparedItem prepareItem(CartItemOrderDto request) {
        if (request == null || request.productId() == null || request.quantity() < 1) {
            throw new OrderCreationException(message("order.error.invalidItem"));
        }
        Product product = productRepository.findActiveByIdForUpdate(request.productId())
                .orElseThrow(() -> new OrderCreationException(message("order.error.productUnavailable")));
        if (product.getStock() < request.quantity()) {
            throw new OrderCreationException(message("order.error.insufficientStock", product.getNameProduct()));
        }
        return new PreparedItem(product, request.quantity());
    }

    private List<CartItemOrderDto> normalize(List<CartItemOrderDto> requests) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (CartItemOrderDto request : requests) {
            if (request == null || request.productId() == null || request.quantity() < 1) {
                throw new OrderCreationException(message("order.error.invalidItem"));
            }
            quantities.merge(request.productId(), request.quantity(), (current, added) -> {
                try {
                    return Math.addExact(current, added);
                } catch (ArithmeticException exception) {
                    throw new OrderCreationException(message("order.error.quantityTooLarge"));
                }
            });
        }
        return quantities.entrySet().stream()
                .map(entry -> new CartItemOrderDto(entry.getKey(), entry.getValue()))
                .toList();
    }

    private OrderEntity newOrder() {
        OrderEntity order = new OrderEntity();
        order.setOrderNumber(generateOrderNumber());
        order.setDeliveryType(DeliveryType.STORE_PICKUP);
        order.setDeliveryPrice(BigDecimal.ZERO);
        order.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setOrderStatus(OrderStatus.NEW);
        order.setDiscountPrice(BigDecimal.ZERO);
        return order;
    }

    private OrderItemEntity toOrderItem(OrderEntity order, PreparedItem item) {
        Product product = item.product();
        OrderItemEntity orderItem = new OrderItemEntity();
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setProductName(product.getDisplayName());
        orderItem.setProductSku(product.getSku());
        orderItem.setQuantity(item.quantity());
        orderItem.setUnitPrice(product.getPrice());
        orderItem.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(item.quantity())));
        orderItem.setProductImageUrl(mainImageUrl(product));
        return orderItem;
    }

    private String mainImageUrl(Product product) {
        return product.getPictures().stream()
                .filter(Picture::isMainImage)
                .findFirst()
                .map(picture -> "/ProductImages/" +
                        UriUtils.encodePathSegment(directoryName(product.getDisplayName()), StandardCharsets.UTF_8) + "/" +
                        UriUtils.encodePathSegment(picture.getFileName(), StandardCharsets.UTF_8))
                .orElse(null);
    }

    private String directoryName(String productName) {
        return Normalizer.normalize(productName.trim(), Normalizer.Form.NFC)
                .replaceAll("[<>:\"/\\\\|?*\\p{Cntrl}]", "-")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^[. -]+|[. -]+$", "");
    }

    private String generateOrderNumber() {
        return "NRT-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String required(String value, String messageKey) {
        if (value == null || value.isBlank()) throw new OrderCreationException(message(messageKey));
        return value.trim();
    }

    private String message(String code, Object... arguments) {
        return messageSource.getMessage(code, arguments, LocaleContextHolder.getLocale());
    }

    private record PreparedItem(Product product, int quantity) {
    }
}
