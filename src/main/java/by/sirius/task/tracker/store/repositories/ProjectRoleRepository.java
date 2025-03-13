package by.sirius.task.tracker.store.repositories;

import by.sirius.task.tracker.store.entities.ProjectEntity;
import by.sirius.task.tracker.store.entities.ProjectRoleEntity;
import by.sirius.task.tracker.store.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectRoleRepository extends JpaRepository<ProjectRoleEntity, Long> {
    @Query("SELECT p FROM ProjectRoleEntity p WHERE p.project.admin.username = :username AND p.project.id = :projectId")
    Optional<ProjectRoleEntity> findByUsernameAndProjectId(@Param("username") String username,
                                                           @Param("projectId") Long projectId);
    void deleteByUserAndProject(UserEntity user, ProjectEntity project);
}
