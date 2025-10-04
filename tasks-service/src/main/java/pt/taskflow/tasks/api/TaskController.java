package pt.taskflow.tasks.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.taskflow.tasks.domain.Task;
import pt.taskflow.tasks.domain.TaskRepository;

import java.util.*;

/**
 * REST API for Tasks (Phase 0).
 * Conventions:
 *  - JSON in/out
 *  - minimal validation to reduce cognitive load
 *  - status transitions not enforced yet (we keep it simple in Phase 0)
 *
 * Tip for learners:
 *  - If you see "Ensure compiler uses -parameters", this project already sets it
 *    in maven-compiler-plugin. Make sure you run with Maven or refresh the IDE.
 */
@RestController
@RequestMapping(value = "/tasks", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TaskController {

  // Spring injects a runtime proxy that implements our TaskRepository interface
  private final TaskRepository repo;

  /**
   * Create a task.
   * Phase 0: we accept the body as-is and rely on defaults for id/status.
   * Returns 201 with the persisted entity (including generated id and timestamps).
   */
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Task> create(@RequestBody Task body) {
    Task saved = repo.save(body);
    return ResponseEntity.status(201).body(saved);
  }

  /**
   * List tasks. Optional filters:
   *  - status=TODO|DOING|DONE
   *  - projectId=<string>
   * With both filters present, we match by both.
   */
  @GetMapping
  public List<Task> list(
      @RequestParam(name = "status", required = false) String status,
      @RequestParam(name = "projectId", required = false) String projectId) {
    if (status != null && projectId != null) return repo.findByStatusAndProjectId(status, projectId);
    if (status != null) return repo.findByStatus(status);
    if (projectId != null) return repo.findByProjectId(projectId);
    return repo.findAll();
  }

  /** Fetch a task by id, or return 404 if not found. */
  @GetMapping("/{id}")
  public ResponseEntity<Task> getById(@PathVariable("id") String id) {
    return repo.findById(id).map(ResponseEntity::ok)
              .orElseGet(() -> ResponseEntity.notFound().build());
  }

  /**
   * Update task status only (PATCH).
   * Body shape: { "status": "DONE" }
   * Phase 0: we do not validate transitions; we keep the happy path.
   */
  @PatchMapping(path = "/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> updateStatus(
      @PathVariable("id") String id,
      @RequestBody Map<String, String> body) {

    return repo.findById(id).map(t -> {
      String newStatus = body.get("status");
      if (newStatus != null) t.setStatus(newStatus);
      return ResponseEntity.ok(repo.save(t));
    }).orElseGet(() -> ResponseEntity.notFound().build());
  }
}
