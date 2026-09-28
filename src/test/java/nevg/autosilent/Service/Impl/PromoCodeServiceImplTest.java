package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Entity.PromoCode;
import nevg.autosilent.Repository.PromoCodeRepository;
import nevg.autosilent.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromoCodeServiceImplTest {

    @Mock
    private PromoCodeRepository promoCodeRepository;

    @Mock
    private UserRepository userRepository;

    private PromoCodeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PromoCodeServiceImpl(promoCodeRepository, userRepository);
    }

    @Test
    void updateDiscountChangesExistingPromoCode() {
        PromoCode promoCode = new PromoCode();
        promoCode.setDiscountPercent(5);
        when(promoCodeRepository.findById(7L)).thenReturn(Optional.of(promoCode));

        service.updateDiscount(7L, 25);

        assertThat(promoCode.getDiscountPercent()).isEqualTo(25);
    }

    @Test
    void updateDiscountRejectsValueOutsideAllowedList() {
        assertThatThrownBy(() -> service.updateDiscount(7L, 99))
                .isInstanceOf(IllegalArgumentException.class);

        verify(promoCodeRepository, never()).findById(7L);
    }

    @Test
    void deleteRemovesExistingPromoCode() {
        PromoCode promoCode = new PromoCode();
        when(promoCodeRepository.findById(7L)).thenReturn(Optional.of(promoCode));

        service.delete(7L);

        verify(promoCodeRepository).delete(promoCode);
    }
}
