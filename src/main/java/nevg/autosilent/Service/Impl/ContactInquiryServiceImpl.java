package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Dto.ContactInquiryDto;
import nevg.autosilent.Models.Entity.ContactInquiry;
import nevg.autosilent.Repository.ContactInquiryRepository;
import nevg.autosilent.Service.ContactInquiryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactInquiryServiceImpl implements ContactInquiryService {

    private final ContactInquiryRepository repository;

    public ContactInquiryServiceImpl(ContactInquiryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void create(ContactInquiryDto request, String ipAddress) {
        ContactInquiry inquiry = new ContactInquiry();
        inquiry.setName(request.getName().trim());
        inquiry.setEmail(request.getEmail().trim().toLowerCase(java.util.Locale.ROOT));
        inquiry.setSubject(request.getSubject().trim());
        inquiry.setMessage(request.getMessage().trim());
        inquiry.setIpAddress(ipAddress);
        repository.save(inquiry);

        System.out.printf("""
                %n========== НОВО ЗАПИТВАНЕ ==========%n
                Име: %s
                Имейл: %s
                Тема: %s
                Съобщение: %s
                IP адрес: %s
                ====================================%n%n""",
                inquiry.getName(), inquiry.getEmail(), inquiry.getSubject(),
                inquiry.getMessage(), inquiry.getIpAddress());
    }
}
