package nevg.autosilent.Service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class ClientIpResolver {

    private final boolean trustForwardedHeaders;

    public ClientIpResolver(@Value("${AutoSilent.security.trust-forwarded-headers:false}") boolean trustForwardedHeaders) {
        this.trustForwardedHeaders = trustForwardedHeaders;
    }

    public String resolve(HttpServletRequest request) {
        if (trustForwardedHeaders) {
            String cloudflareAddress = validAddress(request.getHeader("CF-Connecting-IP"));
            if (cloudflareAddress != null) return cloudflareAddress;

            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null) {
                String forwardedAddress = validAddress(forwardedFor.split(",", 2)[0].trim());
                if (forwardedAddress != null) return forwardedAddress;
            }
        }

        String remoteAddress = validAddress(request.getRemoteAddr());
        return remoteAddress == null ? "0.0.0.0" : remoteAddress;
    }

    private String validAddress(String value) {
        if (value == null || value.isBlank() || value.length() > 45
                || !value.matches("[0-9a-fA-F:.]+")) {
            return null;
        }
        try {
            return InetAddress.getByName(value).getHostAddress();
        } catch (UnknownHostException exception) {
            return null;
        }
    }
}
