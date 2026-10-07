package nevg.autosilent.Controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

@SpringBootTest
class KitPageRenderTest {
    @Autowired WebApplicationContext context;

    @Test
    void adminCanRenderKitFormWithComponentFields() throws Exception {
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                        new nevg.autosilent.Models.Security.ShopUserDetails("admin@example.com", "unused", "Admin",
                                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))), "unused",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            var response = MockMvcBuilders.webAppContextSetup(context).build()
                    .perform(get("/admin/settings/kits").principal(authentication)).andReturn().getResponse();
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getContentAsString())
                    .contains("components[0].productId", "components[0].quantity", "Добави продукт");
            assertThat(org.jsoup.Jsoup.parse(response.getContentAsString())
                    .selectFirst("form.product-editor").hasAttr("novalidate")).isFalse();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void invalidPriceExplainsThatUploadedImageMustBeSelectedAgain() throws Exception {
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                new nevg.autosilent.Models.Security.ShopUserDetails("admin@example.com", "unused", "Admin",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))), "unused",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            var response = MockMvcBuilders.webAppContextSetup(context).build()
                    .perform(multipart("/admin/settings/kits")
                            .file(new MockMultipartFile("mainImage", "kit.png", "image/png", new byte[]{1, 2, 3}))
                            .param("nameProduct", "Кит")
                            .param("model", "за предни врати")
                            .param("description", "Кит за предни врати")
                            .param("price", "0")
                            .param("components[0].productId", "1")
                            .param("components[0].quantity", "1")
                            .principal(authentication))
                    .andReturn().getResponse();
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getContentAsString())
                    .contains("Изберете я отново преди запис")
                    .doesNotContain("Изберете различни продукти с количество поне 1");

            var retryWithoutImage = MockMvcBuilders.webAppContextSetup(context).build()
                    .perform(multipart("/admin/settings/kits")
                            .param("nameProduct", "Кит")
                            .param("model", "за предни врати")
                            .param("description", "Кит за предни врати")
                            .param("price", "29.99")
                            .param("components[0].productId", "1")
                            .param("components[0].quantity", "1")
                            .principal(authentication))
                    .andReturn().getResponse();
            assertThat(retryWithoutImage.getStatus()).isEqualTo(200);
            assertThat(retryWithoutImage.getContentAsString())
                    .contains("Добавете основна снимка")
                    .doesNotContain("Изберете различни продукти с количество поне 1");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
