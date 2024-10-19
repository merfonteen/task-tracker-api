package by.sirius.task.tracker.store.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;
import java.util.Objects;

@Entity
@Data
@Table(name = "roles")
public class RoleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    @ManyToMany(mappedBy = "roles")
    private List<UserEntity> users;

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(!(o instanceof RoleEntity)) return false;
        RoleEntity that = (RoleEntity) o;
        return Objects.equals(that.id, id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
