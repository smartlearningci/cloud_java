package pt.taskflow.tasks.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/**
 * Welcome endpoint for learners—shows where to start and what to call.
 */
@RestController
public class RootController {

  @GetMapping("/")
  public Map<String, String> root() {
    return Map.of(
        "service", "tasks-service",
        "hint", "Use /tasks (GET/POST), /tasks/{id} (GET) and /tasks/{id}/status (PATCH)"
    );
  }
}
