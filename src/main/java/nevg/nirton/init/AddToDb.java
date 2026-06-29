package nevg.nirton.init;

import nevg.nirton.Service.ProductService;
import nevg.nirton.Service.UserRegistrationService;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AddToDb implements CommandLineRunner {

    private final UserRegistrationService userRegistrationService;
    private final ProductService productService;

    public AddToDb(UserRegistrationService userRegistrationService, ProductService productService) {
        this.userRegistrationService = userRegistrationService;
        this.productService = productService;
    }

    @Override
    public void run(String @NonNull ... args) throws Exception {
        userRegistrationService.addFirstAdminProfileAndAddRoles();
    }
}
