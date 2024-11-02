package by.sirius.task.tracker.api.services.helpers;

import by.sirius.task.tracker.api.exceptions.NotFoundException;
import by.sirius.task.tracker.api.store.entities.*;
import by.sirius.task.tracker.api.store.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
@Component
public class ServiceHelper {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ProjectRepository projectRepository;
    private final TaskStateRepository taskStateRepository;
    private final InvitationRepository invitationRepository;
    private final TaskHistoryRepository taskHistoryRepository;

    public ProjectEntity getProjectOrThrowException(Long projectId) {
        return projectRepository
                .findById(projectId)
                .orElseThrow(() -> {
                    log.error("Project with id {} not found", projectId);
                    return new NotFoundException(
                            String.format("Project with \"%d\" id doesn't exist", projectId), HttpStatus.NOT_FOUND);
                });
    }

    public TaskStateEntity getTaskStateOrThrowException(Long taskStateId) {
        return taskStateRepository
                .findById(taskStateId)
                .orElseThrow(() -> {
                    log.error("Task state with id {} not found", taskStateId);
                    return new NotFoundException(
                            String.format("Task state with id \"%d\" doesn't exist.", taskStateId), HttpStatus.NOT_FOUND
                    );
                });
    }

    public TaskEntity getTaskOrThrowException(Long taskId) {
        return taskRepository
                .findById(taskId)
                .orElseThrow(() -> {
                    log.error("Task with id {} not found", taskId);
                    return new NotFoundException(
                            String.format("Task with id \"%d\" doesn't exist", taskId), HttpStatus.BAD_REQUEST
                    );
                });
    }

    public UserEntity getUserOrThrowException(String username) {
        return userRepository
                .findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User with username {} not found", username);
                    return new NotFoundException("User not found", HttpStatus.NOT_FOUND);
                });
    }

    public RoleEntity getUserRoleOrThrowException() {
        return roleRepository
                .findByName("ROLE_USER")
                .orElseThrow(() -> {
                    log.error("User role 'ROLE_USER' not found");
                    return new NotFoundException("User role not found", HttpStatus.NOT_FOUND);
                });
    }

    public RoleEntity getAdminRoleOrThrowException() {
        return roleRepository
                .findByName("ROLE_ADMIN")
                .orElseThrow(() -> {
                    log.error("Admin role 'ROLE_ADMIN' not found");
                    return new NotFoundException("Admin role not found", HttpStatus.NOT_FOUND);
                });
    }

    public InvitationEntity getInvitationOrThrowException(Long invitationId) {
        return invitationRepository
                .findById(invitationId)
                .orElseThrow(() -> {
                    log.error("Invitation with id {} not found", invitationId);
                    return new NotFoundException("Invitation not found", HttpStatus.NOT_FOUND);
                });
    }

    public TaskHistoryEntity getTaskHistoryOrThrowException(Long taskHistoryId) {
        return taskHistoryRepository
                .findById(taskHistoryId)
                .orElseThrow(() -> {
                    log.error("Task history with id {} not found", taskHistoryId);
                    return new NotFoundException("Task history not found", HttpStatus.NOT_FOUND);
                });
    }

    public void replaceOldTaskStatePosition(TaskStateEntity changeTaskState) {

        Optional<TaskStateEntity> optionalOldLeftTaskState = changeTaskState.getLeftTaskState();
        Optional<TaskStateEntity> optionalOldRightTaskState = changeTaskState.getRightTaskState();

        optionalOldLeftTaskState
                .ifPresent(it -> {

                    it.setRightTaskState(optionalOldRightTaskState.orElse(null));

                    taskStateRepository.saveAndFlush(it);
                });

        optionalOldRightTaskState
                .ifPresent(it -> {

                    it.setLeftTaskState(optionalOldLeftTaskState.orElse(null));

                    taskStateRepository.saveAndFlush(it);
                });
    }

    public void replaceOldTaskPosition(TaskEntity changeTask) {

        Optional<TaskEntity> optionalOldLeftTask = changeTask.getLeftTask();
        Optional<TaskEntity> optionalOldRightTask = changeTask.getRightTask();

        optionalOldLeftTask
                .ifPresent(it -> {
                    it.setRightTask(optionalOldRightTask.orElse(null));
                    taskRepository.saveAndFlush(it);
                });

        optionalOldRightTask
                .ifPresent(it -> {
                    it.setLeftTask(optionalOldLeftTask.orElse(null));
                    taskRepository.saveAndFlush(it);
                });
    }
}
