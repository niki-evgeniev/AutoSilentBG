package nevg.autosilent.AppConfiguration;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.UrlHandlerFilter;

@Configuration
public class TrailingSlashConfiguration {

    @Bean
    public FilterRegistrationBean<UrlHandlerFilter> trailingSlashFilter() {
        UrlHandlerFilter filter = UrlHandlerFilter
                .trailingSlashHandler("/**")
                .redirect(HttpStatus.PERMANENT_REDIRECT)
                .build();

        FilterRegistrationBean<UrlHandlerFilter> registration =
                new FilterRegistrationBean<>(filter);
        // After forwarded-header handling and before Spring Security (default order -100).
        registration.setOrder(-101);
        return registration;
    }
}
