package nevg.autosilent.AppConfiguration;

import org.springframework.beans.factory.annotation.Value;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class ProductImagesConfiguration implements WebMvcConfigurer {

    private final String sitemapDirectory;

    public ProductImagesConfiguration(@Value("${AutoSilent.sitemap-directory:./sitemap}") String sitemapDirectory) {
        this.sitemapDirectory = sitemapDirectory;
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        String location = Path.of("ProductImages").toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/ProductImages/**").addResourceLocations(location);

        String sitemapLocation = Path.of(sitemapDirectory).toAbsolutePath().normalize().toUri().toString();
        if (!sitemapLocation.endsWith("/")) {
            sitemapLocation += "/";
        }
        registry.addResourceHandler("/sitemap.xml")
                .addResourceLocations(sitemapLocation)
                .setCacheControl(CacheControl.noCache());
    }
}
