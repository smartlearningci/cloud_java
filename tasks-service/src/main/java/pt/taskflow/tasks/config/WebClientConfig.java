package pt.taskflow.tasks.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuração do WebClient para chamadas HTTP de saída.
 *
 * - @LoadBalanced diz ao Spring Cloud para resolver NOME LÓGICO de serviço
 *   (ex.: http://tasks-service) via Service Discovery (Eureka) e distribuir
 *   as chamadas pelas instâncias disponíveis (client-side load balancing).
 *
 * - Isto permite evitar "localhost:8081" hard-coded. Quando escalar para
 *   2+ instâncias, o código continua igual — o LB trata da distribuição.
 */
@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced // ativa o Spring Cloud LoadBalancer neste builder
    public WebClient.Builder loadBalancedWebClientBuilder() {
        // Não definimos baseUrl aqui de propósito. O baseUrl é injectado
        // no OutboundClient a partir de configuração externa (Config Server).
        return WebClient.builder();
    }
}
