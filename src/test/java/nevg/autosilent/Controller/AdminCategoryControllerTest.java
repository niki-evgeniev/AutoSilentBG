package nevg.autosilent.Controller;

import nevg.autosilent.Models.Dto.CategoryCreateDto;
import nevg.autosilent.Models.Dto.CategoryViewDto;
import nevg.autosilent.Service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    private AdminCategoryController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminCategoryController(categoryService);
    }

    @Test
    void categoriesShowsFormAndExistingCategories() {
        List<CategoryViewDto> categories = List.of(new CategoryViewDto(1L, "Audio"));
        when(categoryService.getAll()).thenReturn(categories);

        var result = controller.categories();

        assertThat(result.getViewName()).isEqualTo("admin-categories");
        assertThat(result.getModel().get("category")).isInstanceOf(CategoryCreateDto.class);
        assertThat(result.getModel().get("categories")).isSameAs(categories);
    }

    @Test
    void createSavesAndRedirects() {
        CategoryCreateDto category = new CategoryCreateDto();
        category.setName("Audio");
        var binding = new BeanPropertyBindingResult(category, "category");
        var redirect = new RedirectAttributesModelMap();

        var result = controller.create(category, binding, redirect);

        verify(categoryService).create(category);
        assertThat(result.getViewName()).isEqualTo("redirect:/admin/settings/categories");
        assertThat(redirect.getFlashAttributes().get("categoryCreated")).isEqualTo(true);
    }
}
