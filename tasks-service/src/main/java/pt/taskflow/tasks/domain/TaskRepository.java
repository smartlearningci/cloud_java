package pt.taskflow.tasks.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Spring Data JPA generates the repository implementation at runtime.
 * Method names follow "query derivation" conventions, e.g. findByStatus.
 */
public interface TaskRepository extends JpaRepository<Task, String> {
  List<Task> findByStatus(String status);
  List<Task> findByProjectId(String projectId);
  List<Task> findByStatusAndProjectId(String status, String projectId);
}
