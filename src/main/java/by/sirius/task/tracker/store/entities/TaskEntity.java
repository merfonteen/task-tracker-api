package by.sirius.task.tracker.store.entities;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tasks")
public class TaskEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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

    @ManyToOne
    @JoinColumn(name = "assigned_user_id", referencedColumnName = "id")
    private UserEntity assignedUser;

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(!(o instanceof TaskEntity)) return false;
        TaskEntity that = (TaskEntity) o;
        return Objects.equals(that.id, id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public Optional<TaskEntity> getLeftTask() {
        return Optional.ofNullable(leftTask);
    }

    public Optional<TaskEntity> getRightTask() {
        return Optional.ofNullable(rightTask);
    }
}
