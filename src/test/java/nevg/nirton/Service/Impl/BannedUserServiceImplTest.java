package nevg.nirton.Service.Impl;

import nevg.nirton.Models.Entity.IpAddress;
import nevg.nirton.Repository.IpAddressRepository;
import nevg.nirton.Repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BannedUserServiceImplTest {

    private final IpAddressRepository ipAddressRepository = mock(IpAddressRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-06T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void bansAddressAfterConfiguredRequestLimit() {
        when(ipAddressRepository.findByAddress("192.0.2.10")).thenReturn(Optional.empty());
        BannedUserServiceImpl service = serviceWithLimit(2);

        assertThat(service.recordVisitAndCheckIfBanned("192.0.2.10", null)).isFalse();
        assertThat(service.recordVisitAndCheckIfBanned("192.0.2.10", null)).isFalse();
        assertThat(service.recordVisitAndCheckIfBanned("192.0.2.10", null)).isTrue();
    }

    @Test
    void permanentBanHasNoExpiry() {
        IpAddress ipAddress = storedAddress();
        ipAddress.setBanned(true);
        ipAddress.setBannedUntil(null);
        when(ipAddressRepository.findByAddress(ipAddress.getAddress())).thenReturn(Optional.of(ipAddress));

        assertThat(serviceWithLimit(100).checkIfIpAddressIsBanned(ipAddress.getAddress())).isTrue();
    }

    @Test
    void clearsExpiredTemporaryBan() {
        IpAddress ipAddress = storedAddress();
        ipAddress.setBanned(true);
        ipAddress.setBannedUntil(LocalDateTime.now(clock).minusSeconds(1));
        when(ipAddressRepository.findByAddress(ipAddress.getAddress())).thenReturn(Optional.of(ipAddress));

        assertThat(serviceWithLimit(100).checkIfIpAddressIsBanned(ipAddress.getAddress())).isFalse();
        assertThat(ipAddress.isBanned()).isFalse();
        assertThat(ipAddress.getBannedUntil()).isNull();
    }

    private BannedUserServiceImpl serviceWithLimit(int maxRequests) {
        return new BannedUserServiceImpl(ipAddressRepository, userRepository, clock, maxRequests,
                Duration.ofMinutes(1), Duration.ofMinutes(15));
    }

    private IpAddress storedAddress() {
        IpAddress ipAddress = new IpAddress();
        ipAddress.setAddress("192.0.2.11");
        ipAddress.setFirstSeen(LocalDateTime.now(clock).minusDays(1));
        ipAddress.setLastSeen(LocalDateTime.now(clock));
        return ipAddress;
    }
}
