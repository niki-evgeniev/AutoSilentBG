package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Entity.IpAddress;
import nevg.autosilent.Repository.IpAddressRepository;
import nevg.autosilent.Repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BannedUserServiceImplTest {

    private final IpAddressRepository ipAddressRepository = mock(IpAddressRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-06T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void bansAddressAfterConfiguredRequestLimit() {
        IpAddress ipAddress = storedAddress("192.0.2.10");
        when(ipAddressRepository.findByAddressForUpdate(ipAddress.getAddress()))
                .thenReturn(Optional.of(ipAddress));
        BannedUserServiceImpl service = serviceWithLimit(2);

        assertThat(service.recordVisitAndCheckIfBanned("192.0.2.10", null)).isFalse();
        assertThat(service.recordVisitAndCheckIfBanned("192.0.2.10", null)).isFalse();
        assertThat(service.recordVisitAndCheckIfBanned("192.0.2.10", null)).isTrue();
        verify(ipAddressRepository, never())
                .insertIfAbsent(any(), eq("192.0.2.10"), any());
    }

    @Test
    void firstVisitInsertsAddressOnlyAfterLookupShowsItIsMissing() {
        String address = "192.0.2.12";
        IpAddress insertedAddress = storedAddress(address);
        insertedAddress.setCountVisits(0L);
        when(ipAddressRepository.findByAddressForUpdate(address))
                .thenReturn(Optional.empty(), Optional.of(insertedAddress));

        assertThat(serviceWithLimit(100).recordVisitAndCheckIfBanned(address, null)).isFalse();

        verify(ipAddressRepository, times(1)).insertIfAbsent(any(), eq(address), any());
        assertThat(insertedAddress.getCountVisits()).isEqualTo(1);
        assertThat(insertedAddress.getLastSeen()).isEqualTo(LocalDateTime.now(clock));
        assertThat(insertedAddress.getVisitsDate()).isEqualTo(LocalDateTime.now(clock).toLocalDate());
        assertThat(insertedAddress.getVisitsToday()).isEqualTo(1);
    }

    @Test
    void dailyVisitCounterContinuesForTheSameDay() {
        IpAddress ipAddress = storedAddress();
        ipAddress.setVisitsDate(LocalDateTime.now(clock).toLocalDate());
        ipAddress.setVisitsToday(4);
        when(ipAddressRepository.findByAddressForUpdate(ipAddress.getAddress()))
                .thenReturn(Optional.of(ipAddress));

        serviceWithLimit(100).recordVisitAndCheckIfBanned(ipAddress.getAddress(), null);

        assertThat(ipAddress.getVisitsToday()).isEqualTo(5);
    }

    @Test
    void dailyVisitCounterResetsWhenTheDateChanges() {
        IpAddress ipAddress = storedAddress();
        ipAddress.setVisitsDate(LocalDateTime.now(clock).toLocalDate().minusDays(1));
        ipAddress.setVisitsToday(12);
        when(ipAddressRepository.findByAddressForUpdate(ipAddress.getAddress()))
                .thenReturn(Optional.of(ipAddress));

        serviceWithLimit(100).recordVisitAndCheckIfBanned(ipAddress.getAddress(), null);

        assertThat(ipAddress.getVisitsDate()).isEqualTo(LocalDateTime.now(clock).toLocalDate());
        assertThat(ipAddress.getVisitsToday()).isEqualTo(1);
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
        return storedAddress("192.0.2.11");
    }

    private IpAddress storedAddress(String address) {
        IpAddress ipAddress = new IpAddress();
        ipAddress.setAddress(address);
        ipAddress.setFirstSeen(LocalDateTime.now(clock).minusDays(1));
        ipAddress.setLastSeen(LocalDateTime.now(clock));
        return ipAddress;
    }
}
