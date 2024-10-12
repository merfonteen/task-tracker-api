package by.sirius.task.tracker.api.services.helpers;

import by.sirius.task.tracker.api.exceptions.NotFoundException;
import by.sirius.task.tracker.store.entities.*;
import by.sirius.task.tracker.store.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class ServiceHelper {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ProjectRepository projectRepository;
    private final TaskStateRepository taskStateRepository;
    private final InvitationRepository invitationRepository;

    public ProjectEntity getProjectOrThrowException(Long projectId) {
        return projectRepository
                .findById(projectId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Project with \"%d\" id doesn't exist", projectId), HttpStatus.NOT_FOUND)
                );
    }

    public TaskStateEntity getTaskStateOrThrowException(Long taskStateId) {
        return taskStateRepository
                .findById(taskStateId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Task state with \"%d\" id doesn't exist.", taskStateId), HttpStatus.NOT_FOUND)
                );
    }

    public TaskEntity getTaskOrThrowException(Long taskId) {
        return taskRepository
                .findById(taskId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Task with \"%d\" id doesn't exist", taskId), HttpStatus.BAD_REQUEST)
                );
    }

    public UserEntity getUserOrThrowException(String username) {
        return userRepository
                .findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found", HttpStatus.NOT_FOUND));

    }

    public RoleEntity getUserRoleOrThrowException() {
        return roleRepository
                .findByName("ROLE_USER")
                .orElseThrow(() -> new NotFoundException("User role not found", HttpStatus.NOT_FOUND));
    }

    public RoleEntity getAdminRoleOrThrowException() {
        return roleRepository
                .findByName("ROLE_ADMIN")
                .orElseThrow(() -> new NotFoundException("Admin role not found", HttpStatus.NOT_FOUND));
    }

    public InvitationEntity getInvitationOrThrowException(Long invitationId) {
        return invitationRepository
                .findById(invitationId)
                .orElseThrow(() -> new NotFoundException("Invitation not found", HttpStatus.NOT_FOUND));
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
