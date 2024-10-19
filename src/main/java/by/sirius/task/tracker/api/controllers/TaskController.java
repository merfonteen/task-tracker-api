package by.sirius.task.tracker.api.controllers;

import by.sirius.task.tracker.api.dto.AckDto;
import by.sirius.task.tracker.api.dto.TaskDto;
import by.sirius.task.tracker.api.services.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@RestController
@Slf4j
public class TaskController {

    private final TaskService taskService;

    public static final String GET_TASKS = "/api/projects/{project_id}/task-states/{task_state_id}/tasks";
    public static final String CREATE_TASK = "/api/projects/{project_id}/task-states/{task_state_id}/tasks";
    public static final String EDIT_TASK = "/api/tasks/{task_id}";
    public static final String CHANGE_TASK_POSITION = "/api/tasks/{task_id}/position/change";
    public static final String DELETE_TASK = "/api/tasks/{task_id}";
    public static final String CHANGE_TASK_STATE = "/api/tasks/{task_id}/state/change";

    @PreAuthorize("@projectSecurityService.hasProjectPermission(#projectId, 'READ')")
    @GetMapping(GET_TASKS)
    public List<TaskDto> getTasks(@PathVariable(name = "project_id") Long projectId,
                                  @PathVariable(name = "task_state_id") Long taskStateId) {
        log.debug("Fetching tasks for project ID: {} and task state ID: {}", projectId, taskStateId);
        return taskService.getTasks(projectId, taskStateId);
    }

    @PreAuthorize("@projectSecurityService.hasProjectPermission(#projectId, 'WRITE')")
    @PostMapping(CREATE_TASK)
    public TaskDto createTask(@PathVariable(name = "project_id") Long projectId,
                              @PathVariable(name = "task_state_id") Long taskStateId,
                              @RequestParam String taskName) {
        log.info("Creating task '{}' in project ID: {} and task state ID: {}", taskName, projectId, taskStateId);
        return taskService.createTask(projectId, taskStateId, taskName);
    }

    @PreAuthorize("@projectSecurityService.hasTaskPermission(#taskId, 'WRITE')")
    @PatchMapping(EDIT_TASK)
    public TaskDto editTask(@PathVariable(name = "task_id") Long taskId,
                            @RequestParam String taskName) {
        log.info("Editing task ID: {}, new name: {}", taskId, taskName);
        return taskService.editTask(taskId, taskName);
    }

    @PreAuthorize("@projectSecurityService.hasTaskPermission(#taskId, 'READ')")
    @PatchMapping(CHANGE_TASK_POSITION)
    public TaskDto changeTaskPosition(
            @PathVariable(name = "task_id") Long taskId,
            @RequestParam(required = false) Optional<Long> leftTaskId) {
        log.info("Changing task position for task ID: {} with left task ID: {}", taskId, leftTaskId.orElse(null));
        return taskService.changeTaskPosition(taskId, leftTaskId);
    }

    @PreAuthorize("@projectSecurityService.hasTaskPermission(#taskId, 'WRITE')")
    @DeleteMapping(DELETE_TASK)
    public AckDto deleteTask(@PathVariable(name = "task_id") Long taskId) {
        log.warn("Deleting task with ID: {}", taskId);
        return taskService.deleteTask(taskId);
    }

    @PreAuthorize("@projectSecurityService.hasTaskPermission(#taskId, 'READ')")
    @PatchMapping(CHANGE_TASK_STATE)
    public TaskDto changeTaskState(@PathVariable(name = "task_id") Long taskId,
                                   @RequestParam Long newTaskStateId) {
        log.info("Changing task state for task ID: {} to new task state ID: {}", taskId, newTaskStateId);
        return taskService.changeTaskState(taskId, newTaskStateId);
    }
}
