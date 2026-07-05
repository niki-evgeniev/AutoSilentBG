package nevg.nirton.Service.Impl;

import nevg.nirton.Models.Entity.IpAddress;
import nevg.nirton.Repository.IpAddressRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
    }

    private IpAddress address() {
        IpAddress address = new IpAddress();
        address.setId(7L);
        address.setAddress("192.0.2.30");
        return address;
    }
}
