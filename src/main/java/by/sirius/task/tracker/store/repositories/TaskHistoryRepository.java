package by.sirius.task.tracker.store.repositories;

import by.sirius.task.tracker.store.entities.TaskHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskHistoryRepository extends JpaRepository<TaskHistoryEntity, Long> {
    List<TaskHistoryEntity> findByTaskId(Long taskId);
}
