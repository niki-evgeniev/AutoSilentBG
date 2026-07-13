package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.ContactInquiryDto;

public interface ContactInquiryService {

    void create(ContactInquiryDto inquiry, String ipAddress);
}
