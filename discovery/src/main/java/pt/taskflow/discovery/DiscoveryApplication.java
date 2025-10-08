package pt.taskflow.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Starts the Eureka registry UI on http://localhost:8761.
 * Clients (gateway, tasks-service) will register here.
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryApplication {
  public static void main(String[] args) {
    SpringApplication.run(DiscoveryApplication.class, args);
  }
}
