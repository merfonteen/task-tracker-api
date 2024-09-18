package by.sirius.task.tracker.api.controllers;

import by.sirius.task.tracker.api.dto.AckDto;
import by.sirius.task.tracker.api.dto.TaskStateDto;
import by.sirius.task.tracker.store.services.TaskStateService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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

    @GetMapping(GET_TASK_STATES)
    public List<TaskStateDto> getTaskStates(@PathVariable(name = "project_id") Long projectId) {
        return taskStateService.getTaskStates(projectId);
    }

    @PostMapping(CREATE_TASK_STATE)
    @Transactional
    public TaskStateDto createTaskState(
            @PathVariable(name = "project_id") Long projectId,
            @RequestParam(name = "task_state_name") String taskStateName) {
        return taskStateService.createTaskState(projectId, taskStateName);
    }

    @PatchMapping(EDIT_TASK_STATE)
    @Transactional
    public TaskStateDto editTaskState(
            @PathVariable(name = "task_state_id") Long taskStateId,
            @RequestParam(name = "task_state_name") String taskStateName) {
        return taskStateService.editTaskState(taskStateId, taskStateName);
    }

    @PatchMapping(CHANGE_TASK_STATE_POSITION)
    public TaskStateDto changeTaskStatePosition(
            @PathVariable(name = "task_state_id") Long taskStateId,
            @RequestParam(name = "optional_left_task_state_id", required = false) Optional<Long> optionalLeftTaskStateId) {
        return taskStateService.changeTaskStatePosition(taskStateId, optionalLeftTaskStateId);
    }

    @DeleteMapping(DELETE_TASK_STATE)
    @Transactional
    public AckDto deleteTaskState(@PathVariable(name = "task_state_id") Long taskStateId) {
        return taskStateService.deleteTaskState(taskStateId);
    }
}
