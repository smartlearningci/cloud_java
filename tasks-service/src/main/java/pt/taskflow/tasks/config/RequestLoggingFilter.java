package pt.taskflow.tasks.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * RequestLoggingFilter (Optional)
 * ------------------------------
 * Purpose:
 *   - Print one INFO log per request with method, URI, local port and user agent.
 *   - This is *in addition* to the MDC-based pattern; it’s very didactic for learners.
 *
 * If you prefer Tomcat access logs or Reactor Netty access logs, you can disable this filter.
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        log.info("REQ {} {} port={} ua=\"{}\"",
                request.getMethod(),
                request.getRequestURI(),
                request.getLocalPort(),
                request.getHeader("User-Agent"));

        chain.doFilter(request, response);
    }
}
