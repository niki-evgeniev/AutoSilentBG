package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Dto.CategoryCreateDto;
import nevg.autosilent.Models.Entity.Category;
import nevg.autosilent.Repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryServiceImpl(categoryRepository);
    }

    @Test
    void createTrimsAndSavesCategory() {
        CategoryCreateDto request = new CategoryCreateDto();
        request.setName("  Автоаксесоари  ");

        categoryService.create(request);

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getCategory()).isEqualTo("Автоаксесоари");
    }

    @Test
    void createRejectsDuplicateCategory() {
        CategoryCreateDto request = new CategoryCreateDto();
        request.setName("Audio");
        when(categoryRepository.existsByCategoryIgnoreCase("Audio")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("съществува");
    }

    @Test
    void getAllMapsCategoriesInRepositoryOrder() {
        Category first = category(1L, "Audio");
        Category second = category(2L, "Изолация");
        when(categoryRepository.findAllByOrderByCategoryAsc()).thenReturn(List.of(first, second));

        assertThat(categoryService.getAll())
                .extracting("id", "name")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1L, "Audio"),
                        org.assertj.core.groups.Tuple.tuple(2L, "Изолация"));
    }

    @Test
    void getByIdReturnsCategoryWithSeoSlug() {
        Category category = category(3L, "Звукоизолация");
        when(categoryRepository.findById(3L)).thenReturn(java.util.Optional.of(category));

        assertThat(categoryService.getById(3L)).hasValueSatisfying(result -> {
            assertThat(result.name()).isEqualTo("Звукоизолация");
            assertThat(result.slug()).isEqualTo("zvukoizolatsiya");
        });
    }

    private Category category(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setCategory(name);
        return category;
    }
}
