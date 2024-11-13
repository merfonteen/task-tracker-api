package by.sirius.task.tracker.api.services;

import by.sirius.task.tracker.api.dto.AckDto;
import by.sirius.task.tracker.api.dto.TaskStateDto;
import by.sirius.task.tracker.api.exceptions.BadRequestException;
import by.sirius.task.tracker.api.factories.TaskStateDtoFactory;
import by.sirius.task.tracker.api.services.helpers.ServiceHelper;
import by.sirius.task.tracker.api.store.entities.ProjectEntity;
import by.sirius.task.tracker.api.store.entities.TaskStateEntity;
import by.sirius.task.tracker.api.store.repositories.TaskStateRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class TaskStateService {

    private final TaskStateRepository taskStateRepository;
    private final TaskStateDtoFactory taskStateDtoFactory;

    private final ServiceHelper serviceHelper;

    @Cacheable(value = "taskStates", key = "#projectId")
    public List<TaskStateDto> getTaskStates(Long projectId) {
        log.debug("Fetching task states for project ID: {}", projectId);

        ProjectEntity project = serviceHelper.getProjectOrThrowException(projectId);

        return project
                .getTaskStates()
                .stream()
                .map(taskStateDtoFactory::makeTaskStateDto)
                .collect(Collectors.toList());
    }

    @CacheEvict(value = "taskStates", key = "#projectId")
    @Transactional
    public TaskStateDto createTaskState(Long projectId, String taskStateName) {
        log.info("Creating task state '{}' in project with ID: {}", taskStateName, projectId);

        if (taskStateName.isBlank()) {
            throw new BadRequestException("Task state name can't be empty.", HttpStatus.BAD_REQUEST);
        }

        ProjectEntity project = serviceHelper.getProjectOrThrowException(projectId);
        Optional<TaskStateEntity> optionalAnotherTaskState = Optional.empty();

        for (TaskStateEntity taskState : project.getTaskStates()) {
            if (taskState.getName().equalsIgnoreCase(taskStateName)) {
                throw new BadRequestException(
                        String.format("Task state \"%s\" already exists.", taskStateName), HttpStatus.BAD_REQUEST);
            }
            if (taskState.getRightTaskState().isEmpty()) {
                optionalAnotherTaskState = Optional.of(taskState);
                break;
            }
        }

        TaskStateEntity taskState = taskStateRepository.save(
                TaskStateEntity.builder()
                        .name(taskStateName)
                        .project(project)
                        .build()
        );

        optionalAnotherTaskState
                .ifPresent(anotherTaskState -> {
                    taskState.setLeftTaskState(anotherTaskState);
                    anotherTaskState.setRightTaskState(taskState);
                    taskStateRepository.save(anotherTaskState);
                });

        final TaskStateEntity savedTaskState = taskStateRepository.save(taskState);

        return taskStateDtoFactory.makeTaskStateDto(savedTaskState);
    }

    @CacheEvict(value = "taskStates", key = "#taskStateId")
    @Transactional
    public TaskStateDto editTaskState(Long taskStateId, String taskStateName) {
        log.info("Editing task state with ID: {}, new name: {}", taskStateId, taskStateName);

        if (taskStateName.isBlank()) {
            throw new BadRequestException("Task state name can't be empty.", HttpStatus.BAD_REQUEST);
        }

        TaskStateEntity taskState = serviceHelper.getTaskStateOrThrowException(taskStateId);

        taskStateRepository
                .findTaskStateEntityByProjectIdAndNameContainsIgnoreCase(
                        taskState.getProject().getId(),
                        taskStateName
                )
                .filter(anotherTaskState -> !anotherTaskState.getId().equals(taskStateId))
                .ifPresent(anotherTaskState -> {
                    throw new BadRequestException(
                            String.format("Task state \"%s\" already exists.", taskStateName), HttpStatus.BAD_REQUEST);
                });

        taskState.setName(taskStateName);
        taskState = taskStateRepository.save(taskState);

        return taskStateDtoFactory.makeTaskStateDto(taskState);
    }

    @CacheEvict(value = "taskStates", key = "#taskStateId")
    @Transactional
    public AckDto deleteTaskState(Long taskStateId) {
        log.warn("Deleting task state with ID: {}", taskStateId);

        TaskStateEntity changeTaskState = serviceHelper.getTaskStateOrThrowException(taskStateId);

        serviceHelper.replaceOldTaskStatePosition(changeTaskState);
        taskStateRepository.deleteById(taskStateId);

        return AckDto.builder().answer(true).build();
    }

    @CacheEvict(value = "taskStates", key = "#taskStateId")
    @Transactional
    public TaskStateDto changeTaskStatePosition(Long taskStateId, Optional<Long> optionalLeftTaskStateId) {
        log.info("Changing task state position for task state ID: {}, left state ID: {}",
                taskStateId, optionalLeftTaskStateId.orElse(null));

        TaskStateEntity changeTaskState = serviceHelper.getTaskStateOrThrowException(taskStateId);
        ProjectEntity project = changeTaskState.getProject();

        Optional<Long> optionalOldLeftTaskStateId = changeTaskState
                .getLeftTaskState()
                .map(TaskStateEntity::getId);

        if (optionalOldLeftTaskStateId.equals(optionalLeftTaskStateId)) {
            return taskStateDtoFactory.makeTaskStateDto(changeTaskState);
        }

        Optional<TaskStateEntity> optionalNewLeftTaskState = optionalLeftTaskStateId
                .map(leftTaskStateId -> {

                    if (taskStateId.equals(leftTaskStateId)) {
                        throw new BadRequestException(
                                "Left task state id equals changed task state.", HttpStatus.BAD_REQUEST);
                    }

                    TaskStateEntity leftTaskStateEntity = serviceHelper.getTaskStateOrThrowException(leftTaskStateId);

                    if (!project.getId().equals(leftTaskStateEntity.getProject().getId())) {
                        throw new BadRequestException(
                                "Task state position can be changed within the same project.", HttpStatus.BAD_REQUEST);
                    }

                    return leftTaskStateEntity;
                });

        Optional<TaskStateEntity> optionalNewRightTaskState;
        if (optionalNewLeftTaskState.isEmpty()) {
            optionalNewRightTaskState = project
                    .getTaskStates()
                    .stream()
                    .filter(anotherTaskState -> anotherTaskState.getLeftTaskState().isEmpty())
                    .findAny();
        } else {

            optionalNewRightTaskState = optionalNewLeftTaskState
                    .get()
                    .getRightTaskState();
        }

        serviceHelper.replaceOldTaskStatePosition(changeTaskState);

        if (optionalNewLeftTaskState.isPresent()) {
            TaskStateEntity newLeftTaskState = optionalNewLeftTaskState.get();
            newLeftTaskState.setRightTaskState(changeTaskState);
            changeTaskState.setLeftTaskState(newLeftTaskState);
        } else {
            changeTaskState.setLeftTaskState(null);
        }

        if (optionalNewRightTaskState.isPresent()) {
            TaskStateEntity newRightTaskState = optionalNewRightTaskState.get();
            newRightTaskState.setLeftTaskState(changeTaskState);
            changeTaskState.setRightTaskState(newRightTaskState);
        } else {
            changeTaskState.setRightTaskState(null);
        }

        changeTaskState = taskStateRepository.save(changeTaskState);

        optionalNewLeftTaskState
                .ifPresent(taskStateRepository::save);

        optionalNewRightTaskState
                .ifPresent(taskStateRepository::save);

        return taskStateDtoFactory.makeTaskStateDto(changeTaskState);
    }
}
