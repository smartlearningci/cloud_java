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
 */
@RestController
@RequestMapping(value = "/tasks", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TaskController {

  private final TaskRepository repo;

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Task> create(@RequestBody Task body) {
    Task saved = repo.save(body);
    return ResponseEntity.status(201).body(saved);
  }

  @GetMapping
  public List<Task> list(
      @RequestParam(name = "status", required = false) String status,
      @RequestParam(name = "projectId", required = false) String projectId) {
    if (status != null && projectId != null) return repo.findByStatusAndProjectId(status, projectId);
    if (status != null) return repo.findByStatus(status);
    if (projectId != null) return repo.findByProjectId(projectId);
    return repo.findAll();
  }

  @GetMapping("/{id}")
  public ResponseEntity<Task> getById(@PathVariable("id") String id) {
    return repo.findById(id).map(ResponseEntity::ok)
              .orElseGet(() -> ResponseEntity.notFound().build());
  }

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

  /** 👇 Novo endpoint "human friendly" para browser */
  @GetMapping(path = "/", produces = MediaType.TEXT_HTML_VALUE)
  public String infoPage() {
    return """
        <html>
          <head><title>Task Service API</title></head>
          <body style='font-family:Arial; margin:2em;'>
            <h1>✅ Task Service API</h1>
            <p>Bem-vindo ao <strong>Task Service</strong>!</p>
            <p>Use os endpoints REST:</p>
            <ul>
              <li>GET /tasks – listar tarefas</li>
              <li>POST /tasks – criar tarefa</li>
              <li>GET /tasks/{id} – ver uma tarefa</li>
              <li>PATCH /tasks/{id}/status – atualizar estado</li>
            </ul>
          </body>
        </html>
        """;
  }
}
