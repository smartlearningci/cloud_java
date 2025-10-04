package pt.taskflow.tasks.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Simple Task entity mapped with JPA.
 * Lombok generates getters/setters/constructors/builder at compile time.
 * Note:
 *  - id is a UUID string generated on @PrePersist if missing
 *  - status defaults to "TODO" on @PrePersist if missing
 *  - createdAt/updatedAt timestamps are managed automatically
 */
@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Task {

  @Id
  private String id;

  private String title;        // required for create (no validation yet in Phase 0)
  private String description;  // optional free text
  private String status;       // TODO | DOING | DONE
  private String projectId;    // optional: filter tasks by project
  private String assignee;     // optional: owner/user name
  private Instant createdAt;
  private Instant updatedAt;

  /** Initialize defaults on first persist */
  @PrePersist
  public void prePersist() {
    if (id == null) id = UUID.randomUUID().toString();
    if (status == null) status = "TODO";
    createdAt = Instant.now();
    updatedAt = createdAt;
  }

  /** Track modification time on updates */
  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }
}
