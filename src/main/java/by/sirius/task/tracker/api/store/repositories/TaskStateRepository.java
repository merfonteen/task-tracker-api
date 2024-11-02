package by.sirius.task.tracker.api.store.repositories;

import by.sirius.task.tracker.api.store.entities.TaskStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaskStateRepository extends JpaRepository<TaskStateEntity, Long> {
    Optional<TaskStateEntity> findTaskStateEntityByProjectIdAndNameContainsIgnoreCase(Long projectId, String name);
    Optional<TaskStateEntity> findByProjectIdAndId(Long projectId, Long taskStateId);
}
