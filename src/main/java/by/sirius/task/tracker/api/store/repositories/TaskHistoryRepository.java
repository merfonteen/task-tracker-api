package by.sirius.task.tracker.api.store.repositories;

import by.sirius.task.tracker.api.store.entities.TaskHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskHistoryRepository extends JpaRepository<TaskHistoryEntity, Long> {
    List<TaskHistoryEntity> findByTaskId(Long taskId);
}
