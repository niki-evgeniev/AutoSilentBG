package nevg.autosilent.AppConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationProfilesTest {

    private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

    @Test
    void developmentIsTheDefaultProfile() throws IOException {
        assertThat(property("application.yaml", "spring.profiles.default")).isEqualTo("dev");
        assertThat(property("application-dev.yaml", "spring.config.activate.on-profile"))
                .isEqualTo("dev");
        assertThat(property("application-dev.yaml", "server.port")).isEqualTo(8080);
    }

    @Test
    void productionProfileUsesPort8585AndProductionCaching() throws IOException {
        assertThat(property("application-prod.yaml", "spring.config.activate.on-profile"))
                .isEqualTo("prod");
        assertThat(property("application-prod.yaml", "server.port")).isEqualTo(8585);
        assertThat(property(
                "application-prod.yaml",
                "spring.web.resources.cache.cachecontrol.cache-public")).isEqualTo(true);
        assertThat(property("application-prod.yaml", "spring.thymeleaf.cache")).isEqualTo(true);
    }

    private Object property(String resource, String key) throws IOException {
        List<PropertySource<?>> sources =
                loader.load(resource, new FileSystemResource(
                        "src/main/resources/" + resource));
        assertThat(sources).isNotEmpty();
        return sources.getFirst().getProperty(key);
    }
}
