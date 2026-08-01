package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Dto.ContactInquiryDto;
import nevg.autosilent.Models.Dto.ProductReturnRequestDto;
import nevg.autosilent.Service.ContactInquiryService;
import nevg.autosilent.Service.ProductReturnService;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class ProductReturnServiceImpl implements ProductReturnService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private final ContactInquiryService contactInquiryService;

    public ProductReturnServiceImpl(ContactInquiryService contactInquiryService) {
        this.contactInquiryService = contactInquiryService;
    }

    @Override
    public void create(ProductReturnRequestDto request, String ipAddress) {
        String firstName = request.getFirstName().trim();
        String lastName = request.getLastName().trim();
        String orderNumber = request.getOrderNumber().trim().toUpperCase(Locale.ROOT);

        ContactInquiryDto inquiry = new ContactInquiryDto();
        inquiry.setName(firstName + " " + lastName);
        inquiry.setEmail(request.getEmail().trim());
        inquiry.setSubject("[RETURN] Order " + orderNumber);
        inquiry.setMessage(String.join(System.lineSeparator(),
                "Order number: " + orderNumber,
                "Ordered on: " + DATE_FORMAT.format(request.getOrderedOn()),
                "Phone: " + request.getPhone().trim(),
                "Product name/model: " + request.getProductName().trim(),
                "Product code: " + request.getProductCode().trim(),
                "Quantity: " + request.getQuantity(),
                "Requested resolution: " + request.getResolution(),
                "",
                "Reason for return:",
                request.getReason().trim()));

        contactInquiryService.create(inquiry, ipAddress);
    }
}
