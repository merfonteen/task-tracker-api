package by.sirius.task.tracker.store.entities;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "projects")
public class ProjectEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String name;

    @Builder.Default
    private Instant createdAt = Instant.now();

    @ManyToOne
    @JoinColumn(name = "admin_id", referencedColumnName = "id")
    private UserEntity admin;

    @Builder.Default
    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true)
    @JoinColumn(name = "project_id", referencedColumnName = "id")
    private List<TaskStateEntity> taskStates = new ArrayList<>();

    @Builder.Default
    @ManyToMany(mappedBy = "memberProjects", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private List<UserEntity> users = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectRoleEntity> projectRoles = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProjectEntity)) return false;
        ProjectEntity that = (ProjectEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
