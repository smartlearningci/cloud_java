package pt.taskflow.tasks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Tasks Service.
 * Phase 0 design goals:
 *  - keep it tiny and fast to run
 *  - return JSON quickly (seed a couple of records)
 *  - no external infra required (H2 in-memory)
 */
@SpringBootApplication
public class TasksServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(TasksServiceApplication.class, args);
  }
}
