package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Dto.PromoCodeCreateDto;
import nevg.autosilent.Models.Dto.PromoCodeViewDto;
import nevg.autosilent.Models.Entity.PromoCode;
import nevg.autosilent.Models.Entity.User;
import nevg.autosilent.Repository.PromoCodeRepository;
import nevg.autosilent.Repository.UserRepository;
import nevg.autosilent.Service.Exception.ProductAlreadyExistsException;
import nevg.autosilent.Service.PromoCodeService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class PromoCodeServiceImpl implements PromoCodeService {

    private static final List<Integer> ALLOWED_DISCOUNTS = List.of(5, 10, 15, 20, 25, 30);
    private static final Set<Integer> ALLOWED_DISCOUNT_SET = Set.copyOf(ALLOWED_DISCOUNTS);

    private final PromoCodeRepository promoCodeRepository;
    private final UserRepository userRepository;

    public PromoCodeServiceImpl(PromoCodeRepository promoCodeRepository, UserRepository userRepository) {
        this.promoCodeRepository = promoCodeRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<Integer> allowedDiscounts() {
        return ALLOWED_DISCOUNTS;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromoCodeViewDto> getAll() {
        return promoCodeRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(code -> new PromoCodeViewDto(
                        code.getId(),
                        code.getCode(),
                        code.getDiscountPercent(),
                        code.getCreatedBy().getEmail(),
                        code.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional
    public void create(PromoCodeCreateDto request, String adminEmail) {
        String code = normalizeCode(request.getCode());
        if (promoCodeRepository.existsByCode(code)) {
            throw new ProductAlreadyExistsException("code", "Вече съществува промокод с тази стойност.");
        }
        if (request.getDiscountPercent() == null || !ALLOWED_DISCOUNT_SET.contains(request.getDiscountPercent())) {
            throw new IllegalArgumentException("Невалиден процент за отстъпка.");
        }

        User admin = userRepository.findByEmailIgnoreCase(adminEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Администраторът не е намерен."));

        PromoCode promoCode = new PromoCode();
        promoCode.setCode(code);
        promoCode.setDiscountPercent(request.getDiscountPercent());
        promoCode.setCreatedBy(admin);
        promoCodeRepository.save(promoCode);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal discountPercent(String code) {
        String normalizedCode = normalizeCode(code);
        if (normalizedCode.isBlank()) {
            return BigDecimal.ZERO;
        }
        return promoCodeRepository.findByCode(normalizedCode)
                .map(promoCode -> BigDecimal.valueOf(promoCode.getDiscountPercent()))
                .orElseThrow(() -> new IllegalArgumentException("Невалиден промокод."));
    }

    @Override
    public String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }
}
