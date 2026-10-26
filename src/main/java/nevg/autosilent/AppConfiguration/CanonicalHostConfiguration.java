package nevg.autosilent.AppConfiguration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@Profile("prod")
public class CanonicalHostConfiguration {

    private static final String CANONICAL_ORIGIN = "https://autosilent.bg";

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> canonicalHostFilter() {
        OncePerRequestFilter filter = new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain) throws ServletException, IOException {
                String forwardedProto = request.getHeader("X-Forwarded-Proto");
                String scheme = forwardedProto == null || forwardedProto.isBlank()
                        ? request.getScheme() : forwardedProto.split(",", 2)[0].trim();
                String forwardedHost = request.getHeader("X-Forwarded-Host");
                String host = forwardedHost == null || forwardedHost.isBlank()
                        ? request.getServerName() : forwardedHost.split(",", 2)[0].trim();
                if (!"https".equalsIgnoreCase(scheme) || !"autosilent.bg".equalsIgnoreCase(host)) {
                    String path = request.getRequestURI();
                    String query = request.getQueryString();
                    response.setStatus(HttpServletResponse.SC_PERMANENT_REDIRECT);
                    response.setHeader("Location", CANONICAL_ORIGIN + path
                            + (query == null || query.isEmpty() ? "" : "?" + query));
                    return;
                }
                chain.doFilter(request, response);
            }
        };
        FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(-102);
        return registration;
    }
}
