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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

import java.math.BigDecimal;
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

    public OrderServiceImpl(OrderRepository orderRepository,
                            OrderItemRepository orderItemRepository,
                            OrderStatusHistoryRepository statusHistoryRepository,
                            ProductRepository productRepository,
                            UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CheckoutCustomerDto getCheckoutCustomer(String userEmail) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new OrderCreationException("Потребителят не е намерен."));
        return new CheckoutCustomerDto(
                user.getFirstName(), user.getLastName(), user.getEmail(), user.getPhoneNumber());
    }

    @Override
    @Transactional
    public String createRegisteredOrder(String userEmail, String firstName, String lastName,
                                        String phone, String customerNote,
                                        List<CartItemOrderDto> items) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new OrderCreationException("Потребителят не е намерен."));
        if (items == null || items.isEmpty()) {
            throw new OrderCreationException("Кошницата е празна.");
        }

        OrderEntity order = newOrder();
        order.setUser(user);
        order.setCustomerFirstName(required(firstName, "Името е задължително."));
        order.setCustomerLastName(required(lastName, "Фамилията е задължителна."));
        order.setCustomerEmail(user.getEmail());
        order.setCustomerPhone(required(phone, "Телефонът е задължителен."));
        order.setCustomerNote(customerNote == null || customerNote.isBlank() ? null : customerNote.trim());
        return persistOrder(order, items);
    }

    @Override
    @Transactional
    public String createQuickOrder(QuickOrderDto request) {
        String[] names = request.names().trim().split("\\s+", 2);
        if (names.length < 2) {
            throw new OrderCreationException("Въведете две имена.");
        }

        OrderEntity order = newOrder();
        order.setCustomerFirstName(names[0]);
        order.setCustomerLastName(names[1]);
        order.setCustomerEmail(request.email().trim());
        order.setCustomerPhone(request.phone().trim());
        return persistOrder(order, List.of(new CartItemOrderDto(request.productId(), request.quantity())));
    }

    private String persistOrder(OrderEntity order, List<CartItemOrderDto> requests) {
        List<PreparedItem> preparedItems = normalize(requests).stream().map(this::prepareItem).toList();
        BigDecimal subtotal = preparedItems.stream()
                .map(item -> item.product().getPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setSubtotalPrice(subtotal);
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
            throw new OrderCreationException("Невалиден продукт или количество.");
        }
        Product product = productRepository.findActiveByIdForUpdate(request.productId())
                .orElseThrow(() -> new OrderCreationException("Продуктът вече не е наличен."));
        if (product.getStock() < request.quantity()) {
            throw new OrderCreationException("Недостатъчна наличност за „" + product.getNameProduct() + "“.");
        }
        return new PreparedItem(product, request.quantity());
    }

    private List<CartItemOrderDto> normalize(List<CartItemOrderDto> requests) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (CartItemOrderDto request : requests) {
            if (request == null || request.productId() == null || request.quantity() < 1) {
                throw new OrderCreationException("Невалиден продукт или количество.");
            }
            quantities.merge(request.productId(), request.quantity(), (current, added) -> {
                try {
                    return Math.addExact(current, added);
                } catch (ArithmeticException exception) {
                    throw new OrderCreationException("Количеството е прекалено голямо.");
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

    private String required(String value, String message) {
        if (value == null || value.isBlank()) throw new OrderCreationException(message);
        return value.trim();
    }

    private record PreparedItem(Product product, int quantity) {
    }
}
