package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.CartItemOrderDto;
import nevg.autosilent.Models.Dto.CheckoutCustomerDto;
import nevg.autosilent.Models.Dto.QuickOrderDto;

import java.util.List;

public interface OrderService {

    CheckoutCustomerDto getCheckoutCustomer(String userEmail);

    String createRegisteredOrder(String userEmail, String firstName, String lastName,
                                 String phone, String customerNote,
                                 List<CartItemOrderDto> items);

    String createGuestOrder(String email, String firstName, String lastName,
                            String phone, String customerNote,
                            List<CartItemOrderDto> items);

    String createQuickOrder(QuickOrderDto request);
}
