package by.sirius.task.tracker.store.services.helpers;

import by.sirius.task.tracker.api.exceptions.NotFoundException;
import by.sirius.task.tracker.store.entities.ProjectEntity;
import by.sirius.task.tracker.store.entities.TaskEntity;
import by.sirius.task.tracker.store.entities.TaskStateEntity;
import by.sirius.task.tracker.store.entities.UserEntity;
import by.sirius.task.tracker.store.repositories.ProjectRepository;
import by.sirius.task.tracker.store.repositories.TaskRepository;
import by.sirius.task.tracker.store.repositories.TaskStateRepository;
import by.sirius.task.tracker.store.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class ServiceHelper {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TaskStateRepository taskStateRepository;

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

    public TaskEntity getTaskEntityOrThrowException(Long taskId) {
        return taskRepository
                .findById(taskId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Task with \"%d\" id doesn't exist", taskId), HttpStatus.BAD_REQUEST)
                );
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
