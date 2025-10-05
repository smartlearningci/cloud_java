package pt.taskflow.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Simple API key gate for demonstration purposes.
 * - Disabled by default (no API_KEY configured).
 * - Enable by setting env var API_KEY=<value> (e.g., in `.env` or compose).
 *
 * NOTE: This is intentionally minimal. In later phases (security module),
 *       we'll swap this for OIDC/JWT or IAM integration.
 */
@Component
public class ApiKeyFilter implements GlobalFilter, Ordered {

  @Value("${security.api-key.header:X-API-KEY}")
  private String headerName;

  @Value("${security.api-key.value:}")
  private String apiKey; // empty => disabled

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    if (apiKey == null || apiKey.isBlank()) {
      // No API key configured => do not enforce (Phase-1 keeps friction low)
      return chain.filter(exchange);
    }
    var provided = exchange.getRequest().getHeaders().getFirst(headerName);
    if (apiKey.equals(provided)) {
      return chain.filter(exchange);
    }
    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    return exchange.getResponse().setComplete();
  }

  @Override
  public int getOrder() {
    return -100; // Run early in the filter chain
  }
}
