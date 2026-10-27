package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.CartOrderDto;
import nevg.autosilent.Models.Enums.DeliveryType;
import nevg.autosilent.Repository.OrderRepository;
import nevg.autosilent.Repository.OrderAddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutOrderService {
    private final OrderService orders;
    private final EcontDirectoryService directory;
    private final OrderRepository repository;
    private final OrderAddressRepository addresses;

    public CheckoutOrderService(OrderService orders, EcontDirectoryService directory,
            OrderRepository repository, OrderAddressRepository addresses) {
        this.orders = orders; this.directory = directory; this.repository = repository; this.addresses = addresses;
    }

    @Transactional
    public String create(CartOrderDto request, String userEmail) {
        var address = directory.resolve(request.delivery());
        String number = userEmail == null
                ? orders.createGuestOrder(request.email(), request.firstName(), request.lastName(), request.phone(),
                        request.customerNote(), request.items(), request.promoCode())
                : orders.createRegisteredOrder(userEmail, request.firstName(), request.lastName(), request.phone(),
                        request.customerNote(), request.items(), request.promoCode());
        var order = repository.findByOrderNumber(number).orElseThrow();
        order.setDeliveryType("OFFICE".equals(request.delivery().type())
                ? DeliveryType.ECONT_OFFICE : DeliveryType.ECONT_ADDRESS);
        address.setOrder(order);
        order.setAddress(address);
        addresses.save(address);
        return number;
    }
}
