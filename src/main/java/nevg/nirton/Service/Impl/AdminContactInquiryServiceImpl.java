package nevg.nirton.Service.Impl;

import nevg.nirton.Models.Dto.AdminContactInquiryDto;
import nevg.nirton.Models.Entity.ContactInquiry;
import nevg.nirton.Repository.ContactInquiryRepository;
import nevg.nirton.Service.AdminContactInquiryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminContactInquiryServiceImpl implements AdminContactInquiryService {

    private static final int PAGE_SIZE = 30;
    private final ContactInquiryRepository repository;

    public AdminContactInquiryServiceImpl(ContactInquiryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminContactInquiryDto> getAll(int page) {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(page, 0), PAGE_SIZE))
                .map(this::toDto);
    }

    @Override
    @Transactional
    public AdminContactInquiryDto getAndMarkAsRead(Long id) {
        ContactInquiry inquiry = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!inquiry.isRead()) {
            inquiry.setRead(true);
            repository.save(inquiry);
        }
        return toDto(inquiry);
    }

    private AdminContactInquiryDto toDto(ContactInquiry inquiry) {
        return new AdminContactInquiryDto(inquiry.getId(), inquiry.getName(), inquiry.getEmail(),
                inquiry.getSubject(), inquiry.getMessage(), inquiry.getIpAddress(),
                inquiry.getCreatedAt(), inquiry.isRead());
    }
}
