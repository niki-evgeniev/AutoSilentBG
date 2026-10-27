package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.CheckoutDeliveryDto;
import nevg.autosilent.Models.Entity.*;
import nevg.autosilent.Repository.*;
import nevg.autosilent.Service.Exception.OrderCreationException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class EcontDirectoryService {
    private final EcontClient client;
    private final EcontCityRepository cities;
    private final EcontOfficeRepository offices;
    private final EcontSyncStateRepository states;
    private final TransactionTemplate transaction;
    private final MessageSource messages;

    public EcontDirectoryService(EcontClient client, EcontCityRepository cities,
            EcontOfficeRepository offices, EcontSyncStateRepository states,
            PlatformTransactionManager transactionManager, MessageSource messages) {
        this.client = client; this.cities = cities; this.offices = offices;
        this.states = states; this.transaction = new TransactionTemplate(transactionManager);
        this.messages = messages;
    }

    public List<EcontCity> cities() { return cities.findByActiveTrueOrderByNameAsc(); }
    public List<EcontOffice> offices(Long cityId) { return offices.findByCityIdAndActiveTrueOrderByNameAsc(cityId); }
    public EcontSyncState status() { return states.findById(1L).orElseGet(EcontSyncState::new); }

    // Download both lists first; replace them atomically only after validating the whole response.
    public synchronized boolean refresh() {
        try {
            JsonNode cityData = client.fetch("getCities", "cities");
            JsonNode officeData = client.fetch("getOffices", "offices");
            Map<Long, EcontCity> incomingCities = new HashMap<>();
            for (JsonNode node : cityData) {
                var city = new EcontCity();
                city.setId(node.path("id").asLong()); city.setName(node.path("name").asText());
                city.setRegion(node.path("regionName").asText(""));
                city.setPostcode(node.path("postCode").asText("")); city.setActive(true);
                if (city.getId() <= 0 || city.getName().isBlank()
                        || incomingCities.put(city.getId(), city) != null) throw new IllegalStateException("Invalid Econt city");
            }
            Map<Long, EcontOffice> incomingOffices = new HashMap<>();
            for (JsonNode node : officeData) {
                // Lockers and mobile stations are excluded from this first version.
                if (node.path("isAPS").asBoolean() || node.path("isMPS").asBoolean()) continue;
                var office = new EcontOffice();
                office.setId(node.path("id").asLong()); office.setCode(node.path("code").asText());
                office.setName(node.path("name").asText());
                office.setCityId(node.path("address").path("city").path("id").asLong());
                office.setAddress(node.path("address").path("fullAddress").asText()); office.setActive(true);
                if (office.getId() <= 0 || office.getCode().isBlank() || office.getName().isBlank()
                        || office.getAddress().isBlank() || !incomingCities.containsKey(office.getCityId())
                        || incomingOffices.put(office.getId(), office) != null) throw new IllegalStateException("Invalid Econt office");
            }
            if (incomingCities.isEmpty() || incomingOffices.isEmpty()) throw new IllegalStateException("Empty Econt directory");
            transaction.executeWithoutResult(tx -> {
                var oldCities = cities.findAll();
                oldCities.forEach(city -> city.setActive(false));
                cities.saveAll(oldCities); cities.saveAll(incomingCities.values());
                var oldOffices = offices.findAll();
                oldOffices.forEach(office -> office.setActive(false));
                offices.saveAll(oldOffices); offices.saveAll(incomingOffices.values());
                var state = status(); state.setLastSuccess(LocalDateTime.now()); state.setLastError(null); states.save(state);
            });
            return true;
        } catch (RuntimeException exception) {
            var state = status();
            state.setLastError("Econt synchronization failed: " + exception.getClass().getSimpleName());
            states.save(state);
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Econt directory refresh failed; keeping previous data", exception);
            return false;
        }
    }

    public OrderAddressEntity resolve(CheckoutDeliveryDto request) {
        if (request == null || request.cityId() == null) throw invalid();
        EcontCity city = cities.findById(request.cityId()).filter(EcontCity::isActive).orElseThrow(this::invalid);
        var result = new OrderAddressEntity();
        result.setCity(city.getName() + (city.getRegion() == null || city.getRegion().isBlank() ? "" : ", " + city.getRegion()));
        result.setPostcode(city.getPostcode());
        if ("OFFICE".equals(request.type())) {
            if (request.officeId() == null) throw invalid();
            var office = offices.findById(request.officeId()).filter(EcontOffice::isActive)
                    .filter(o -> o.getCityId().equals(city.getId())).orElseThrow(this::invalid);
            result.setCourierOfficeCode(office.getCode()); result.setCourierOfficeName(office.getName());
            result.setAddressLine(office.getAddress());
        } else if ("ADDRESS".equals(request.type()) && request.address() != null
                && request.address().trim().length() >= 5 && request.address().trim().length() <= 255) {
            result.setAddressLine(request.address().trim());
        } else throw invalid();
        return result;
    }

    private OrderCreationException invalid() {
        return new OrderCreationException(messages.getMessage("shipping.invalid", null, LocaleContextHolder.getLocale()));
    }
}
