package nevg.nirton.Service;

import nevg.nirton.Models.Dto.CartItemOrderDto;
import nevg.nirton.Models.Dto.CheckoutCustomerDto;
import nevg.nirton.Models.Dto.QuickOrderDto;

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
