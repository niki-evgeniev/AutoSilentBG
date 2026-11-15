package nevg.autosilent.Controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.jsoup.Jsoup;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
class MaterialGuidePageTest {
    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy security;

    @Test
    void anonymousVisitorCanReadTheWholeGuideWithoutJavascript() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
        var response = mvc.perform(get("/koe-da-izpolzvam")).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(200);
        var html = Jsoup.parse(response.getContentAsString(StandardCharsets.UTF_8));
        assertThat(html.select(".material")).hasSize(13);
        assertThat(html.select(".zone-card")).hasSize(6);
        assertThat(html.select("h1")).hasSize(1);
        assertThat(html.selectFirst("link[rel=canonical]").attr("href"))
                .isEqualTo("https://autosilent.bg/koe-da-izpolzvam");
        // Every in-page navigation link must resolve to existing, server-delivered content.
        for (var link : html.select("a[href^='#']")) {
            assertThat(html.getElementById(link.attr("href").substring(1))).isNotNull();
        }
        assertThat(html.select(".material[hidden]")).isEmpty();
    }

    @Test
    void legacyHtmlAddressPermanentlyRedirectsToCleanAddress() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
        var response = mvc.perform(get("/koe-da-izpolzvam.html")).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(301);
        assertThat(response.getRedirectedUrl()).isEqualTo("/koe-da-izpolzvam");
    }

    @Test
    void guideAssetsArePublicButAdminStillRequiresAuthentication() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
        for (String path : new String[]{"/css/material-guide.css", "/js/material-guide.js"}) {
            var response = mvc.perform(get(path)).andReturn().getResponse();
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getContentAsByteArray()).isNotEmpty();
        }
        var protectedResponse = mvc.perform(get("/admin/settings/catalog-seo"))
                .andReturn().getResponse();
        assertThat(protectedResponse.getStatus()).isEqualTo(302);
        assertThat(protectedResponse.getRedirectedUrl()).endsWith("/user/sign_in");
    }
}
