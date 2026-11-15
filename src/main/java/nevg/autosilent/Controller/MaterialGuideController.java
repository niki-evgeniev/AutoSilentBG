package nevg.autosilent.Controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@RestController
public class MaterialGuideController {

    @GetMapping("/koe-da-izpolzvam")
    public ResponseEntity<Resource> guide() {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .body(new ClassPathResource("static/koe-da-izpolzvam.html"));
    }

    @GetMapping("/koe-da-izpolzvam.html")
    public ResponseEntity<Void> legacyGuide() {
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .location(URI.create("/koe-da-izpolzvam"))
                .build();
    }
}
