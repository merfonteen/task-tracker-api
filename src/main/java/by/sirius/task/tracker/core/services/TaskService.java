package by.sirius.task.tracker.core.services;

import by.sirius.task.tracker.api.dto.AckDto;
import by.sirius.task.tracker.api.dto.TaskDto;
import by.sirius.task.tracker.api.exceptions.BadRequestException;
import by.sirius.task.tracker.api.exceptions.NotFoundException;
import by.sirius.task.tracker.core.factories.TaskDtoFactory;
import by.sirius.task.tracker.core.services.helpers.ServiceHelper;
import by.sirius.task.tracker.store.entities.*;
import by.sirius.task.tracker.store.repositories.TaskHistoryRepository;
import by.sirius.task.tracker.store.repositories.TaskRepository;
import by.sirius.task.tracker.store.repositories.TaskStateRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class TaskService {

    private final EmailService emailService;
    private final TaskDtoFactory taskDtoFactory;
    private final TaskRepository taskRepository;
    private final TaskStateRepository taskStateRepository;
    private final TaskHistoryRepository taskHistoryRepository;

    private final ServiceHelper serviceHelper;

    public TaskDto getTaskById(Long projectId, Long taskStateId, Long taskId) {
        serviceHelper.findProjectByIdOrThrowException(projectId);
        TaskEntity task = serviceHelper.findTaskByIdOrThrowException(taskId);

        TaskStateEntity taskStateWithTask = taskStateRepository.findByProjectIdAndId(projectId, taskStateId)
                .orElseThrow(() -> new NotFoundException("Task state not found", HttpStatus.NOT_FOUND));

        if (!taskStateWithTask.getTasks().contains(task)) {
            throw new BadRequestException("Task state with id '%d' doesn't contain task with id '%d'"
                    .formatted(taskStateId, taskId),
                    HttpStatus.BAD_REQUEST);
        }

        return taskDtoFactory.makeTaskDto(task);
    }

    public List<TaskDto> getTasks(Long projectId, Long taskStateId) {
        log.debug("Fetching tasks for project ID: {} and task state ID: {}", projectId, taskStateId);

        Optional<TaskStateEntity> taskState = taskStateRepository.findByProjectIdAndId(projectId, taskStateId);

        return taskState
                .map(state -> state.getTasks()
                        .stream()
                        .map(taskDtoFactory::makeTaskDto)
                        .collect(Collectors.toList()))
                .orElseThrow(() -> new NotFoundException(
                        String.format("Task state with id \"%d\" not found", taskStateId), HttpStatus.BAD_REQUEST));
    }

    public List<TaskDto> getAssignedTasks(String username) {
        log.debug("Getting assigned for project user: {}", username);

        UserEntity user = serviceHelper.findUserByUsernameOrThrowException(username);
        List<TaskEntity> assignedTasks = taskRepository.findByAssignedUserId(user.getId());

        return assignedTasks.stream()
                .map(taskDtoFactory::makeTaskDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskDto createTask(Long projectId, Long taskStateId, String taskName) {
        log.info("Creating task '{}' in project ID: {} and task state ID: {}", taskName, projectId, taskStateId);

        if (taskName.isBlank()) {
            throw new BadRequestException("Task name can't be empty", HttpStatus.BAD_REQUEST);
        }

        ProjectEntity project = serviceHelper.findProjectByIdOrThrowException(projectId);
        TaskStateEntity taskState = serviceHelper.findTaskStateByIdOrThrowException(taskStateId);

        Optional<TaskEntity> optionalAnotherTask = Optional.empty();

        if (!project.getTaskStates().contains(taskState)) {
            throw new NotFoundException("Project doesn't contain a such task state", HttpStatus.NOT_FOUND);
        }

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
                .taskState(taskState)
                .build();

        taskState.getTasks().add(task);

        taskRepository.save(task);

        optionalAnotherTask.ifPresent(it -> {
            task.setLeftTask(it);
            it.setRightTask(task);
            taskRepository.save(it);
        });

        taskStateRepository.save(taskState);

        return taskDtoFactory.makeTaskDto(task);
    }

    @Transactional
    public TaskDto editTask(Long taskId, String taskName) {
        log.info("Editing task ID: {}, new name: {}", taskId, taskName);
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        if (taskName.isBlank()) {
            throw new BadRequestException("Task name can't be empty", HttpStatus.BAD_REQUEST);
        }

        TaskEntity taskToUpdate = serviceHelper.findTaskByIdOrThrowException(taskId);

        validateTaskAction(taskToUpdate, currentUsername, "You are not authorized to edit this task");

        Long taskStateId = taskToUpdate.getTaskState().getId();

        taskRepository
                .findByTaskStateIdAndNameIgnoreCase(taskStateId, taskName)
                .filter(existingTask -> !existingTask.getId().equals(taskId))
                .ifPresent(existingTask -> {
                    throw new BadRequestException(
                            String.format("Task \"%s\" already exists in this task state", taskName), HttpStatus.BAD_REQUEST);
                });

        String oldTaskName = taskToUpdate.getName();
        taskToUpdate.setName(taskName);

        TaskEntity updatedTask = taskRepository.save(taskToUpdate);

        TaskHistoryEntity taskHistory = TaskHistoryEntity.builder()
                .task(updatedTask)
                .username(currentUsername)
                .changeType("EDIT")
                .fieldName("name")
                .oldValue(oldTaskName)
                .newValue(updatedTask.getName())
                .build();

        taskHistoryRepository.save(taskHistory);

        return taskDtoFactory.makeTaskDto(updatedTask);
    }

    @Transactional
    public AckDto deleteTask(Long taskId) {
        log.warn("Deleting task with ID: {}", taskId);
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        TaskEntity taskToDelete = serviceHelper.findTaskByIdOrThrowException(taskId);

        validateTaskAction(taskToDelete, currentUsername, "You are not authorized to remove this task");

        serviceHelper.replaceOldTaskPosition(taskToDelete);

        TaskStateEntity taskState = taskToDelete.getTaskState();
        taskState.getTasks().remove(taskToDelete);

        TaskHistoryEntity taskHistory = TaskHistoryEntity.builder()
                .task(taskToDelete)
                .username(currentUsername)
                .changeType("DELETE")
                .build();

        taskHistoryRepository.save(taskHistory);
        taskStateRepository.save(taskState);
        taskRepository.delete(taskToDelete);

        return AckDto.builder().answer(true).build();
    }

    @Transactional
    public TaskDto changeTaskPosition(Long taskId, Optional<Long> optionalLeftTaskId) {
        log.info("Changing task position for task ID: {} with left task ID: {}",
                taskId, optionalLeftTaskId.orElse(null));
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        TaskEntity changeTask = serviceHelper.findTaskByIdOrThrowException(taskId);
        TaskStateEntity taskState = changeTask.getTaskState();

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
                                "Left task id equals changed task", HttpStatus.BAD_REQUEST);
                    }

                    TaskEntity leftTaskEntity = serviceHelper.findTaskByIdOrThrowException(leftTaskId);

                    if (!taskState.getId().equals(leftTaskEntity.getTaskState().getId())) {
                        throw new BadRequestException(
                                "Task position can only be changed within the same task state", HttpStatus.BAD_REQUEST);
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

        changeTask = taskRepository.save(changeTask);

        optionalNewLeftTask
                .ifPresent(taskRepository::save);

        optionalNewRightTask
                .ifPresent(taskRepository::save);

        taskStateRepository.save(taskState);

        TaskHistoryEntity taskHistory = TaskHistoryEntity.builder()
                .task(changeTask)
                .username(currentUsername)
                .changeType("EDIT")
                .fieldName("task position")
                .oldValue(optionalOldLeftTaskId.map(String::valueOf).orElse(null))
                .newValue(optionalNewLeftTask.map(task -> String.valueOf(task.getId())).orElse(null))
                .build();

        taskHistoryRepository.save(taskHistory);

        return taskDtoFactory.makeTaskDto(changeTask);
    }

    @Transactional
    public TaskDto changeTaskState(Long taskId, Long newTaskStateId) {
        log.info("Changing task state for task ID: {} to new task state ID: {}", taskId, newTaskStateId);
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        TaskEntity taskToMove = serviceHelper.findTaskByIdOrThrowException(taskId);
        TaskStateEntity newTaskState = serviceHelper.findTaskStateByIdOrThrowException(newTaskStateId);
        TaskStateEntity currentTaskState = taskToMove.getTaskState();

        newTaskState.getTasks()
                .stream()
                .map(TaskEntity::getName)
                .filter(taskName -> taskName.equals(taskToMove.getName()))
                .findAny()
                .ifPresent(existingTask -> {
                    throw new BadRequestException(
                            String.format("Task state \"%s\" already contains  task name \"%s\"",
                                    newTaskState.getName(), taskToMove.getName()), HttpStatus.BAD_REQUEST);
                });

        serviceHelper.replaceOldTaskPosition(taskToMove);

        currentTaskState.getTasks().remove(taskToMove);

        Optional<TaskEntity> optionalLastTaskInNewState = newTaskState.getTasks()
                .stream()
                .filter(task -> task.getRightTask().isEmpty())
                .findAny();

        optionalLastTaskInNewState.ifPresent(lastTask -> {
            lastTask.setRightTask(taskToMove);
            taskToMove.setLeftTask(lastTask);
            taskRepository.save(lastTask);
        });

        taskToMove.setRightTask(null);
        taskToMove.setTaskState(newTaskState);

        newTaskState.getTasks().add(taskToMove);

        TaskEntity updatedTask = taskRepository.save(taskToMove);

        TaskHistoryEntity taskHistory = TaskHistoryEntity.builder()
                .task(updatedTask)
                .username(currentUsername)
                .changeType("EDIT")
                .fieldName("task state")
                .oldValue(taskToMove.getTaskState().getName())
                .newValue(updatedTask.getTaskState().getName())
                .build();

        taskHistoryRepository.save(taskHistory);
        taskStateRepository.save(currentTaskState);
        taskStateRepository.save(newTaskState);

        return taskDtoFactory.makeTaskDto(updatedTask);
    }

    @Transactional
    public TaskDto assignTaskToUser(Long taskId, String username) {
        log.info("Assigning task with ID: {} for user: {}", taskId, username);
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        TaskEntity task = serviceHelper.findTaskByIdOrThrowException(taskId);
        TaskStateEntity taskState = task.getTaskState();
        ProjectEntity project = taskState.getProject();
        UserEntity user = serviceHelper.findUserByUsernameOrThrowException(username);

        if (!project.getUsers().contains(user)) {
            throw new BadRequestException("Project doesn't contain user: " + username, HttpStatus.BAD_REQUEST);
        }

        String usernameBefore = task.getAssignedUser().getUsername();

        task.setAssignedUser(user);
        taskRepository.save(task);

        emailService.sendEmail(
                user.getEmail(),
                "You have been assigned a task",
                "You have been assigned to the task: " + task.getName()
        );

        TaskHistoryEntity taskHistory = TaskHistoryEntity.builder()
                .task(task)
                .username(currentUsername)
                .changeType("EDIT")
                .fieldName("assigned user")
                .oldValue(usernameBefore)
                .newValue(task.getAssignedUser().getUsername())
                .build();

        taskHistoryRepository.save(taskHistory);

        return taskDtoFactory.makeTaskDto(task);
    }

    private static void validateTaskAction(TaskEntity taskToDelete, String currentUsername, String exMessage) {
        if (!taskToDelete.getAssignedUser().getUsername().equals(currentUsername) ||
                !taskToDelete.getTaskState().getProject().getAdmin().getUsername().equals(currentUsername)) {
            throw new BadRequestException(exMessage, HttpStatus.BAD_REQUEST);
        }
    }
}
