package by.sirius.task.tracker.api.factories;

import by.sirius.task.tracker.api.dto.TaskHistoryDto;
import by.sirius.task.tracker.api.store.entities.TaskHistoryEntity;
import org.springframework.stereotype.Component;

@Component
public class TaskHistoryDtoFactory {
    public TaskHistoryDto makeTaskHistoryDto(TaskHistoryEntity taskHistory) {
        return TaskHistoryDto.builder()
                .id(taskHistory.getId())
                .taskId(taskHistory.getTask().getId())
                .username(taskHistory.getUsername())
                .changeType(taskHistory.getChangeType())
                .fieldName(taskHistory.getFieldName())
                .oldValue(taskHistory.getOldValue())
                .newValue(taskHistory.getNewValue())
                .timestamp(taskHistory.getChangedAt())
                .build();
    }
}
