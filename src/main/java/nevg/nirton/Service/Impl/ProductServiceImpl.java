package nevg.nirton.Service.Impl;

import nevg.nirton.Models.Dto.ProductCreateDto;
import nevg.nirton.Models.Dto.ProductViewDto;
import nevg.nirton.Models.Entity.Picture;
import nevg.nirton.Models.Entity.Product;
import nevg.nirton.Models.Entity.User;
import nevg.nirton.Repository.ProductRepository;
import nevg.nirton.Repository.UserRepository;
import nevg.nirton.Service.Exception.InvalidProductImageException;
import nevg.nirton.Service.Exception.ProductAlreadyExistsException;
import nevg.nirton.Service.Exception.ProductCreationException;
import nevg.nirton.Service.ProductService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

@Service
public class ProductServiceImpl implements ProductService {

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final Path imagesDirectory;

    public ProductServiceImpl(ProductRepository productRepository, UserRepository userRepository) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.imagesDirectory = Path.of("ProductImages").toAbsolutePath().normalize();
    }

    @Override
    @Transactional
    public void create(ProductCreateDto request, String ownerEmail) {
        validateUniqueFields(request);
        List<MultipartFile> additionalImages = request.getAdditionalImages().stream()
                .filter(image -> image != null && !image.isEmpty())
                .toList();
        validateImages(request.getMainImage(), additionalImages);

        User owner = userRepository.findByEmailIgnoreCase(ownerEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Потребителят не е намерен."));
        List<Path> storedFiles = new ArrayList<>();
        Path productDirectory = imagesDirectory.resolve(toDirectoryName(request.getNameProduct())).normalize();

        if (!productDirectory.startsWith(imagesDirectory)) {
            throw new InvalidProductImageException("Невалидно име на папка за продукта.");
        }

        try {
            Files.createDirectories(productDirectory);
            String mainImageName = storeImage(request.getMainImage(), productDirectory, "main", storedFiles);

            Product product = new Product();
            product.setNameProduct(request.getNameProduct().trim());
            product.setSku(request.getSku().trim().toUpperCase(Locale.ROOT));
            product.setCategory(request.getCategory().trim());
            product.setPrice(request.getPrice());
            product.setDescription(request.getDescription().trim());
            product.setStock(request.getStock());
            product.setActive(request.isActive());
            product.setUser(owner);

            product.addPicture(createPicture(mainImageName, true));
            int imageNumber = 1;
            for (MultipartFile image : additionalImages) {
                String fileName = storeImage(image, productDirectory, "image-" + imageNumber++, storedFiles);
                product.addPicture(createPicture(fileName, false));
            }
            productRepository.saveAndFlush(product);
        } catch (DataIntegrityViolationException exception) {
            deleteFiles(storedFiles, productDirectory);
            ProductAlreadyExistsException duplicate = duplicateFrom(exception);
            if (duplicate != null) {
                throw duplicate;
            }
            throw new ProductCreationException("Продуктът не можа да бъде записан в базата.", exception);
        } catch (IOException exception) {
            deleteFiles(storedFiles, productDirectory);
            throw new InvalidProductImageException("Снимките не можаха да бъдат записани. Опитайте отново.");
        } catch (RuntimeException exception) {
            deleteFiles(storedFiles, productDirectory);
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductViewDto> getActiveProducts() {
        return productRepository.findAllByActiveTrueOrderByAddDateDesc().stream()
                .map(this::toViewDto)
                .toList();
    }

    private ProductViewDto toViewDto(Product product) {
        String mainImageUrl = product.getPictures().stream()
                .filter(Picture::isMainImage)
                .findFirst()
                .map(picture -> "/ProductImages/"
                        + UriUtils.encodePathSegment(toDirectoryName(product.getNameProduct()), StandardCharsets.UTF_8)
                        + "/"
                        + UriUtils.encodePathSegment(picture.getFileName(), StandardCharsets.UTF_8))
                .orElse(null);

        return new ProductViewDto(
                product.getId(),
                product.getNameProduct(),
                product.getSku(),
                product.getCategory(),
                product.getPrice(),
                product.getDescription(),
                product.getStock(),
                mainImageUrl
        );
    }


    private void validateUniqueFields(ProductCreateDto request) {
        if (productRepository.existsByNameProductIgnoreCase(request.getNameProduct().trim())) {
            throw new ProductAlreadyExistsException("nameProduct", "Вече съществува продукт с това име.");
        }
        if (productRepository.existsBySkuIgnoreCase(request.getSku().trim())) {
            throw new ProductAlreadyExistsException("sku", "Вече съществува продукт с този код.");
        }
    }

    private void validateImages(MultipartFile mainImage, List<MultipartFile> additionalImages) {
        if (mainImage == null || mainImage.isEmpty()) {
            throw new InvalidProductImageException("Главната снимка е задължителна.");
        }
        if (additionalImages.size() > 4) {
            throw new InvalidProductImageException("Можете да добавите най-много 4 допълнителни снимки.");
        }
        detectExtension(mainImage);
        additionalImages.forEach(this::detectExtension);
    }

    private String storeImage(MultipartFile image,
                              Path productDirectory,
                              String prefix,
                              List<Path> storedFiles) throws IOException {
        String extension = detectExtension(image);
        String fileName = prefix + "-" + UUID.randomUUID() + "." + extension;
        Path destination = productDirectory.resolve(fileName).normalize();
        if (!destination.startsWith(productDirectory)) {
            throw new InvalidProductImageException("Невалиден път за снимка.");
        }
        try (InputStream inputStream = image.getInputStream()) {
            Files.copy(inputStream, destination);
        }
        storedFiles.add(destination);
        return fileName;
    }

    private Picture createPicture(String fileName, boolean mainImage) {
        Picture picture = new Picture();
        picture.setFileName(fileName);
        picture.setMainImage(mainImage);
        return picture;
    }

    private String toDirectoryName(String productName) {
        String directoryName = Normalizer.normalize(productName.trim(), Normalizer.Form.NFC)
                .replaceAll("[<>:\"/\\\\|?*\\p{Cntrl}]", "-")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^[. -]+|[. -]+$", "");

        if (directoryName.isBlank()) {
            throw new InvalidProductImageException("Името на продукта не може да се използва за папка.");
        }
        if (directoryName.length() > 100) {
            directoryName = directoryName.substring(0, 100);
        }
        return directoryName;
    }

    private String detectExtension(MultipartFile image) {
        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new InvalidProductImageException("Всяка снимка може да бъде до 5 MB.");
        }

        byte[] header = new byte[12];
        int bytesRead;
        try (InputStream inputStream = image.getInputStream()) {
            bytesRead = inputStream.read(header);
        } catch (IOException exception) {
            throw new InvalidProductImageException("Една от снимките не може да бъде прочетена.");
        }

        if (bytesRead >= 8 && (header[0] & 0xff) == 0x89 && header[1] == 0x50
                && header[2] == 0x4e && header[3] == 0x47) {
            return "png";
        }
        if (bytesRead >= 3 && (header[0] & 0xff) == 0xff && (header[1] & 0xff) == 0xd8
                && (header[2] & 0xff) == 0xff) {
            return "jpg";
        }
        if (bytesRead >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F'
                && header[3] == 'F' && header[8] == 'W' && header[9] == 'E'
                && header[10] == 'B' && header[11] == 'P') {
            return "webp";
        }
        throw new InvalidProductImageException("Разрешени са само JPG, PNG и WEBP снимки.");
    }

    private ProductAlreadyExistsException duplicateFrom(DataIntegrityViolationException exception) {
        String message = exception.getMostSpecificCause().getMessage().toLowerCase(Locale.ROOT);
        if (!message.contains("duplicate entry")) {
            return null;
        }
        if (message.contains("sku") || message.contains("ukfhmd06dsmj6k0n90swsh8ie9g")) {
            return new ProductAlreadyExistsException("sku", "Вече съществува продукт с този код.");
        }
        return new ProductAlreadyExistsException("nameProduct", "Вече съществува продукт с това име.");
    }

    private void deleteFiles(List<Path> files, Path productDirectory) {
        for (Path file : files) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException ignored) {
                // A failed cleanup must not hide the original persistence error.
            }
        }
        try {
            Files.deleteIfExists(productDirectory);
        } catch (IOException ignored) {
            // The directory is retained when it contains files from an existing product.
        }
    }
}
