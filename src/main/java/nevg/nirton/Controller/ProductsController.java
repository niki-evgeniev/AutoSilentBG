package nevg.nirton.Controller;


import jakarta.validation.Valid;
import nevg.nirton.Models.Dto.ProductCreateDto;
import nevg.nirton.Models.Security.ShopUserDetails;
import nevg.nirton.Service.Exception.InvalidProductImageException;
import nevg.nirton.Service.Exception.ProductAlreadyExistsException;
import nevg.nirton.Service.Exception.ProductCreationException;
import nevg.nirton.Service.ProductService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Objects;

@Controller
public class ProductsController {

    private final ProductService productService;

    public ProductsController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    public ModelAndView products (){
        ModelAndView modelAndView = new ModelAndView("products");
        modelAndView.addObject("products", productService.getActiveProducts());
        return modelAndView;
    }

    @GetMapping("/products/add")
    public ModelAndView addProduct() {
        return productForm(new ProductCreateDto());
    }

    @PostMapping("/products/add")
    public ModelAndView addProduct(@Valid @ModelAttribute("product") ProductCreateDto product,
                                   BindingResult bindingResult,
                                   @AuthenticationPrincipal ShopUserDetails currentUser,
                                   RedirectAttributes redirectAttributes) {
        if (product.getMainImage() == null || product.getMainImage().isEmpty()) {
            bindingResult.rejectValue("mainImage", "image.required", "Главната снимка е задължителна.");
        }
        long additionalImageCount = product.getAdditionalImages().stream()
                .filter(Objects::nonNull)
                .filter(image -> !image.isEmpty())
                .count();
        if (additionalImageCount > 4) {
            bindingResult.rejectValue("additionalImages", "images.limit",
                    "Можете да добавите най-много 4 допълнителни снимки.");
        }

        if (bindingResult.hasErrors()) {
            return productForm(product);
        }

        try {
            productService.create(product, currentUser.getUsername());
        } catch (ProductAlreadyExistsException exception) {
            bindingResult.rejectValue(exception.getField(), "product.exists", exception.getMessage());
            return productForm(product);
        } catch (InvalidProductImageException exception) {
            bindingResult.reject("images.invalid", exception.getMessage());
            return productForm(product);
        } catch (ProductCreationException exception) {
            bindingResult.reject("product.persistence", exception.getMessage());
            return productForm(product);
        }

        redirectAttributes.addFlashAttribute("productSuccess", "Продуктът е добавен успешно.");
        return new ModelAndView("redirect:/products/add");
    }

    private ModelAndView productForm(ProductCreateDto product) {
        ModelAndView modelAndView = new ModelAndView("add-product");
        modelAndView.addObject("product", product);
        return modelAndView;
    }
}
