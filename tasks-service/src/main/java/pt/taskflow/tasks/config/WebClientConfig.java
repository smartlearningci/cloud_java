package pt.taskflow.tasks.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Provides a Load-Balanced WebClient.Builder.
 *
 * Why:
 * - With @LoadBalanced, you can use logical service names (e.g., "http://tasks-service")
 *   instead of hard-coded host:port. The Spring Cloud LoadBalancer will pick an instance,
 *   using service discovery (Eureka) + round-robin by default.
 * - This means no code change is needed when you scale from 1 to N instances.
 */
@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        // Keep this builder generic; concrete baseUrl will be set in the client class
        // using externalized configuration (12-Factor: Config).
        return WebClient.builder();
    }
}
