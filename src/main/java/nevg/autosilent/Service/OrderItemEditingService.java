package nevg.autosilent.Service;

import nevg.autosilent.Models.Entity.*;
import nevg.autosilent.Models.Enums.*;
import nevg.autosilent.Repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.math.*;
import java.util.*;

@Service
public class OrderItemEditingService {
    private final OrderRepository orders;
    private final OrderItemRepository items;
    private final ProductRepository products;
    private final KitRepository kits;
    private final UserRepository users;
    private final OrderStatusHistoryRepository history;
    private final MessageSource messages;

    public OrderItemEditingService(OrderRepository orders, OrderItemRepository items, ProductRepository products,
            KitRepository kits, UserRepository users, OrderStatusHistoryRepository history, MessageSource messages) {
        this.orders = orders; this.items = items; this.products = products; this.kits = kits;
        this.users = users; this.history = history; this.messages = messages;
    }

    public static boolean editable(OrderStatus status, PaymentStatus payment) {
        return (status == OrderStatus.NEW || status == OrderStatus.CONFIRMED || status == OrderStatus.PROCESSING)
                && payment == PaymentStatus.PENDING;
    }

    public record Choice(Long id, String name, BigDecimal price, int stock) {}

    @Transactional(readOnly = true)
    public List<Choice> catalog() {
        return products.findAllByActiveTrueOrderByAddDateDesc().stream()
                .map(p -> new Choice(p.getId(), p.getDisplayName() + " · " + p.getSku(), p.getPrice(),
                        kits.findByCatalogProductId(p.getId()).map(Kit::availableStock).orElse(p.getStock())))
                .sorted(Comparator.comparing(Choice::name)).toList();
    }

    @Transactional
    public void add(Long orderId, Long productId, int quantity, String actor) {
        OrderEntity order = locked(orderId);
        User user = actor(actor);
        if (productId == null || quantity < 1) throw error("order.edit.invalid");
        Product product = products.findById(productId).filter(Product::isActive)
                .orElseThrow(() -> error("order.error.productUnavailable"));
        var allocation = allocation(product);
        changeStock(allocation, quantity, false);
        var item = new OrderItemEntity();
        item.setOrder(order); item.setProduct(product); item.setProductName(product.getDisplayName());
        item.setProductSku(product.getSku()); item.setQuantity(quantity); item.setUnitPrice(product.getPrice());
        item.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        item.getStockAllocation().putAll(allocation);
        items.saveAndFlush(item);
        recalculate(order);
        record(order, user, message("order.edit.added", product.getDisplayName(), quantity));
    }

    @Transactional
    public void remove(Long orderId, Long itemId, String actor) {
        OrderEntity order = locked(orderId);
        User user = actor(actor);
        var current = items.findByOrderId(orderId);
        var item = current.stream().filter(i -> i.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> error("order.edit.missingItem"));
        if (current.size() <= 1) throw error("order.edit.lastItem");
        var allocation = item.getStockAllocation();
        // Older orders have no allocation snapshot; use the original product definition.
        if (allocation.isEmpty() && item.getProduct() != null) allocation = allocation(item.getProduct());
        changeStock(allocation, item.getQuantity(), true);
        items.delete(item); items.flush();
        recalculate(order);
        record(order, user, message("order.edit.removed", item.getProductName(), item.getQuantity()));
    }

    private Map<Long, Integer> allocation(Product product) {
        Map<Long, Integer> result = new TreeMap<>();
        var kit = kits.findByCatalogProductId(product.getId());
        if (kit.isEmpty()) result.put(product.getId(), 1);
        else kit.get().getItems().forEach(component -> result.merge(component.getProduct().getId(),
                component.getQuantity(), Math::addExact));
        if (result.isEmpty()) throw error("order.edit.invalid");
        return result;
    }

    private void changeStock(Map<Long, Integer> allocation, int quantity, boolean restore) {
        for (Long id : allocation.keySet().stream().sorted().toList()) {
            var product = products.findByIdForStockUpdate(id);
            if (restore && product.isEmpty()) continue; // A deleted catalog product cannot be recreated here.
            var stock = product.orElseThrow(() -> error("order.error.productUnavailable"));
            try {
                int amount = Math.multiplyExact(allocation.get(id), quantity);
                if (amount < 1) throw error("order.edit.invalid");
                if (!restore && (!stock.isActive() || stock.getStock() < amount))
                    throw error("order.error.insufficientStock", stock.getDisplayName());
                stock.setStock(restore ? Math.addExact(stock.getStock(), amount) : stock.getStock() - amount);
            } catch (ArithmeticException exception) { throw error("order.error.quantityTooLarge"); }
        }
    }

    private void recalculate(OrderEntity order) {
        BigDecimal percent = order.getAppliedDiscountPercent();
        if (percent == null) {
            percent = order.getPromoDiscountPercent().signum() > 0 ? order.getPromoDiscountPercent()
                    : order.getSubtotalPrice().signum() > 0 ? order.getDiscountPrice().multiply(BigDecimal.valueOf(100))
                        .divide(order.getSubtotalPrice(), 6, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            order.setAppliedDiscountPercent(percent);
        }
        var currentItems = items.findByOrderId(order.getId());
        BigDecimal subtotal = currentItems.stream().map(OrderItemEntity::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal eligible = currentItems.stream().filter(item -> item.getPromotionId() == null)
                .map(OrderItemEntity::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setSubtotalPrice(subtotal);
        order.setDiscountPrice(eligible.multiply(percent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        order.setTotalPrice(subtotal.subtract(order.getDiscountPrice()).add(order.getDeliveryPrice()));
    }

    private OrderEntity locked(Long id) {
        var order = orders.findByIdForUpdate(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!editable(order.getOrderStatus(), order.getPaymentStatus())) throw error("order.edit.closed");
        return order;
    }
    private User actor(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
    private void record(OrderEntity order, User actor, String note) {
        var entry = new OrderStatusHistoryEntity(); entry.setOrder(order);
        entry.setOldStatus(order.getOrderStatus()); entry.setNewStatus(order.getOrderStatus());
        entry.setChangedByUser(actor); entry.setNote(note); history.save(entry);
    }
    private String message(String key, Object... args) { return messages.getMessage(key, args, LocaleContextHolder.getLocale()); }
    private IllegalArgumentException error(String key, Object... args) { return new IllegalArgumentException(message(key, args)); }
}
