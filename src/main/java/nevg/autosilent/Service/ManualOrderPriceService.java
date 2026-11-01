package nevg.autosilent.Service;

import nevg.autosilent.Models.Entity.OrderStatusHistoryEntity;
import nevg.autosilent.Repository.*;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.*;

@Service
public class ManualOrderPriceService {
    private final OrderRepository orders;
    private final UserRepository users;
    private final OrderStatusHistoryRepository history;
    private final MessageSource messages;
    private final OrderItemRepository items;

    public ManualOrderPriceService(OrderRepository orders, UserRepository users,
            OrderStatusHistoryRepository history, MessageSource messages, OrderItemRepository items) {
        this.orders = orders; this.users = users; this.history = history; this.messages = messages;
        this.items = items;
    }

    @Transactional
    public void update(Long id, BigDecimal total, String email) {
        var order = orders.findByIdForUpdate(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!OrderItemEditingService.editable(order.getOrderStatus(), order.getPaymentStatus())) throw error("order.edit.closed");
        var actor = users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        BigDecimal maximum = order.getSubtotalPrice().add(order.getDeliveryPrice());
        BigDecimal protectedAmount = items.findByOrderId(id).stream().filter(item -> item.getPromotionId() != null)
                .map(nevg.autosilent.Models.Entity.OrderItemEntity::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal eligible = order.getSubtotalPrice().subtract(protectedAmount);
        if (total != null && protectedAmount.signum() > 0 && total.compareTo(protectedAmount.add(order.getDeliveryPrice())) < 0)
            throw error("promotion.noDiscounts");
        if (total == null || total.scale() > 2 || total.compareTo(order.getDeliveryPrice().add(protectedAmount)) < 0
                || total.compareTo(maximum) > 0 || total.compareTo(new BigDecimal("99999999.99")) > 0) throw error("order.price.invalid");
        BigDecimal discount = maximum.subtract(total).setScale(2, RoundingMode.HALF_UP);
        BigDecimal percent = eligible.signum() > 0
                ? discount.multiply(BigDecimal.valueOf(100)).divide(eligible, 6, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal previous = order.getTotalPrice();
        order.setTotalPrice(total.setScale(2));
        order.setDiscountPrice(discount);
        order.setAppliedDiscountPercent(percent);
        // The manually agreed price replaces the previous discount, rather than stacking with a promo code.
        order.setPromoCode(null); order.setPromoDiscountPercent(BigDecimal.ZERO);
        var entry = new OrderStatusHistoryEntity(); entry.setOrder(order); entry.setChangedByUser(actor);
        entry.setOldStatus(order.getOrderStatus()); entry.setNewStatus(order.getOrderStatus());
        entry.setNote(messages.getMessage("order.price.history", new Object[]{previous.toPlainString(), total.toPlainString(), discount.toPlainString()}, LocaleContextHolder.getLocale()));
        history.save(entry);
    }
    private IllegalArgumentException error(String key) {
        return new IllegalArgumentException(messages.getMessage(key, null, LocaleContextHolder.getLocale()));
    }
}
