package nevg.autosilent.Service;

import jakarta.servlet.http.HttpServletRequest;

public interface ClientIpResolver {
    String resolve(HttpServletRequest request);
}
