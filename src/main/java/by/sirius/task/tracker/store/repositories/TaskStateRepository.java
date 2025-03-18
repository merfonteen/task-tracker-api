package by.sirius.task.tracker.store.repositories;

import by.sirius.task.tracker.store.entities.TaskStateEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaskStateRepository extends JpaRepository<TaskStateEntity, Long> {
    Optional<TaskStateEntity> findTaskStateEntityByProjectIdAndNameContainsIgnoreCase(Long projectId, String name);

    Optional<TaskStateEntity> findByProjectIdAndId(Long projectId, Long taskStateId);

    @EntityGraph(attributePaths = {"tasks"})
    @Query("SELECT ts FROM TaskStateEntity ts WHERE ts.project.id = :projectId AND ts.id = :taskStateId")
    Optional<TaskStateEntity> findWithTasksByProjectIdAndId(@Param("projectId") Long projectId,
                                                            @Param("taskStateId") Long taskStateId);
}
