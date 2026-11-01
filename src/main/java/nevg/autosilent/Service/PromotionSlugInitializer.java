package nevg.autosilent.Service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class PromotionSlugInitializer implements ApplicationRunner {
    private final PromotionService promotions;
    public PromotionSlugInitializer(PromotionService promotions) { this.promotions = promotions; }
    @Override public void run(ApplicationArguments args) { promotions.initializeSlugs(); }
}
