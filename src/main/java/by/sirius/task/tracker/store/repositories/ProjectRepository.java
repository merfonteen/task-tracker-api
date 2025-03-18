package by.sirius.task.tracker.store.repositories;

import by.sirius.task.tracker.store.entities.ProjectEntity;
import by.sirius.task.tracker.store.entities.UserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {
    Optional<ProjectEntity> findByName(String name);

    List<ProjectEntity> findAllByAdmin(UserEntity admin);

    List<ProjectEntity> findAllByUsersContaining(UserEntity user);

    @EntityGraph(attributePaths = {"taskStates"})
    @Query("SELECT p FROM ProjectEntity p WHERE p.id = :projectId AND p.admin.username = :adminName")
    Optional<ProjectEntity> findWithTaskStatesByProjectIdAndAdminName(@Param("projectId") Long projectId,
                                                            @Param("adminName") String adminName);
}
