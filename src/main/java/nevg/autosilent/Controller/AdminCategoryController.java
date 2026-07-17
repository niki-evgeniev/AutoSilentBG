package nevg.autosilent.Controller;

import jakarta.validation.Valid;
import nevg.autosilent.Models.Dto.CategoryCreateDto;
import nevg.autosilent.Service.CategoryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/admin/settings/categories")
    public ModelAndView categories() {
        return categoryPage(new CategoryCreateDto());
    }

    @PostMapping("/admin/settings/categories")
    public ModelAndView create(@Valid @ModelAttribute("category") CategoryCreateDto category,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return categoryPage(category);
        }

        try {
            categoryService.create(category);
        } catch (IllegalArgumentException exception) {
            bindingResult.rejectValue("name", "category.exists", exception.getMessage());
            return categoryPage(category);
        }

        redirectAttributes.addFlashAttribute("categoryCreated", true);
        return new ModelAndView("redirect:/admin/settings/categories");
    }

    private ModelAndView categoryPage(CategoryCreateDto category) {
        ModelAndView modelAndView = new ModelAndView("admin-categories");
        modelAndView.addObject("category", category);
        modelAndView.addObject("categories", categoryService.getAll());
        return modelAndView;
    }
}
