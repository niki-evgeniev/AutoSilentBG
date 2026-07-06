package nevg.nirton.Controller;

import nevg.nirton.Service.FavoriteService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Set;

@ControllerAdvice
public class FavoriteModelAdvice {

    private final FavoriteService favoriteService;

    public FavoriteModelAdvice(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @ModelAttribute("favoriteProductIds")
    public Set<Long> favoriteProductIds(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Set.of();
        }
        return favoriteService.getFavoriteProductIds(authentication.getName());
    }
}
