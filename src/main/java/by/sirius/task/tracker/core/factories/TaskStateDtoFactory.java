package by.sirius.task.tracker.core.factories;

import by.sirius.task.tracker.api.dto.TaskStateDto;
import by.sirius.task.tracker.store.entities.TaskStateEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class TaskStateDtoFactory {

    private final TaskDtoFactory taskDtoFactory;

    public TaskStateDto makeTaskStateDto(TaskStateEntity taskStateEntity) {
        return TaskStateDto.builder()
                .id(taskStateEntity.getId())
                .name(taskStateEntity.getName())
                .createdAt(taskStateEntity.getCreatedAt())
                .leftTaskStateId(taskStateEntity.getLeftTaskState().map(TaskStateEntity::getId).orElse(null))
                .rightTaskStateId(taskStateEntity.getRightTaskState().map(TaskStateEntity::getId).orElse(null))
                .tasks(
                taskStateEntity
                        .getTasks()
                        .stream()
                        .map(taskDtoFactory::makeTaskDto)
                        .collect(Collectors.toList())
        )
                .build();
    }
}
