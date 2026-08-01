package nevg.autosilent.Controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;

class FooterInformationControllerTest {

    private final FooterInformationController controller = new FooterInformationController();

    @Test
    void deliveryReturnsDeliveryPage() {
        ModelAndView result = controller.delivery();

        assertThat(result.getViewName()).isEqualTo("delivery");
        assertThat(result.getModel()).isEmpty();
    }

    @Test
    void paymentReturnsPaymentPage() {
        ModelAndView result = controller.payment();

        assertThat(result.getViewName()).isEqualTo("payment");
        assertThat(result.getModel()).isEmpty();
    }

    @Test
    void privacyPolicyReturnsPrivacyPolicyPage() {
        ModelAndView result = controller.privacyPolicy();

        assertThat(result.getViewName()).isEqualTo("privacy-policy");
        assertThat(result.getModel()).isEmpty();
    }
}
