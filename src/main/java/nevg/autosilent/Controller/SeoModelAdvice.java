package nevg.autosilent.Controller;

import jakarta.servlet.http.HttpServletRequest;
import nevg.autosilent.Models.Dto.SeoPageMetadata;
import nevg.autosilent.Service.SeoUrlService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Locale;

@ControllerAdvice
public class SeoModelAdvice {

    private final SeoUrlService seoUrlService;

    public SeoModelAdvice(SeoUrlService seoUrlService) {
        this.seoUrlService = seoUrlService;
    }

    @ModelAttribute("seoPage")
    public SeoPageMetadata seoPage(HttpServletRequest request, Locale locale) {
        return seoUrlService.metadata(request, locale);
    }

    @ModelAttribute("bulgarianLanguageUrl")
    public String bulgarianLanguageUrl(HttpServletRequest request) {
        return seoUrlService.languageUrl(request, "bg");
    }

    @ModelAttribute("englishLanguageUrl")
    public String englishLanguageUrl(HttpServletRequest request) {
        return seoUrlService.languageUrl(request, "en");
    }
}
