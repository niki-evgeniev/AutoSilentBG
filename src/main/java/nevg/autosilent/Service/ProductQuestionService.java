package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.*;
import nevg.autosilent.Repository.ContactInquiryRepository;
import nevg.autosilent.Repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;

@Service
public class ProductQuestionService {
    private final ContactInquiryService inquiries;
    private final ContactInquiryRepository repository;
    private final ProductRepository products;

    public ProductQuestionService(ContactInquiryService inquiries, ContactInquiryRepository repository,
            ProductRepository products) {
        this.inquiries = inquiries; this.repository = repository; this.products = products;
    }

    @Transactional
    public synchronized void create(Long productId, ProductQuestionDto request, String ip) {
        var product = products.findById(productId).filter(p -> p.isActive())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (repository.existsByIpAddressAndSubjectStartingWithAndCreatedAtAfter(ip, "[QUESTION]", LocalDateTime.now().minusMinutes(1))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        }
        var inquiry = new ContactInquiryDto();
        inquiry.setName(request.name().trim()); inquiry.setEmail(request.email().trim());
        String subject = "[QUESTION] " + product.getDisplayName();
        inquiry.setSubject(subject.substring(0, Math.min(150, subject.length())));
        inquiry.setMessage("Product: " + product.getDisplayName() + "\nProduct URL: /shumoizolaciya/" + product.getUrl()
                + "\nSKU: " + product.getSku() + "\nPhone: " + request.phone().trim() + "\n\n" + request.question().trim());
        inquiries.create(inquiry, ip);
    }
}
