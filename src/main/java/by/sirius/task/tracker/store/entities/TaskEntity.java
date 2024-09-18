package by.sirius.task.tracker.store.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "task")
public class TaskEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToOne
    private TaskEntity leftTask;

    @OneToOne
    private TaskEntity rightTask;

    @ManyToOne
    private TaskStateEntity taskStateEntity;

    @Builder.Default
    private Instant createdAt = Instant.now();

    public Optional<TaskEntity> getLeftTask() {
        return Optional.ofNullable(leftTask);
    }

    public Optional<TaskEntity> getRightTask() {
        return Optional.ofNullable(rightTask);
    }
}
