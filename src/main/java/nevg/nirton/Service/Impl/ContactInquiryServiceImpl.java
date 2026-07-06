package nevg.nirton.Service.Impl;

import nevg.nirton.Models.Dto.ContactInquiryDto;
import nevg.nirton.Models.Entity.ContactInquiry;
import nevg.nirton.Repository.ContactInquiryRepository;
import nevg.nirton.Service.ContactInquiryService;
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
    }
}
