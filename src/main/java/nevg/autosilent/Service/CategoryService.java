package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.CategoryCreateDto;
import nevg.autosilent.Models.Dto.CategoryViewDto;

import java.util.List;

public interface CategoryService {
    void create(CategoryCreateDto category);

    List<CategoryViewDto> getAll();
}
