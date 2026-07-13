package nevg.autosilent.AppConfiguration;

import nevg.autosilent.Interceptor.BannedUserInterceptor;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class IpSecurityConfiguration implements WebMvcConfigurer {

    private final BannedUserInterceptor bannedUserInterceptor;

    public IpSecurityConfiguration(BannedUserInterceptor bannedUserInterceptor) {
        this.bannedUserInterceptor = bannedUserInterceptor;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(bannedUserInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/css/**", "/js/**", "/images/**", "/fonts/**",
                        "/ProductImages/**", "/favicon.ico", "/robots.txt", "/error");
    }
}
