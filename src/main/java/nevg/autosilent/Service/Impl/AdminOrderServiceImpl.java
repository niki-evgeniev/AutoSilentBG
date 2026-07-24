package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Dto.*;
import nevg.autosilent.Models.Entity.OrderEntity;
import nevg.autosilent.Models.Entity.OrderStatusHistoryEntity;
import nevg.autosilent.Models.Entity.User;
import nevg.autosilent.Models.Enums.OrderStatus;
import nevg.autosilent.Models.Enums.PaymentStatus;
import nevg.autosilent.Repository.OrderItemRepository;
import nevg.autosilent.Repository.OrderRepository;
import nevg.autosilent.Repository.OrderStatusHistoryRepository;
import nevg.autosilent.Repository.UserRepository;
import nevg.autosilent.Service.AdminOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AdminOrderServiceImpl implements AdminOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final MessageSource messageSource;

    public AdminOrderServiceImpl(OrderRepository orderRepository,
                                 OrderItemRepository orderItemRepository,
                                 OrderStatusHistoryRepository historyRepository,
                                 UserRepository userRepository,
                                 MessageSource messageSource) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.messageSource = messageSource;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminOrderSummaryDto> getAllOrders() {
        return mapOrderSummaries(orderRepository.findAllByOrderByCreatedAtDesc());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminOrderSummaryDto> searchOrdersByNumber(String search) {
        return filterOrders(search, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminOrderSummaryDto> filterOrders(String search, OrderStatus status) {
        String normalizedSearch = search == null ? "" : search.trim();
        if (normalizedSearch.isBlank() && status == null) {
            return getAllOrders();
        }
        if (normalizedSearch.isBlank()) {
            return mapOrderSummaries(orderRepository.findByOrderStatusOrderByCreatedAtDesc(status));
        }
        if (status == null) {
            return mapOrderSummaries(orderRepository
                    .findByOrderNumberContainingIgnoreCaseOrderByCreatedAtDesc(normalizedSearch));
        }
        return mapOrderSummaries(orderRepository
                .findByOrderNumberContainingIgnoreCaseAndOrderStatusOrderByCreatedAtDesc(
                        normalizedSearch, status));
    }

    private List<AdminOrderSummaryDto> mapOrderSummaries(List<OrderEntity> orders) {
        return orders.stream()
                .map(order -> new AdminOrderSummaryDto(
                        order.getId(),
                        order.getOrderNumber(),
                        order.getCustomerFirstName() + " " + order.getCustomerLastName(),
                        order.getCustomerPhone(),
                        order.getOrderStatus(),
                        order.getTotalPrice(),
                        order.getCreatedAt(),
                        order.isGuestOrder()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminOrderDetailDto getOrder(Long orderId) {
        OrderEntity order = findOrder(orderId);
        List<AdminOrderItemDto> items = orderItemRepository.findByOrderId(orderId).stream()
                .map(item -> new AdminOrderItemDto(
                        item.getProductName(), item.getProductSku(), item.getQuantity(),
                        item.getUnitPrice(), item.getTotalPrice(), item.getProductImageUrl()))
                .toList();
        List<AdminOrderHistoryDto> history = historyRepository
                .findByOrderIdOrderByChangedAtDesc(orderId).stream()
                .map(entry -> new AdminOrderHistoryDto(
                        entry.getOldStatus(), entry.getNewStatus(),
                        entry.getChangedByUser() == null ? null : entry.getChangedByUser().getEmail(),
                        entry.getNote(), entry.getChangedAt()))
                .toList();

        return new AdminOrderDetailDto(
                order.getId(), order.getOrderNumber(),
                order.getCustomerFirstName(), order.getCustomerLastName(),
                order.getCustomerEmail(), order.getCustomerPhone(), order.isGuestOrder(),
                order.getDeliveryType(), order.getPaymentMethod(), order.getPaymentStatus(),
                order.getOrderStatus(), order.getSubtotalPrice(), order.getDeliveryPrice(),
                order.getDiscountPrice(), order.getPromoCode(), order.getPromoDiscountPercent(),
                order.getTotalPrice(), order.getCustomerNote(),
                order.getAdminNote(), order.getCreatedAt(), items, history);
    }

    @Override
    @Transactional
    public void updateStatus(Long orderId, OrderStatus newStatus, String note, String changedByEmail) {
        OrderEntity order = findOrder(orderId);
        if (order.getOrderStatus() == newStatus) return;

        User changedBy = userRepository.findByEmailIgnoreCase(changedByEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        OrderStatus oldStatus = order.getOrderStatus();
        order.setOrderStatus(newStatus);
        if (newStatus == OrderStatus.DELIVERED) order.setPaymentStatus(PaymentStatus.PAID);
        if (newStatus == OrderStatus.RETURNED) order.setPaymentStatus(PaymentStatus.REFUNDED);
        if (note != null && !note.isBlank()) order.setAdminNote(note.trim());
        orderRepository.save(order);

        OrderStatusHistoryEntity history = new OrderStatusHistoryEntity();
        history.setOrder(order);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setChangedByUser(changedBy);
        history.setNote(note == null || note.isBlank() ? null : note.trim());
        historyRepository.save(history);
    }

    private OrderEntity findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        messageSource.getMessage("admin.order.notFound", null, LocaleContextHolder.getLocale())));
    }
}
