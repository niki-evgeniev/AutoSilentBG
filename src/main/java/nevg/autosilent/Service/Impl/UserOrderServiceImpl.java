package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Dto.UserOrderSummaryDto;
import nevg.autosilent.Models.Entity.User;
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

    public UserOrderServiceImpl(UserRepository userRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
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
}
