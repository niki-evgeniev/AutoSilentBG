package nevg.nirton.Service.Impl;

import nevg.nirton.Models.Dto.CartItemOrderDto;
import nevg.nirton.Models.Dto.CheckoutCustomerDto;
import nevg.nirton.Models.Dto.QuickOrderDto;
import nevg.nirton.Models.Entity.*;
import nevg.nirton.Models.Enums.DeliveryType;
import nevg.nirton.Models.Enums.OrderStatus;
import nevg.nirton.Models.Enums.PaymentMethod;
import nevg.nirton.Models.Enums.PaymentStatus;
import nevg.nirton.Repository.OrderItemRepository;
import nevg.nirton.Repository.OrderRepository;
import nevg.nirton.Repository.OrderStatusHistoryRepository;
import nevg.nirton.Repository.ProductRepository;
import nevg.nirton.Repository.UserRepository;
import nevg.nirton.Service.Exception.OrderCreationException;
import nevg.nirton.Service.OrderService;
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
    private final MessageSource messageSource;

    public OrderServiceImpl(OrderRepository orderRepository,
                            OrderItemRepository orderItemRepository,
                            OrderStatusHistoryRepository statusHistoryRepository,
                            ProductRepository productRepository,
                            UserRepository userRepository,
                            MessageSource messageSource) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
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
                                        List<CartItemOrderDto> items) {
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
        return persistOrder(order, items, user.getDiscountPercent());
    }

    @Override
    @Transactional
    public String createGuestOrder(String email, String firstName, String lastName,
                                   String phone, String customerNote,
                                   List<CartItemOrderDto> items) {
        if (items == null || items.isEmpty()) {
            throw new OrderCreationException(message("order.error.emptyCart"));
        }

        OrderEntity order = newOrder();
        order.setCustomerFirstName(required(firstName, "order.error.firstNameRequired"));
        order.setCustomerLastName(required(lastName, "order.error.lastNameRequired"));
        order.setCustomerEmail(required(email, "order.error.emailRequired"));
        order.setCustomerPhone(required(phone, "order.error.phoneRequired"));
        order.setCustomerNote(customerNote == null || customerNote.isBlank() ? null : customerNote.trim());
        return persistOrder(order, items, BigDecimal.ZERO);
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
        return persistOrder(order, List.of(new CartItemOrderDto(request.productId(), request.quantity())), BigDecimal.ZERO);
    }

    private String persistOrder(OrderEntity order, List<CartItemOrderDto> requests, BigDecimal discountPercent) {
        List<PreparedItem> preparedItems = normalize(requests).stream().map(this::prepareItem).toList();
        BigDecimal subtotal = preparedItems.stream()
                .map(item -> item.product().getPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal percent = discountPercent == null ? BigDecimal.ZERO : discountPercent;
        BigDecimal discount = subtotal.multiply(percent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        order.setSubtotalPrice(subtotal);
        order.setDiscountPrice(discount);
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
        return order.getOrderNumber();
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
        orderItem.setProductName(product.getNameProduct());
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
                        UriUtils.encodePathSegment(directoryName(product.getNameProduct()), StandardCharsets.UTF_8) + "/" +
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
