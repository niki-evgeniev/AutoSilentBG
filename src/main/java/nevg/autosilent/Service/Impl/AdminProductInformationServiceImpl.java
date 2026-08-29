package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Dto.AdminProductInformationDto;
import nevg.autosilent.Models.Dto.AdminProductInformationPageDto;
import nevg.autosilent.Models.Dto.AdminProductInformationSummaryDto;
import nevg.autosilent.Models.Entity.Product;
import nevg.autosilent.Models.Enums.OrderStatus;
import nevg.autosilent.Repository.OrderItemRepository;
import nevg.autosilent.Repository.ProductRepository;
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

    public AdminProductInformationServiceImpl(ProductRepository productRepository,
                                              OrderItemRepository orderItemRepository) {
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminProductInformationPageDto getProductInformation() {
        Map<Long, Long> soldByProductId = orderItemRepository
                .sumQuantityByProductForStatus(OrderStatus.DELIVERED)
                .stream()
                .collect(Collectors.toMap(item -> item.productId(), item -> item.quantity()));
        List<AdminProductInformationDto> products = productRepository
                .findAllByOrderByNameProductAscModelAsc()
                .stream()
                .map(product -> toDto(product, soldByProductId.getOrDefault(product.getId(), 0L)))
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

    private AdminProductInformationDto toDto(Product product, long sold) {
        BigDecimal price = product.getPrice() == null ? BigDecimal.ZERO : product.getPrice();
        return new AdminProductInformationDto(
                product.getId(),
                product.getUrl(),
                product.getDisplayName(),
                product.getSku(),
                product.getCategory().getCategory(),
                price,
                product.getStock(),
                sold,
                product.getCount(),
                price.multiply(BigDecimal.valueOf(product.getStock())),
                product.isActive(),
                product.getAddDate()
        );
    }
}
