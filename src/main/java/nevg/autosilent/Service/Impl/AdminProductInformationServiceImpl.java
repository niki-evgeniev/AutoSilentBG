package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Dto.AdminProductInformationDto;
import nevg.autosilent.Models.Dto.AdminProductInformationPageDto;
import nevg.autosilent.Models.Dto.AdminProductInformationSummaryDto;
import nevg.autosilent.Models.Entity.Product;
import nevg.autosilent.Models.Enums.OrderStatus;
import nevg.autosilent.Repository.OrderItemRepository;
import nevg.autosilent.Repository.ProductRepository;
import nevg.autosilent.Repository.KitRepository;
import nevg.autosilent.Models.Entity.Kit;
import nevg.autosilent.Service.AdminProductInformationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminProductInformationServiceImpl implements AdminProductInformationService {

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final KitRepository kitRepository;

    public AdminProductInformationServiceImpl(ProductRepository productRepository,
                                              OrderItemRepository orderItemRepository,
                                              KitRepository kitRepository) {
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
        this.kitRepository = kitRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminProductInformationPageDto getProductInformation() {
        Map<Long, Kit> kitsByProductId = kitRepository.findAll().stream()
                .collect(Collectors.toMap(kit -> kit.getCatalogProduct().getId(), kit -> kit));
        Map<Long, Long> soldByProductId = orderItemRepository
                .sumQuantityByProductForStatus(OrderStatus.DELIVERED)
                .stream()
                .collect(Collectors.toMap(item -> item.productId(), item -> item.quantity()));
        List<AdminProductInformationDto> products = productRepository
                .findAllByOrderByNameProductAscModelAsc()
                .stream()
                .map(product -> toDto(product, soldByProductId.getOrDefault(product.getId(), 0L), kitsByProductId.get(product.getId())))
                .toList();

        AdminProductInformationSummaryDto summary = new AdminProductInformationSummaryDto(
                products.size(),
                products.stream().filter(AdminProductInformationDto::active).count(),
                products.stream().mapToLong(AdminProductInformationDto::stock).sum(),
                products.stream().mapToLong(AdminProductInformationDto::sold).sum(),
                products.stream().mapToLong(AdminProductInformationDto::views).sum(),
                products.stream()
                        .map(AdminProductInformationDto::stockValue)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );
        return new AdminProductInformationPageDto(products, summary);
    }

    private AdminProductInformationDto toDto(Product product, long sold, Kit kit) {
        BigDecimal price = product.getPrice() == null ? BigDecimal.ZERO : product.getPrice();
        int stock = kit == null ? product.getStock() : kit.availableStock();
        return new AdminProductInformationDto(
                product.getId(),
                product.getUrl(),
                product.getDisplayName(),
                product.getSku(),
                product.getCategory().getCategory(),
                price,
                stock,
                sold,
                product.getCount(),
                price.multiply(BigDecimal.valueOf(stock)),
                product.isActive(),
                product.getAddDate(),
                kit == null
        );
    }
}
