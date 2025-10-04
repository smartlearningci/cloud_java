package pt.taskflow.tasks.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import pt.taskflow.tasks.domain.Task;
import pt.taskflow.tasks.domain.TaskRepository;

/**
 * Preloads a couple of tasks on startup so that the first GET /tasks
 * returns JSON with content (helps engagement after a long workday).
 * Only runs when the repository is empty.
 */
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

  private final TaskRepository repo;

  @Override
  public void run(String... args) {
    if (repo.count() == 0) {
      repo.save(Task.builder().title("Example Task 1").description("demo seed").build());
      repo.save(Task.builder().title("Example Task 2").description("demo seed").status("DOING").build());
    }
  }
}
