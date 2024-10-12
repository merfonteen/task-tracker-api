package by.sirius.task.tracker.api.controllers;

import by.sirius.task.tracker.api.dto.AckDto;
import by.sirius.task.tracker.api.dto.TaskStateDto;
import by.sirius.task.tracker.api.services.TaskStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@RestController
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
        return taskStateService.getTaskStates(projectId);
    }

    @PreAuthorize("@projectSecurityService.hasProjectPermission(#projectId, 'WRITE')")
    @PostMapping(CREATE_TASK_STATE)
    public TaskStateDto createTaskState(
            @PathVariable(name = "project_id") Long projectId,
            @RequestParam(name = "task_state_name") String taskStateName) {
        return taskStateService.createTaskState(projectId, taskStateName);
    }

    @PreAuthorize("@projectSecurityService.hasTaskStatePermission(#taskStateId, 'WRITE')")
    @PatchMapping(EDIT_TASK_STATE)
    public TaskStateDto editTaskState(
            @PathVariable(name = "task_state_id") Long taskStateId,
            @RequestParam(name = "task_state_name") String taskStateName) {
        return taskStateService.editTaskState(taskStateId, taskStateName);
    }

    @PreAuthorize("@projectSecurityService.hasTaskStatePermission(#taskStateId, 'WRITE')")
    @PatchMapping(CHANGE_TASK_STATE_POSITION)
    public TaskStateDto changeTaskStatePosition(
            @PathVariable(name = "task_state_id") Long taskStateId,
            @RequestParam(name = "optional_left_task_state_id", required = false) Optional<Long> optionalLeftTaskStateId) {
        return taskStateService.changeTaskStatePosition(taskStateId, optionalLeftTaskStateId);
    }

    @PreAuthorize("@projectSecurityService.hasTaskStatePermission(#taskStateId, 'WRITE')")
    @DeleteMapping(DELETE_TASK_STATE)
    public AckDto deleteTaskState(@PathVariable(name = "task_state_id") Long taskStateId) {
        return taskStateService.deleteTaskState(taskStateId);
    }
}
