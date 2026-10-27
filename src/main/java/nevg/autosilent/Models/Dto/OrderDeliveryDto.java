package nevg.autosilent.Models.Dto;

import nevg.autosilent.Models.Entity.OrderEntity;
import nevg.autosilent.Models.Enums.DeliveryType;

public record OrderDeliveryDto(String city, String postcode, String address,
        String officeName, String officeCode, DeliveryType type) {
    public static OrderDeliveryDto from(OrderEntity order) {
        var address = order.getAddress();
        return address == null ? null : new OrderDeliveryDto(address.getCity(), address.getPostcode(),
                address.getAddressLine(), address.getCourierOfficeName(), address.getCourierOfficeCode(), order.getDeliveryType());
    }
}
