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
 * Filtro simples que escreve um log por pedido recebido.
 * Útil para ver a porta local (8081/8082) e confirmar o round-robin do LB.
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  jakarta.servlet.http.HttpServletResponse response,
                                  FilterChain filterChain)
      throws ServletException, IOException {

    // Exemplo de linha de log -> "REQ GET /tasks port=8081 ua="curl/8.6.0""
    log.info("REQ {} {} port={} ua=\"{}\"",
        request.getMethod(),
        request.getRequestURI(),
        request.getLocalPort(),
        request.getHeader("User-Agent"));

    filterChain.doFilter(request, response);
  }
}
