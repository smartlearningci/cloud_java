package pt.taskflow.tasks.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * CorrelationIdFilter
 * -------------------
 * Purpose:
 *   - Ensure every incoming HTTP request has a correlation id (corrId).
 *   - If the client (or Gateway) already sent one via "X-Correlation-Id", we use it.
 *   - Otherwise, we generate a new UUID.
 *   - Store corrId + local server port + path in the MDC so they appear in logs.
 *   - Echo corrId back to the client in the response header for easier troubleshooting.
 *
 * Why:
 *   - With multiple services and instances, you need a way to "trace" a request end-to-end.
 *   - corrId in logs + response simplifies debugging and support.
 *
 * Notes:
 *   - OncePerRequestFilter guarantees this runs exactly once per request.
 *   - Always clear MDC in a finally block to avoid leakage between requests/threads.
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    /** HTTP header name used to carry the correlation id across services. */
    public static final String CORR_ID_HEADER = "X-Correlation-Id";

    /** MDC keys we will populate (keys are free-form strings used by the logging system). */
    public static final String MDC_CORR_ID    = "corrId";
    public static final String MDC_LOCAL_PORT = "localPort";
    public static final String MDC_PATH       = "path";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        // 1) Read corrId from header (if present), otherwise create a new one.
        String corrId = request.getHeader(CORR_ID_HEADER);
        if (corrId == null || corrId.isBlank()) {
            corrId = UUID.randomUUID().toString();
        }

        try {
            // 2) Put useful attributes into MDC so they appear in log patterns.
            MDC.put(MDC_CORR_ID, corrId);
            MDC.put(MDC_LOCAL_PORT, String.valueOf(request.getLocalPort()));
            MDC.put(MDC_PATH, request.getMethod() + " " + request.getRequestURI());

            // 3) Echo corrId in the response header (helps clients correlate).
            response.setHeader(CORR_ID_HEADER, corrId);

            // 4) Continue the chain.
            chain.doFilter(request, response);

        } finally {
            // 5) Clean up the MDC (absolutely important).
            MDC.remove(MDC_CORR_ID);
            MDC.remove(MDC_LOCAL_PORT);
            MDC.remove(MDC_PATH);
        }
    }
}
