package nevg.autosilent.Controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;

@SpringBootTest
class LlmsControllerTest {
    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy security;

    @Test
    void servesAnonymousRequestsWithExplicitUtf8AndUnchangedResourceBytes() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
        var response = mvc.perform(get("/llms.txt")).andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).isEqualTo("text/plain;charset=UTF-8");
        assertThat(response.getHeader("Cache-Control")).contains("no-cache");
        assertThat(response.getContentAsByteArray())
                .isEqualTo(new ClassPathResource("static/llms.txt").getContentAsByteArray());
        // Use a strict decoder so corrupt UTF-8 bytes cannot silently become replacement characters.
        var text = StandardCharsets.UTF_8.newDecoder()
                .decode(ByteBuffer.wrap(response.getContentAsByteArray())).toString();
        assertThat(text).startsWith("# AutoSilent.bg").contains("е български онлайн магазин");
        assertThat(response.getContentAsString()).isEqualTo(text);

        var headResponse = mvc.perform(head("/llms.txt")).andReturn().getResponse();
        assertThat(headResponse.getStatus()).isEqualTo(200);
        assertThat(headResponse.getContentType()).isEqualTo(response.getContentType());
    }
}
