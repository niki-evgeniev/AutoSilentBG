package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Dto.CategoryCreateDto;
import nevg.autosilent.Models.Dto.CategoryViewDto;
import nevg.autosilent.Models.Entity.Category;
import nevg.autosilent.Repository.CategoryRepository;
import nevg.autosilent.Service.CategoryService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public void create(CategoryCreateDto request) {
        String name = request.getName().trim();
        if (categoryRepository.existsByCategoryIgnoreCase(name)) {
            throw new IllegalArgumentException("Вече съществува категория с това име.");
        }

        Category category = new Category();
        category.setCategory(name);
        try {
            categoryRepository.saveAndFlush(category);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException("Вече съществува категория с това име.", exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryViewDto> getAll() {
        return categoryRepository.findAllByOrderByCategoryAsc().stream()
                .map(category -> new CategoryViewDto(category.getId(), category.getCategory()))
                .toList();
    }
}
