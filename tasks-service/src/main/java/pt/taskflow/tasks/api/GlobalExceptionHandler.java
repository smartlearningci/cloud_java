package pt.taskflow.tasks.api;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;

/**
 * Replace the default "Whitelabel Error Page" with a compact JSON error.
 * Great for teaching: learners get a predictable structure when something breaks.
 * In real projects, you might add error codes, trace IDs, and log correlation.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handle(Exception ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Map.of(
            "timestamp", Instant.now().toString(),
            "message", "Something went wrong 🙈",
            "details", ex.getClass().getSimpleName()
        ));
  }
}
