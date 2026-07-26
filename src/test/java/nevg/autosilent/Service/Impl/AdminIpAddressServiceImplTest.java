package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Entity.IpAddress;
import nevg.autosilent.Repository.IpAddressRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class AdminIpAddressServiceImplTest {

    private final IpAddressRepository repository = mock(IpAddressRepository.class);
    private final AdminIpAddressServiceImpl service = new AdminIpAddressServiceImpl(repository);

    @Test
    void permanentBanHasNoEndDate() {
        IpAddress address = address();
        when(repository.findById(7L)).thenReturn(Optional.of(address));

        service.banPermanently(7L);

        assertThat(address.isBanned()).isTrue();
        assertThat(address.getBannedUntil()).isNull();
        verify(repository).save(address);
    }

    @Test
    void temporaryBanRequiresValidNumberOfDays() {
        assertThatThrownBy(() -> service.banForDays(7L, 0))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
        assertThatThrownBy(() -> service.banForDays(7L, 3651))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
    }

    @Test
    void removeBanClearsState() {
        IpAddress address = address();
        address.setBanned(true);
        when(repository.findById(7L)).thenReturn(Optional.of(address));

        service.removeBan(7L);

        assertThat(address.isBanned()).isFalse();
        assertThat(address.getBannedUntil()).isNull();
        verify(repository).save(address);
    }

    @Test
    void temporaryBanSetsFutureEndDate() {
        IpAddress address = address();
        when(repository.findById(7L)).thenReturn(Optional.of(address));
        LocalDateTime before = LocalDateTime.now().plusDays(6);

        service.banForDays(7L, 7);

        assertThat(address.isBanned()).isTrue();
        assertThat(address.getBannedUntil()).isAfter(before);
        verify(repository).save(address);
    }

    @Test
    void invalidBanPeriodDoesNotQueryRepository() {
        assertThatThrownBy(() -> service.banForDays(7L, -1))
                .isInstanceOf(ResponseStatusException.class);

        verify(repository, never()).findById(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void getAllClampsNegativePageAndMapsExpiredBanAsAllowed() {
        IpAddress address = address();
        address.setBanned(true);
        address.setBannedUntil(LocalDateTime.now().minusDays(1));
        address.setCountVisits(3L);
        address.setFirstSeen(LocalDateTime.now().minusDays(3));
        address.setLastSeen(LocalDateTime.now());
        when(repository.findAll(org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(address)));

        var result = service.getAll(-3);

        assertThat(result.getContent()).singleElement().satisfies(dto -> {
            assertThat(dto.address()).isEqualTo("192.0.2.30");
            assertThat(dto.banned()).isFalse();
            assertThat(dto.bannedUntil()).isNull();
        });
        var captor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(15);
    }

    @Test
    void missingAddressReturnsNotFound() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.banPermanently(404L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }

    private IpAddress address() {
        IpAddress address = new IpAddress();
        address.setId(7L);
        address.setAddress("192.0.2.30");
        return address;
    }
}
