package nevg.autosilent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AutoSilentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutoSilentApplication.class, args);
    }

}
