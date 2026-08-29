package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Dto.AdminIpAddressDto;
import nevg.autosilent.Models.Dto.AdminVisitStatisticsDto;
import nevg.autosilent.Models.Entity.IpAddress;
import nevg.autosilent.Repository.IpAddressRepository;
import nevg.autosilent.Service.AdminIpAddressService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class AdminIpAddressServiceImpl implements AdminIpAddressService {

    private static final int PAGE_SIZE = 15;
    private static final int MAX_BAN_DAYS = 3650;

    private final IpAddressRepository ipAddressRepository;

    public AdminIpAddressServiceImpl(IpAddressRepository ipAddressRepository) {
        this.ipAddressRepository = ipAddressRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminIpAddressDto> getAll(int page) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "lastSeen"));
        LocalDateTime now = LocalDateTime.now();
        return ipAddressRepository.findAll(pageRequest).map(ipAddress -> toDto(ipAddress, now));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminVisitStatisticsDto getVisitStatistics() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime startOfNextDay = today.plusDays(1).atStartOfDay();
        return new AdminVisitStatisticsDto(
                ipAddressRepository.sumAllVisits(),
                ipAddressRepository.countAddressesSeenBetween(startOfDay, startOfNextDay),
                today);
    }

    @Override
    @Transactional
    public void banPermanently(Long id) {
        IpAddress ipAddress = findById(id);
        ipAddress.setBanned(true);
        ipAddress.setBannedUntil(null);
        ipAddressRepository.save(ipAddress);
    }

    @Override
    @Transactional
    public void banForDays(Long id, int days) {
        if (days < 1 || days > MAX_BAN_DAYS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ban period must be between 1 and 3650 days");
        }
        IpAddress ipAddress = findById(id);
        ipAddress.setBanned(true);
        ipAddress.setBannedUntil(LocalDateTime.now().plusDays(days));
        ipAddressRepository.save(ipAddress);
    }

    @Override
    @Transactional
    public void removeBan(Long id) {
        IpAddress ipAddress = findById(id);
        ipAddress.setBanned(false);
        ipAddress.setBannedUntil(null);
        ipAddressRepository.save(ipAddress);
    }

    private IpAddress findById(Long id) {
        return ipAddressRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private AdminIpAddressDto toDto(IpAddress ipAddress, LocalDateTime now) {
        boolean activelyBanned = ipAddress.isBanned()
                && (ipAddress.getBannedUntil() == null || ipAddress.getBannedUntil().isAfter(now));
        return new AdminIpAddressDto(
                ipAddress.getId(), ipAddress.getAddress(), ipAddress.getFirstSeen(), ipAddress.getLastSeen(),
                ipAddress.getCountVisits(), activelyBanned,
                activelyBanned ? ipAddress.getBannedUntil() : null,
                ipAddress.getUser() == null ? null : ipAddress.getUser().getEmail());
    }
}
