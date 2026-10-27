package nevg.autosilent.Controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import nevg.autosilent.Models.Dto.ProductQuestionDto;
import nevg.autosilent.Service.ClientIpResolver;
import nevg.autosilent.Service.ProductQuestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ProductQuestionController {
    private final ProductQuestionService service;
    private final ClientIpResolver ips;
    public ProductQuestionController(ProductQuestionService service, ClientIpResolver ips) {
        this.service = service; this.ips = ips;
    }
    @PostMapping("/products/{id}/question")
    public ResponseEntity<Void> submit(@PathVariable Long id, @Valid @RequestBody ProductQuestionDto question,
            HttpServletRequest request) {
        service.create(id, question, ips.resolve(request));
        return ResponseEntity.noContent().build();
    }
}
