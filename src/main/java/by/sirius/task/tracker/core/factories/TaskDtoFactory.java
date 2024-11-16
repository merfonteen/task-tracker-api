package by.sirius.task.tracker.core.factories;

import by.sirius.task.tracker.api.dto.TaskDto;
import by.sirius.task.tracker.store.entities.TaskEntity;
import org.springframework.stereotype.Component;

@Component
public class TaskDtoFactory {

    public TaskDto makeTaskDto(TaskEntity taskEntity) {
        return TaskDto.builder()
                .id(taskEntity.getId())
                .name(taskEntity.getName())
                .assignedUser(taskEntity.getAssignedUser() != null ? taskEntity.getAssignedUser().getUsername() : null)
                .leftTaskId(taskEntity.getLeftTask().map(TaskEntity::getId).orElse(null))
                .rightTaskId(taskEntity.getRightTask().map(TaskEntity::getId).orElse(null))
                .createdAt(taskEntity.getCreatedAt())
                .build();
    }
}
