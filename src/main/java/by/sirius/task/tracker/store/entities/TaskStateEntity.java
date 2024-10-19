package by.sirius.task.tracker.store.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.scheduling.config.Task;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "task_states")
public class TaskStateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToOne
    private TaskStateEntity leftTaskState;

    @OneToOne
    private TaskStateEntity rightTaskState;

    @Builder.Default
    private Instant createdAt = Instant.now();

    @ManyToOne
    @JoinColumn(name = "project_id", referencedColumnName = "id")
    private ProjectEntity project;

    @Builder.Default
    @OneToMany
    @JoinColumn(name = "task_state_id", referencedColumnName = "id")
    private List<TaskEntity> tasks = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(!(o instanceof TaskStateEntity)) return false;
        TaskStateEntity that = (TaskStateEntity) o;
        return Objects.equals(that.id, id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public Optional<TaskStateEntity> getLeftTaskState() {
        return Optional.ofNullable(leftTaskState);
    }

    public Optional<TaskStateEntity> getRightTaskState() {
        return Optional.ofNullable(rightTaskState);
    }
}
