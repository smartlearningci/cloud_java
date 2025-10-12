package pt.taskflow.tasks.infra;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Cliente HTTP "de saída" usado para demonstrar padrões de resiliência.
 *
 * NOTA:
 * - Este cliente chama um endpoint CONTROLADO (nosso) que pode "demorar" ou "falhar"
 *   propositadamente, para vermos timeouts/retries/circuit breaker a funcionar.
 *
 * - O baseUrl é configurável (Config Server) e por defeito aponta para "http://tasks-service",
 *   ou seja, usa Discovery + LoadBalancer (round-robin entre instâncias registadas).
 */
@Component
public class OutboundClient {

    private final WebClient webClient;

    /**
     * @param builder  WebClient.Builder com @LoadBalanced (ver WebClientConfig)
     * @param baseUrl  URL base das chamadas de saída (injetado por configuração externa)
     */
    public OutboundClient(WebClient.Builder builder,
                          @Value("${demo.outbound.base-url}") String baseUrl) {
        // Definimos aqui o baseUrl concreto, para manter o builder genérico na config.
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * Chamada GET ao nosso endpoint de simulação de atraso/falha.
     *
     * Anotações Resilience4j:
     * - @TimeLimiter: aplica um timeout total à operação reativa.
     * - @Retry: volta a tentar a chamada X vezes em falhas transitórias (configurável).
     * - @CircuitBreaker: abre o circuito após taxa de falhas acima do limiar -> falha rápido.
     *                     Usa "fallback" quando aberto ou quando a chamada falha.
     *
     * Os nomes "externalClient" mapeiam para a configuração no YAML do Config Server.
     */
    @TimeLimiter(name = "externalClient")
    @Retry(name = "externalClient")
    @CircuitBreaker(name = "externalClient", fallbackMethod = "fallback")
    public Mono<String> callDelayed(int ms, boolean fail) {
        return webClient.get()
                // Chamamos o NOSSO endpoint de simulação: /diagnostics/simulate/delay
                .uri(uri -> uri.path("/diagnostics/simulate/delay")
                        .queryParam("ms", ms)      // tempo de atraso que queremos simular
                        .queryParam("fail", fail)  // se true, responderá 500
                        .build())
                .retrieve()
                // bodyToMono(String): queremos o corpo tal e qual (JSON de teste)
                .bodyToMono(String.class);
    }

    /**
     * Fallback chamado quando:
     * - o TimeLimiter atinge timeout,
     * - Retry esgota tentativas,
     * - CircuitBreaker está aberto,
     * - ou ocorre qualquer exceção durante a chamada.
     *
     * Assinatura: mesmos parâmetros + Throwable no fim (exigido pelo Resilience4j).
     */
    private Mono<String> fallback(int ms, boolean fail, Throwable ex) {
        // Resposta "degradada", mas rápida e previsível para o cliente.
        // Em sistemas reais: responderíamos com dados em cache, defaults ou mensagem clara.
        return Mono.just("{\"source\":\"fallback\",\"reason\":\"" + ex.getClass().getSimpleName() + "\"}");
    }
}
