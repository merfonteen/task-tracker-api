package by.sirius.task.tracker.api.controllers;

import by.sirius.task.tracker.api.dto.AckDto;
import by.sirius.task.tracker.api.dto.TaskDto;
import by.sirius.task.tracker.api.dto.TaskHistoryDto;
import by.sirius.task.tracker.api.exceptions.BadRequestException;
import by.sirius.task.tracker.core.services.ProjectSecurityService;
import by.sirius.task.tracker.core.services.TaskHistoryService;
import by.sirius.task.tracker.core.services.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@RestController
@Slf4j
public class TaskController {

    private final TaskService taskService;
    private final TaskHistoryService taskHistoryService;
    private final ProjectSecurityService projectSecurityService;

    public static final String GET_TASK_BY_ID = "/api/projects/{project_id}/task-states/{task_state_id}/tasks/{task_id}";
    public static final String GET_TASKS = "/api/projects/{project_id}/task-states/{task_state_id}/tasks";
    public static final String GET_USER_TASKS = "/api/projects/{project_id}/users/{username}/tasks";
    public static final String GET_TASK_HISTORY = "/api/tasks/{task_id}/history";
    public static final String CREATE_TASK = "/api/projects/{project_id}/task-states/{task_state_id}/tasks";
    public static final String EDIT_TASK = "/api/tasks/{task_id}";
    public static final String DELETE_TASK = "/api/tasks/{task_id}";
    public static final String ASSIGN_TASK_TO_SPECIFIC_USER = "/api/tasks/{task_id}/assign";
    public static final String CHANGE_TASK_STATE = "/api/tasks/{task_id}/state/change";
    public static final String CHANGE_TASK_POSITION = "/api/tasks/{task_id}/position/change";

    @PreAuthorize("@projectSecurityService.hasProjectPermission(#projectId, 'READ')")
    @GetMapping(GET_TASK_BY_ID)
    public TaskDto getTaskById(@PathVariable("project_id") Long projectId,
                               @PathVariable("task_state_id") Long taskStateId,
                               @PathVariable(name = "task_id") Long taskId) {
        return taskService.getTaskById(projectId, taskStateId, taskId);
    }

    @PreAuthorize("@projectSecurityService.hasProjectPermission(#projectId, 'READ')")
    @GetMapping(GET_TASKS)
    public List<TaskDto> getTasks(@PathVariable("project_id") Long projectId,
                                  @PathVariable("task_state_id") Long taskStateId) {
        log.debug("Fetching tasks for project ID: {} and task state ID: {}", projectId, taskStateId);
        return taskService.getTasks(projectId, taskStateId);
    }

    @PreAuthorize("@projectSecurityService.hasProjectPermission(#projectId, 'READ')")
    @GetMapping(GET_USER_TASKS)
    public List<TaskDto> getAssignedTasks( @PathVariable("project_id") Long projectId,
                                           @PathVariable("username") String username,
                                           Principal principal) {
        log.debug("Fetching assigned tasks for project ID: {} and user: {}", projectId, username);
        if (!principal.getName().equals(username) && !projectSecurityService.isAdminOfProject(projectId, principal.getName())) {
            throw new BadRequestException("You can only view your own tasks or as an admin.", HttpStatus.BAD_REQUEST);
        }
        return taskService.getAssignedTasks(username);
    }

    @PreAuthorize("@projectSecurityService.hasTaskPermission(#taskId, 'WRITE')")
    @GetMapping(GET_TASK_HISTORY)
    public List<TaskHistoryDto> getTaskHistory(@PathVariable("task_id") Long taskId) {
        return taskHistoryService.getTaskHistoryByTaskId(taskId);
    }

    @PreAuthorize("@projectSecurityService.hasProjectPermission(#projectId, 'WRITE')")
    @PostMapping(CREATE_TASK)
    public TaskDto createTask(@PathVariable("project_id") Long projectId,
                              @PathVariable("task_state_id") Long taskStateId,
                              @RequestParam String taskName) {
        log.info("Creating task '{}' in project ID: {} and task state ID: {}", taskName, projectId, taskStateId);
        return taskService.createTask(projectId, taskStateId, taskName);
    }

    @PreAuthorize("@projectSecurityService.hasTaskPermission(#taskId, 'READ')")
    @PatchMapping(EDIT_TASK)
    public TaskDto editTask(@PathVariable("task_id") Long taskId,
                            @RequestParam String taskName) {
        log.info("Editing task ID: {}, new name: {}", taskId, taskName);
        return taskService.editTask(taskId, taskName);
    }

    @PreAuthorize("@projectSecurityService.hasTaskPermission(#taskId, 'READ')")
    @DeleteMapping(DELETE_TASK)
    public AckDto deleteTask(@PathVariable("task_id") Long taskId) {
        log.warn("Deleting task with ID: {}", taskId);
        return taskService.deleteTask(taskId);
    }

    @PreAuthorize("@projectSecurityService.hasTaskPermission(#taskId, 'WRITE')")
    @PatchMapping(ASSIGN_TASK_TO_SPECIFIC_USER)
    public TaskDto assignTaskToUser(@PathVariable("task_id") Long taskId,
                                    @RequestParam String username) {
        log.info("Assigning task with ID: {} for user: {}", taskId, username);
        return taskService.assignTaskToUser(taskId, username);
    }

    @PreAuthorize("@projectSecurityService.hasTaskPermission(#taskId, 'READ')")
    @PatchMapping(CHANGE_TASK_STATE)
    public TaskDto changeTaskState(@PathVariable("task_id") Long taskId,
                                   @RequestParam Long newTaskStateId) {
        log.info("Changing task state for task ID: {} to new task state ID: {}", taskId, newTaskStateId);
        return taskService.changeTaskState(taskId, newTaskStateId);
    }

    @PreAuthorize("@projectSecurityService.hasTaskPermission(#taskId, 'READ')")
    @PatchMapping(CHANGE_TASK_POSITION)
    public TaskDto changeTaskPosition(
            @PathVariable("task_id") Long taskId,
            @RequestParam(required = false) Optional<Long> leftTaskId) {
        log.info("Changing task position for task ID: {} with left task ID: {}", taskId, leftTaskId.orElse(null));
        return taskService.changeTaskPosition(taskId, leftTaskId);
    }
}
