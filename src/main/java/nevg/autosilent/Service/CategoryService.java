package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.CategoryCreateDto;
import nevg.autosilent.Models.Dto.CategoryViewDto;

import java.util.List;
import java.util.Optional;

public interface CategoryService {
    void create(CategoryCreateDto category);

    List<CategoryViewDto> getAll();

    Optional<CategoryViewDto> getById(Long id);
}
