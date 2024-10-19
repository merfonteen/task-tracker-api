package by.sirius.task.tracker.api.controllers;

import by.sirius.task.tracker.api.dto.AckDto;
import by.sirius.task.tracker.api.dto.TaskStateDto;
import by.sirius.task.tracker.api.services.TaskStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@RestController
@Slf4j
public class TaskStateController {

    private final TaskStateService taskStateService;

    public static final String GET_TASK_STATES = "/api/projects/{project_id}/task-states";
    public static final String CREATE_TASK_STATE = "/api/projects/{project_id}/task-states";
    public static final String EDIT_TASK_STATE = "/api/task-states/{task_state_id}";
    public static final String CHANGE_TASK_STATE_POSITION = "/api/task-states/{task_state_id}/position/change";
    public static final String DELETE_TASK_STATE = "/api/task-states/{task_state_id}";

    @PreAuthorize("@projectSecurityService.hasProjectPermission(#projectId, 'READ')")
    @GetMapping(GET_TASK_STATES)
    public List<TaskStateDto> getTaskStates(@PathVariable(name = "project_id") Long projectId) {
        log.debug("Fetching task states for project ID: {}", projectId);
        return taskStateService.getTaskStates(projectId);
    }

    @PreAuthorize("@projectSecurityService.hasProjectPermission(#projectId, 'WRITE')")
    @PostMapping(CREATE_TASK_STATE)
    public TaskStateDto createTaskState(
            @PathVariable(name = "project_id") Long projectId,
            @RequestParam String taskStateName) {
        log.info("Creating task state '{}' in project with ID: {}", taskStateName, projectId);
        return taskStateService.createTaskState(projectId, taskStateName);
    }

    @PreAuthorize("@projectSecurityService.hasTaskStatePermission(#taskStateId, 'WRITE')")
    @PatchMapping(EDIT_TASK_STATE)
    public TaskStateDto editTaskState(
            @PathVariable(name = "task_state_id") Long taskStateId,
            @RequestParam String taskStateName) {
        log.info("Editing task state with ID: {}, new name: {}", taskStateId, taskStateName);
        return taskStateService.editTaskState(taskStateId, taskStateName);
    }

    @PreAuthorize("@projectSecurityService.hasTaskStatePermission(#taskStateId, 'WRITE')")
    @PatchMapping(CHANGE_TASK_STATE_POSITION)
    public TaskStateDto changeTaskStatePosition(
            @PathVariable(name = "task_state_id") Long taskStateId,
            @RequestParam(required = false) Optional<Long> optionalLeftTaskStateId) {
        log.info("Changing task state position for task state ID: {}, left state ID: {}", taskStateId, optionalLeftTaskStateId.orElse(null));
        return taskStateService.changeTaskStatePosition(taskStateId, optionalLeftTaskStateId);
    }

    @PreAuthorize("@projectSecurityService.hasTaskStatePermission(#taskStateId, 'WRITE')")
    @DeleteMapping(DELETE_TASK_STATE)
    public AckDto deleteTaskState(@PathVariable(name = "task_state_id") Long taskStateId) {
        log.warn("Deleting task state with ID: {}", taskStateId);
        return taskStateService.deleteTaskState(taskStateId);
    }
}
