package nevg.autosilent.Controller;

import jakarta.validation.Valid;
import nevg.autosilent.Models.Dto.PromotionForm;
import nevg.autosilent.Service.PromotionService;
import nevg.autosilent.Repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.*;

@Controller
public class PromotionController {
    private final PromotionService promotions;
    private final ProductRepository products;
    private final String siteUrl;
    public PromotionController(PromotionService promotions, ProductRepository products,
            @Value("${AutoSilent.site-url:https://autosilent.bg}") String siteUrl) {
        this.promotions = promotions; this.products = products; this.siteUrl = siteUrl.replaceAll("/+$", "");
    }
    @GetMapping("/promocii")
    public ModelAndView list(@RequestParam(defaultValue = "") String search,
                            @RequestParam(defaultValue = "false") boolean inStock,
                            @RequestParam(defaultValue = "newest") String order,
                            @RequestParam(defaultValue = "0") int page) {
        var offers = promotions.active().stream().filter(p -> search.isBlank()
                || (p.name() + " " + p.sku()).toLowerCase(Locale.ROOT).contains(search.trim().toLowerCase(Locale.ROOT)))
                .filter(p -> !inStock || p.stock() > 0).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        if ("priceAsc".equals(order)) offers.sort(Comparator.comparing(PromotionService.Card::price));
        if ("priceDesc".equals(order)) offers.sort(Comparator.comparing(PromotionService.Card::price).reversed());
        int pages = (offers.size() + 8) / 9;
        if (page < 0 || page > 0 && page >= pages) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
        return new ModelAndView("promotions").addObject("products", offers.stream().skip((long) page * 9).limit(9).toList())
                .addObject("search", search).addObject("inStock", inStock).addObject("sortMode", order)
                .addObject("page", page).addObject("pages", pages).addObject("canonical", siteUrl + "/promocii" + (page > 0 ? "?page=" + page : ""));
    }
    @GetMapping("/promocii/{url}")
    public ModelAndView details(@PathVariable String url) {
        if (url.matches("\\d+")) {
            long id;
            try { id = Long.parseLong(url); }
            catch (NumberFormatException exception) { throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND); }
            var redirect = new ModelAndView("redirect:" + promotions.get(id).promotionUrl());
            redirect.setStatus(org.springframework.http.HttpStatus.MOVED_PERMANENTLY);
            return redirect;
        }
        var offer = promotions.getBySlug(url);
        return new ModelAndView("product-details").addObject("promotionOffer", offer)
                .addObject("product", promotions.details(offer)).addObject("relatedProducts", promotions.related(offer.id()));
    }
    @GetMapping("/admin/settings/promotions") @PreAuthorize("hasRole('ADMIN')")
    public ModelAndView settings() {
        return new ModelAndView("admin-promotions").addObject("promotions", promotions.all())
                .addObject("catalogProducts", products.findAllByOrderByNameProductAscModelAsc());
    }
    @PostMapping({"/admin/settings/promotions", "/admin/settings/promotions/{id}"})
    @PreAuthorize("hasRole('ADMIN')")
    public String save(@PathVariable(required = false) Long id, @Valid @ModelAttribute PromotionForm form,
                        BindingResult errors, RedirectAttributes attributes) {
        if (errors.hasErrors()) attributes.addFlashAttribute("promotionInvalid", true);
        else {
            try { promotions.save(id, form); attributes.addFlashAttribute("promotionSaved", true); }
            catch (IllegalArgumentException exception) { attributes.addFlashAttribute("promotionError", exception.getMessage()); }
        }
        return "redirect:/admin/settings/promotions";
    }
}
