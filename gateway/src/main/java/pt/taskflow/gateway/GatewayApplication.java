package pt.taskflow.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Cloud Gateway bootstrap.
 *
 * Phase-1 goal:
 * - Introduce a single entry point at :8080
 * - Forward /api/tasks/** to the Phase-0 backend (/tasks/**)
 * This demonstrates evolution: we add an edge layer without touching the core service.
 */
@SpringBootApplication
public class GatewayApplication {
  public static void main(String[] args) {
    SpringApplication.run(GatewayApplication.class, args);
  }
}
