package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Dto.AdminOrderItemDto;
import nevg.autosilent.Models.Dto.UserOrderDetailDto;
import nevg.autosilent.Models.Dto.UserOrderSummaryDto;
import nevg.autosilent.Models.Entity.OrderEntity;
import nevg.autosilent.Models.Entity.User;
import nevg.autosilent.Repository.OrderItemRepository;
import nevg.autosilent.Repository.OrderRepository;
import nevg.autosilent.Repository.UserRepository;
import nevg.autosilent.Service.UserOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserOrderServiceImpl implements UserOrderService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public UserOrderServiceImpl(UserRepository userRepository,
                                OrderRepository orderRepository,
                                OrderItemRepository orderItemRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserOrderSummaryDto> getOrders(String userEmail) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(order -> new UserOrderSummaryDto(
                        order.getOrderNumber(),
                        order.getOrderStatus(),
                        order.getTotalPrice(),
                        order.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserOrderDetailDto getOrder(String orderNumber, String userEmail) {
        User user = findUser(userEmail);
        OrderEntity order = orderRepository.findByOrderNumberAndUserId(orderNumber, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<AdminOrderItemDto> items = orderItemRepository.findByOrderId(order.getId()).stream()
                .map(item -> new AdminOrderItemDto(
                        item.getProductName(), item.getProductSku(), item.getQuantity(),
                        item.getUnitPrice(), item.getTotalPrice(), item.getProductImageUrl()))
                .toList();

        return new UserOrderDetailDto(
                order.getOrderNumber(), order.getOrderStatus(), order.getSubtotalPrice(),
                order.getDeliveryPrice(), order.getDiscountPrice(), order.getPromoCode(),
                order.getPromoDiscountPercent(), order.getTotalPrice(), order.getCreatedAt(), items);
    }

    private User findUser(String userEmail) {
        return userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
