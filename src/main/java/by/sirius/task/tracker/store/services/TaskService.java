package by.sirius.task.tracker.store.services;

import by.sirius.task.tracker.api.dto.AckDto;
import by.sirius.task.tracker.api.dto.TaskDto;
import by.sirius.task.tracker.api.exceptions.BadRequestException;
import by.sirius.task.tracker.api.factories.TaskDtoFactory;
import by.sirius.task.tracker.store.entities.ProjectEntity;
import by.sirius.task.tracker.store.entities.TaskEntity;
import by.sirius.task.tracker.store.entities.TaskStateEntity;
import by.sirius.task.tracker.store.repositories.TaskRepository;
import by.sirius.task.tracker.store.repositories.TaskStateRepository;
import by.sirius.task.tracker.store.services.helpers.ServiceHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.config.Task;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class TaskService {

    private final TaskDtoFactory taskDtoFactory;
    private final TaskRepository taskRepository;
    private final TaskStateRepository taskStateRepository;

    private final ServiceHelper serviceHelper;

    public List<TaskDto> getTasks(Long projectId, Long taskStateId) {

        Optional<TaskStateEntity> taskState = taskStateRepository.findByProjectIdAndId(projectId, taskStateId);

        return taskState
                .map(state -> state.getTasks()
                        .stream()
                        .map(taskDtoFactory::makeTaskDto)
                        .collect(Collectors.toList()))
                .orElseThrow(() -> new BadRequestException(
                        String.format("Task state with ID \"%d\" not found", taskStateId), HttpStatus.BAD_REQUEST));
    }

    public TaskDto createTask(Long projectId, Long taskStateId, String taskName) {

        if (taskName.isBlank()) {
            throw new BadRequestException("Task name can't be empty.", HttpStatus.BAD_REQUEST);
        }

        ProjectEntity project = serviceHelper.getProjectOrThrowException(projectId);
        TaskStateEntity taskState = serviceHelper.getTaskStateOrThrowException(taskStateId);

        Optional<TaskEntity> optionalAnotherTask = Optional.empty();

        for (TaskEntity task : taskState.getTasks()) {
            if (task.getName().equals(taskName)) {
                throw new BadRequestException(
                        String.format("Task name \"%s\" already exists.", taskName), HttpStatus.BAD_REQUEST);
            }
            if (task.getRightTask().isEmpty()) {
                optionalAnotherTask = Optional.of(task);
            }
        }

        TaskEntity task = TaskEntity.builder()
                .name(taskName)
                .taskStateEntity(taskState)
                .build();

        taskState.getTasks().add(task);

        taskRepository.saveAndFlush(task);

        optionalAnotherTask.ifPresent(it -> {
            task.setLeftTask(it);
            it.setRightTask(task);
            taskRepository.saveAndFlush(it);
        });

        taskStateRepository.saveAndFlush(taskState);

        return taskDtoFactory.makeTaskDto(task);
    }


    public TaskDto editTask(Long taskId, String taskName) {

        if (taskName.isBlank()) {
            throw new BadRequestException("Task name can't be empty.", HttpStatus.BAD_REQUEST);
        }

        TaskEntity taskToUpdate = serviceHelper.getTaskEntityOrThrowException(taskId);

        Long taskStateId = taskToUpdate.getTaskStateEntity().getId();

        taskRepository
                .findByTaskStateEntityIdAndNameIgnoreCase(taskStateId, taskName)
                .filter(existingTask -> !existingTask.getId().equals(taskId))
                .ifPresent(existingTask -> {
                    throw new BadRequestException(
                            String.format("Task \"%s\" already exists in this task state.", taskName), HttpStatus.BAD_REQUEST);
                });

        taskToUpdate.setName(taskName);

        TaskEntity updatedTask = taskRepository.saveAndFlush(taskToUpdate);

        return taskDtoFactory.makeTaskDto(updatedTask);
    }

    public TaskDto changeTaskPosition(Long taskId, Optional<Long> optionalLeftTaskId) {

        TaskEntity changeTask = serviceHelper.getTaskEntityOrThrowException(taskId);

        TaskStateEntity taskState = changeTask.getTaskStateEntity();

        Optional<Long> optionalOldLeftTaskId = changeTask
                .getLeftTask()
                .map(TaskEntity::getId);

        if (optionalOldLeftTaskId.equals(optionalLeftTaskId)) {
            return taskDtoFactory.makeTaskDto(changeTask);
        }

        Optional<TaskEntity> optionalNewLeftTask = optionalLeftTaskId
                .map(leftTaskId -> {

                    if (taskId.equals(leftTaskId)) {
                        throw new BadRequestException(
                                "Left task id equals changed task.", HttpStatus.BAD_REQUEST);
                    }

                    TaskEntity leftTaskEntity = serviceHelper.getTaskEntityOrThrowException(leftTaskId);

                    if (!taskState.getId().equals(leftTaskEntity.getTaskStateEntity().getId())) {
                        throw new BadRequestException(
                                "Task position can be changed within the same task state.", HttpStatus.BAD_REQUEST);
                    }

                    return leftTaskEntity;
                });

        Optional<TaskEntity> optionalNewRightTask;
        if (optionalNewLeftTask.isEmpty()) {

            optionalNewRightTask = taskState
                    .getTasks()
                    .stream()
                    .filter(anotherTask -> anotherTask.getLeftTask().isEmpty())
                    .findAny();
        } else {

            optionalNewRightTask = optionalNewLeftTask
                    .get()
                    .getRightTask();
        }

        serviceHelper.replaceOldTaskPosition(changeTask);

        if (optionalNewLeftTask.isPresent()) {

            TaskEntity newLeftTask = optionalNewLeftTask.get();

            newLeftTask.setRightTask(changeTask);

            changeTask.setLeftTask(newLeftTask);
        } else {
            changeTask.setLeftTask(null);
        }

        if (optionalNewRightTask.isPresent()) {

            TaskEntity newRightTask = optionalNewRightTask.get();

            newRightTask.setLeftTask(changeTask);

            changeTask.setRightTask(newRightTask);
        } else {
            changeTask.setRightTask(null);
        }

        changeTask = taskRepository.saveAndFlush(changeTask);

        optionalNewLeftTask
                .ifPresent(taskRepository::saveAndFlush);

        optionalNewRightTask
                .ifPresent(taskRepository::saveAndFlush);

        taskState.getTasks().sort(Comparator.comparing(TaskEntity::getId));
        taskStateRepository.saveAndFlush(taskState);

        return taskDtoFactory.makeTaskDto(changeTask);
    }

    public AckDto deleteTask(Long taskId) {

        TaskEntity taskToDelete = serviceHelper.getTaskEntityOrThrowException(taskId);

        serviceHelper.replaceOldTaskPosition(taskToDelete);

        TaskStateEntity taskState = taskToDelete.getTaskStateEntity();

        taskState.getTasks().remove(taskToDelete);

        taskStateRepository.saveAndFlush(taskState);

        taskRepository.delete(taskToDelete);

        return AckDto.builder().answer(true).build();
    }
}
