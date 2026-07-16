package nevg.autosilent.Interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nevg.autosilent.Service.BannedUserService;
import nevg.autosilent.Service.ClientIpResolver;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

import java.security.Principal;
import java.util.Map;

@Component
public class BannedUserInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BannedUserInterceptor.class);

    private final ThymeleafViewResolver thymeleafViewResolver;
    private final BannedUserService bannedUserService;
    private final ClientIpResolver clientIpResolver;
    private final boolean enabled;

    public BannedUserInterceptor(ThymeleafViewResolver thymeleafViewResolver,
                                 BannedUserService bannedUserService,
                                 ClientIpResolver clientIpResolver,
                                 @Value("${AutoSilent.security.rate-limit.enabled:true}") boolean enabled) {
        this.thymeleafViewResolver = thymeleafViewResolver;
        this.bannedUserService = bannedUserService;
        this.clientIpResolver = clientIpResolver;
        this.enabled = enabled;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {
        if (!enabled) return true;

        String ipAddress = clientIpResolver.resolve(request);
        Principal principal = request.getUserPrincipal();
        String username = principal == null ? null : principal.getName();

        try {
            if (!bannedUserService.recordVisitAndCheckIfBanned(ipAddress, username)) return true;
        } catch (RuntimeException exception) {
            LOGGER.warn("IP ban check failed for {}. Allowing the request to preserve availability.",
                    ipAddress, exception);
            return true;
        }

        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setHeader("Retry-After", "900");
        View view = thymeleafViewResolver.resolveViewName(
                "bannedUser", RequestContextUtils.getLocale(request));
        if (view != null) {
            view.render(Map.of(), request, response);
        } else {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }
        return false;
    }
}
