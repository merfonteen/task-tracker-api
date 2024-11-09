package by.sirius.task.tracker.api.store.repositories;

import by.sirius.task.tracker.api.store.entities.ProjectEntity;
import by.sirius.task.tracker.api.store.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {
    Optional<ProjectEntity> findByName(String name);
    List<ProjectEntity> findAllByAdmin(UserEntity admin);
    List<ProjectEntity> findAllByUsersContaining(UserEntity user);
}
