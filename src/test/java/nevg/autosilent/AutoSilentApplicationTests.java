package nevg.autosilent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
class AutoSilentApplicationTests {

    @Autowired
    private WebApplicationContext context;

    @Test
    void contextLoads() {
    }

    @Test
    void productsPageRendersWithCombinedFilters() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        var defaultResponse = mockMvc.perform(get("/products"))
                .andReturn()
                .getResponse();
        var response = mockMvc.perform(get("/products")
                        .param("brand", "Missing brand")
                        .param("model", "Missing model")
                        .param("minPrice", "10")
                        .param("maxPrice", "100")
                        .param("inStock", "true"))
                .andReturn()
                .getResponse();

        assertThat(defaultResponse.getStatus()).isEqualTo(200);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString())
                .contains("name=\"brand\"")
                .contains("name=\"model\"")
                .contains("name=\"minPrice\"")
                .contains("name=\"maxPrice\"")
                .contains("name=\"inStock\"");
    }
}
