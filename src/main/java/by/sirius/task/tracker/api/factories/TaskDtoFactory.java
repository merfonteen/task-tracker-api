package by.sirius.task.tracker.api.factories;

import by.sirius.task.tracker.api.dto.TaskDto;
import by.sirius.task.tracker.api.dto.TaskStateDto;
import by.sirius.task.tracker.store.entities.TaskEntity;
import by.sirius.task.tracker.store.entities.TaskStateEntity;
import org.springframework.stereotype.Component;

@Component
public class TaskDtoFactory {

    public TaskDto makeTaskDto(TaskEntity taskEntity) {
        return TaskDto.builder()
                .id(taskEntity.getId())
                .name(taskEntity.getName())
                .createdAt(taskEntity.getCreatedAt())
                .build();
    }
}
