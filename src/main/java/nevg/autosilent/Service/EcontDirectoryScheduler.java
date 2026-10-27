package nevg.autosilent.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EcontDirectoryScheduler {
    private final EcontDirectoryService directory;
    private final boolean enabled;
    public EcontDirectoryScheduler(EcontDirectoryService directory,
            @Value("${AutoSilent.econt.sync-enabled:true}") boolean enabled) {
        this.directory = directory; this.enabled = enabled;
    }
    @Scheduled(initialDelay = 15000, fixedDelay = 86400000)
    public void initialize() {
        if (enabled && directory.status().getLastSuccess() == null) directory.refresh();
    }
    @Scheduled(cron = "${AutoSilent.econt.sync-cron:0 0 3 * * *}", zone = "Europe/Sofia")
    public void nightly() { if (enabled) directory.refresh(); }
}
