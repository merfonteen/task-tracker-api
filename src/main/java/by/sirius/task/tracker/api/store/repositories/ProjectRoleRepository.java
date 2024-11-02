package by.sirius.task.tracker.api.store.repositories;

import by.sirius.task.tracker.api.store.entities.ProjectEntity;
import by.sirius.task.tracker.api.store.entities.ProjectRoleEntity;
import by.sirius.task.tracker.api.store.entities.RoleEntity;
import by.sirius.task.tracker.api.store.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectRoleRepository extends JpaRepository<ProjectRoleEntity, Long> {
    Optional<ProjectRoleEntity> findByUserAndProject(UserEntity user, ProjectEntity project);
    boolean existsByUserAndProjectAndRole(UserEntity user, ProjectEntity project, RoleEntity role);
    void deleteByUserAndProject(UserEntity user, ProjectEntity project);
}
