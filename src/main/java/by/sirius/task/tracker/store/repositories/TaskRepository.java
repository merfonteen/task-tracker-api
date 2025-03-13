package by.sirius.task.tracker.store.repositories;

import by.sirius.task.tracker.store.entities.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, Long> {
    Optional<TaskEntity> findByTaskStateIdAndNameIgnoreCase(Long taskStateId, String taskName);
    List<TaskEntity> findByAssignedUserId(Long userId);
}
