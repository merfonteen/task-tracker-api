package by.sirius.task.tracker.api.services;

import by.sirius.task.tracker.api.dto.TaskDto;
import by.sirius.task.tracker.api.exceptions.BadRequestException;
import by.sirius.task.tracker.api.exceptions.NotFoundException;
import by.sirius.task.tracker.core.factories.TaskDtoFactory;
import by.sirius.task.tracker.core.services.TaskService;
import by.sirius.task.tracker.core.services.helpers.ServiceHelper;
import by.sirius.task.tracker.store.entities.ProjectEntity;
import by.sirius.task.tracker.store.entities.TaskEntity;
import by.sirius.task.tracker.store.entities.TaskStateEntity;
import by.sirius.task.tracker.store.entities.UserEntity;
import by.sirius.task.tracker.store.repositories.TaskHistoryRepository;
import by.sirius.task.tracker.store.repositories.TaskRepository;
import by.sirius.task.tracker.store.repositories.TaskStateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private ServiceHelper serviceHelper;

    @Mock
    private TaskDtoFactory taskDtoFactory;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskStateRepository taskStateRepository;

    @Mock
    private TaskHistoryRepository taskHistoryRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void testGetTasks_Success() {
        Long projectId = 1L;
        Long taskStateId = 1L;

        TaskEntity task = TaskEntity.builder()
                .id(1L)
                .name("task1")
                .build();

        TaskDto taskDto = TaskDto.builder()
                .id(1L)
                .name("task1")
                .build();

        TaskStateEntity taskState = TaskStateEntity.builder()
                .id(taskStateId)
                .name("To do")
                .build();

        taskState.getTasks().add(task);

        when(taskStateRepository.findByProjectIdAndId(projectId, taskStateId)).thenReturn(Optional.of(taskState));
        when(taskDtoFactory.makeTaskDto(task)).thenReturn(taskDto);

        List<TaskDto> tasks = taskService.getTasks(projectId, taskStateId);

        assertEquals(1, tasks.size());
        assertEquals("task1", tasks.get(0).getName());
    }

    @Test
    void testGetTasks_WhenTaskStateNotFound_ShouldThrowException() {
        Long projectId = 1L;
        Long taskStateId = 1L;

        when(taskStateRepository.findByProjectIdAndId(projectId, taskStateId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> taskService.getTasks(projectId, taskStateId));
    }

    @Test
    void testGetAssignedTasks_Success() {
        String username = "username";

        UserEntity user = UserEntity.builder()
                .id(1L)
                .username(username)
                .build();

        TaskEntity task = TaskEntity.builder()
                .id(1L)
                .name("task1")
                .assignedUser(user)
                .build();

        TaskDto taskDto = TaskDto.builder()
                .id(1L)
                .name("task1")
                .assignedUser(user.getUsername())
                .build();

        when(serviceHelper.getUserOrThrowException(username)).thenReturn(user);
        when(taskRepository.findByAssignedUser(user)).thenReturn(List.of(task));
        when(taskDtoFactory.makeTaskDto(task)).thenReturn(taskDto);

        List<TaskDto> assignedTasks = taskService.getAssignedTasks(username);

        assertEquals(1, assignedTasks.size());
        assertEquals("task1", assignedTasks.get(0).getName());
    }

    @Test
    void testGetAssignedTasks_WhenUserNotFound_ShouldThrowException() {
        String username = "username";

        when(serviceHelper.getUserOrThrowException(username)).thenThrow(
                new NotFoundException("User not found", HttpStatus.NOT_FOUND));

        assertThrows(NotFoundException.class, () -> taskService.getAssignedTasks(username));
    }

    @Test
    void testGetAssignedTasks_WhenUserHasNoAssignedTasks_ShouldReturnEmptyList() {
        String username = "username";

        UserEntity user = UserEntity.builder()
                .id(1L)
                .username(username)
                .build();

        when(serviceHelper.getUserOrThrowException(username)).thenReturn(user);
        when(taskRepository.findByAssignedUser(user)).thenReturn(Collections.emptyList());

        List<TaskDto> assignedTasks = taskService.getAssignedTasks(username);

        assertEquals(0, assignedTasks.size());
    }

    @Test
    void testCreateTask_Success() {
        Long projectId = 1L;
        Long taskStateId = 1L;
        String taskName = "taskName";

        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .build();

        TaskStateEntity taskState = TaskStateEntity.builder()
                .id(taskStateId)
                .build();

        TaskEntity task = TaskEntity.builder()
                .id(1L)
                .name(taskName)
                .taskState(taskState)
                .build();

        TaskDto taskDto = TaskDto.builder()
                .id(1L)
                .name(taskName)
                .build();

        project.getTaskStates().add(taskState);

        when(serviceHelper.getProjectOrThrowException(projectId)).thenReturn(project);
        when(serviceHelper.getTaskStateOrThrowException(taskStateId)).thenReturn(taskState);
        when(taskRepository.save(any(TaskEntity.class))).thenReturn(task);
        when(taskDtoFactory.makeTaskDto(any(TaskEntity.class))).thenReturn(taskDto);

        TaskDto actual = taskService.createTask(projectId, taskStateId, taskName);

        assertEquals(taskDto.getName(), actual.getName());
    }

    @Test
    void testCreateTask_WhenTaskNameIsEmpty_ShouldThrowException() {
        Long projectId = 1L;
        Long taskStateId = 1L;
        String taskName = "   ";

        assertThrows(BadRequestException.class, () -> taskService.createTask(projectId, taskStateId, taskName));
    }

    @Test
    void testCreateTask_WhenProjectNotFound_ShouldThrowException() {
        Long projectId = 1L;
        Long taskStateId = 1L;
        String taskName = "taskName";

        when(serviceHelper.getProjectOrThrowException(projectId)).thenThrow(
                new NotFoundException("Project not found", HttpStatus.NOT_FOUND));

        assertThrows(NotFoundException.class, () -> taskService.createTask(projectId, taskStateId, taskName));
    }

    @Test
    void testCreateTask_WhenTaskStateNotInProject_ShouldThrowException() {
        Long projectId = 1L;
        Long taskStateId = 1L;
        String taskName = "taskName";

        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .build();

        TaskStateEntity taskState = TaskStateEntity.builder()
                .id(taskStateId)
                .build();

        when(serviceHelper.getProjectOrThrowException(projectId)).thenReturn(project);
        when(serviceHelper.getTaskStateOrThrowException(taskStateId)).thenReturn(taskState);

        assertThrows(NotFoundException.class, () -> taskService.createTask(projectId, taskStateId, taskName));
    }

    @Test
    void testCreateTask_WhenTaskAlreadyExists_ShouldThrowException() {
        Long projectId = 1L;
        Long taskStateId = 1L;
        String taskName = "taskName";

        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .build();

        TaskEntity existingTask = TaskEntity.builder()
                .id(1L)
                .name(taskName)
                .build();

        TaskStateEntity taskState = TaskStateEntity.builder()
                .id(taskStateId)
                .tasks(List.of(existingTask))
                .build();

        project.getTaskStates().add(taskState);

        when(serviceHelper.getProjectOrThrowException(projectId)).thenReturn(project);
        when(serviceHelper.getTaskStateOrThrowException(taskStateId)).thenReturn(taskState);

        assertThrows(BadRequestException.class, () -> taskService.createTask(projectId, taskStateId, taskName));
    }
}