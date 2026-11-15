package nevg.autosilent.Controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
public class LlmsController {

    @GetMapping(value = "/llms.txt", produces = "text/plain;charset=UTF-8")
    public ResponseEntity<Resource> llms() {
        // Plain text has no HTML meta tag: declare the encoding in the HTTP header.
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .cacheControl(CacheControl.noCache())
                .body(new ClassPathResource("static/llms.txt"));
    }
}
