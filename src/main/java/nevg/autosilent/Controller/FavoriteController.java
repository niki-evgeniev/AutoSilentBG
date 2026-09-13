package nevg.autosilent.Controller;

import nevg.autosilent.Models.Security.ShopUserDetails;
import nevg.autosilent.Service.FavoriteService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@PreAuthorize("isAuthenticated()")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping("/favorites")
    public ModelAndView favorites(@AuthenticationPrincipal ShopUserDetails user) {
        ModelAndView result = new ModelAndView("favorites");
        result.addObject("products", favoriteService.getFavorites(user.getUsername()));
        return result;
    }

    @PostMapping("/favorites/{productId}/toggle")
    public ModelAndView toggle(@PathVariable Long productId,
                               @RequestParam(defaultValue = "favorites") String source,
                               @AuthenticationPrincipal ShopUserDetails user) {
        favoriteService.toggle(user.getUsername(), productId);
        return new ModelAndView("redirect:" + redirectTarget(source, productId));
    }

    private String redirectTarget(String source, Long productId) {
        return switch (source) {
            case "products" -> "/shumoizolaciya";
            case "details" -> "/shumoizolaciya/" + productId;
            default -> "/favorites";
        };
    }
}
