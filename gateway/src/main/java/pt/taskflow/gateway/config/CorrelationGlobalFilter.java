// gateway/src/main/java/pt/taskflow/gateway/config/CorrelationGlobalFilter.java
package pt.taskflow.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * CorrelationGlobalFilter (fixed)
 * - Reads or generates X-Correlation-Id
 * - Puts it on the OUTBOUND request (to backends)
 * - Adds it to the RESPONSE headers *before* the chain so headers are still mutable
 * - Logs IN/OUT with corrId
 */
@Component
public class CorrelationGlobalFilter implements GlobalFilter, Ordered {

  private static final Logger log = LoggerFactory.getLogger(CorrelationGlobalFilter.class);
  public static final String CORR_ID_HEADER = "X-Correlation-Id";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange,
                           org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {

    // 1) Read existing corrId or generate a new one
    List<String> incoming = exchange.getRequest().getHeaders().get(CORR_ID_HEADER);
    String corrId = (incoming != null && !incoming.isEmpty() && incoming.get(0) != null && !incoming.get(0).isBlank())
        ? incoming.get(0)
        : UUID.randomUUID().toString();

    // 2) Log GW IN (minimal MDC usage)
    MDC.put("corrId", corrId);
    log.info("GW IN {} {}", exchange.getRequest().getMethod(), exchange.getRequest().getURI());
    MDC.remove("corrId");

    // 3) Mutate request to propagate the corrId to backends
    ServerHttpRequest mutated = exchange.getRequest().mutate()
        .headers(h -> h.set(CORR_ID_HEADER, corrId))
        .build();

    // 4) Set response header *now* (still mutable), so the client sees the same corrId
    exchange.getResponse().getHeaders().set(CORR_ID_HEADER, corrId);

    // 5) Continue the chain with the mutated request; log OUT when done
    return chain.filter(exchange.mutate().request(mutated).build())
        .doOnTerminate(() -> {
          MDC.put("corrId", corrId);
          log.info("GW OUT status={}", exchange.getResponse().getStatusCode());
          MDC.remove("corrId");
        });
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }
}
