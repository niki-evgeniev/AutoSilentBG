package nevg.autosilent.Scheduler;

import nevg.autosilent.Service.SitemapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
public class SitemapScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SitemapScheduler.class);
    private final SitemapService sitemapService;
    private final Path directory;

    public SitemapScheduler(SitemapService sitemapService,
                            @Value("${AutoSilent.sitemap-directory:./sitemap}") String sitemapDirectory) {
        this.sitemapService = sitemapService;
        this.directory = Path.of(sitemapDirectory).toAbsolutePath().normalize();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void generateOnStartup() {
        createSitemap();
    }

    @Scheduled(cron = "0 0 */12 * * *", zone = "Europe/Sofia")
    public synchronized void createSitemap() {
        Path temporaryFile = null;
        try {
            Files.createDirectories(directory);
            temporaryFile = Files.createTempFile(directory, "sitemap-", ".xml.tmp");
            Files.writeString(temporaryFile, sitemapService.generateSitemap(), StandardCharsets.UTF_8);
            Path target = directory.resolve("sitemap.xml");
            try {
                Files.move(temporaryFile, target,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, target, StandardCopyOption.REPLACE_EXISTING);
            }
            LOGGER.info("Sitemap updated: {}", target);
        } catch (IOException exception) {
            throw new UncheckedIOException("Sitemap file could not be written in " + directory, exception);
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException exception) {
                    LOGGER.warn("Temporary sitemap file could not be removed: {}", temporaryFile, exception);
                }
            }
        }
    }
}
