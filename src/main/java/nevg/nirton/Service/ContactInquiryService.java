package nevg.nirton.Service;

import nevg.nirton.Models.Dto.ContactInquiryDto;

public interface ContactInquiryService {
    void create(ContactInquiryDto inquiry, String ipAddress);
}
