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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    @Mock
    private EmailService emailService;

    @InjectMocks
    private TaskService taskService;

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
    }

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

        when(serviceHelper.findUserByUsernameOrThrowException(username)).thenReturn(user);
        when(taskRepository.findByAssignedUserId(user.getId())).thenReturn(List.of(task));
        when(taskDtoFactory.makeTaskDto(task)).thenReturn(taskDto);

        List<TaskDto> assignedTasks = taskService.getAssignedTasks(username);

        assertEquals(1, assignedTasks.size());
        assertEquals("task1", assignedTasks.get(0).getName());
    }

    @Test
    void testGetAssignedTasks_WhenUserNotFound_ShouldThrowException() {
        String username = "username";

        when(serviceHelper.findUserByUsernameOrThrowException(username)).thenThrow(
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

        when(serviceHelper.findUserByUsernameOrThrowException(username)).thenReturn(user);
        when(taskRepository.findByAssignedUserId(user.getId())).thenReturn(Collections.emptyList());

        List<TaskDto> assignedTasks = taskService.getAssignedTasks(username);

        assertEquals(0, assignedTasks.size());
    }

    @Test
    void testCreateTask_Success() {
        Long projectId = 1L;
        Long taskStateId = 1L;
        String taskName = "Task Name";

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

        when(serviceHelper.findProjectByIdOrThrowException(projectId)).thenReturn(project);
        when(serviceHelper.findTaskStateByIdOrThrowException(taskStateId)).thenReturn(taskState);
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
        String taskName = "Task Name";

        when(serviceHelper.findProjectByIdOrThrowException(projectId)).thenThrow(
                new NotFoundException("Project not found", HttpStatus.NOT_FOUND));

        assertThrows(NotFoundException.class, () -> taskService.createTask(projectId, taskStateId, taskName));
    }

    @Test
    void testCreateTask_WhenTaskStateNotInProject_ShouldThrowException() {
        Long projectId = 1L;
        Long taskStateId = 1L;
        String taskName = "Task Name";

        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .build();

        TaskStateEntity taskState = TaskStateEntity.builder()
                .id(taskStateId)
                .build();

        when(serviceHelper.findProjectByIdOrThrowException(projectId)).thenReturn(project);
        when(serviceHelper.findTaskStateByIdOrThrowException(taskStateId)).thenReturn(taskState);

        assertThrows(NotFoundException.class, () -> taskService.createTask(projectId, taskStateId, taskName));
    }

    @Test
    void testCreateTask_WhenTaskAlreadyExists_ShouldThrowException() {
        Long projectId = 1L;
        Long taskStateId = 1L;
        String taskName = "Task Name";

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

        when(serviceHelper.findProjectByIdOrThrowException(projectId)).thenReturn(project);
        when(serviceHelper.findTaskStateByIdOrThrowException(taskStateId)).thenReturn(taskState);

        assertThrows(BadRequestException.class, () -> taskService.createTask(projectId, taskStateId, taskName));
    }

    @Test
    void testEditTask_Success() {
        Long taskId = 1L;
        String taskName = "New Task Name";
        String username = "testUser";

        UserEntity user = UserEntity.builder()
                .username(username)
                .build();

        TaskEntity task = TaskEntity.builder()
                .id(taskId)
                .name("oldName")
                .assignedUser(user)
                .taskState(
                        TaskStateEntity.builder()
                                .id(1L)
                                .project(ProjectEntity.builder()
                                        .id(1L)
                                        .admin(user)
                                        .build())
                                .build())
                .build();

        TaskDto expected = TaskDto.builder()
                .id(1L)
                .name(taskName)
                .build();

        initSecurityContext();

        when(serviceHelper.findTaskByIdOrThrowException(taskId)).thenReturn(task);
        when(taskRepository.save(any())).thenReturn(task);
        when(taskDtoFactory.makeTaskDto(task)).thenReturn(expected);
        when(taskRepository.findByTaskStateIdAndNameIgnoreCase(task.getTaskState().getId(), taskName))
                .thenReturn(Optional.empty());

        TaskDto actual = taskService.editTask(taskId, taskName);

        assertEquals(expected, actual);
        verify(taskRepository).save(task);
        verify(taskHistoryRepository).save(any(TaskHistoryEntity.class));
    }

    @Test
    void testEditTask_WhenTaskNameIsEmpty_ShouldThrowException() {
        Long taskId = 1L;
        String newTaskName = "   ";

        initSecurityContext();

        assertThrows(BadRequestException.class, () -> taskService.editTask(taskId, newTaskName));
        verifyNoInteractions(taskRepository);
    }

    @Test
    void testEditTask_WhenTaskNotFound_ShouldThrowException() {
        Long taskId = 1L;
        String newTaskName = "New Task Name";

        initSecurityContext();

        when(serviceHelper.findTaskByIdOrThrowException(taskId)).thenThrow(
                new NotFoundException("Task not found", HttpStatus.NOT_FOUND));

        assertThrows(NotFoundException.class, () -> taskService.editTask(taskId, newTaskName));
        verifyNoInteractions(taskRepository);
    }

    @Test
    void testEditTask_WhenTaskNameAlreadyExists_ShouldThrowException() {
        Long taskId = 1L;
        String newTaskName = "New Task Name";
        String username = "testUsername";

        initSecurityContext();

        UserEntity user = UserEntity.builder()
                .username(username)
                .build();

        TaskEntity task = TaskEntity.builder()
                .id(taskId)
                .name("oldName")
                .assignedUser(user)
                .taskState(
                        TaskStateEntity.builder()
                                .id(1L)
                                .project(ProjectEntity.builder()
                                        .id(1L)
                                        .admin(user)
                                        .build())
                                .build()
                )
                .build();

        when(serviceHelper.findTaskByIdOrThrowException(taskId)).thenReturn(task);

        assertThrows(BadRequestException.class, () -> taskService.editTask(taskId, newTaskName));
        verify(taskRepository, never()).save(any(TaskEntity.class));
    }

    @Test
    void testDeleteTask_Success() {
        Long taskId = 1L;
        String username = "testUser";

        UserEntity user = UserEntity.builder()
                .id(1L)
                .username(username)
                .build();

        TaskEntity taskToDelete = TaskEntity.builder()
                .id(taskId)
                .name("Task Name")
                .assignedUser(user)
                .build();

        TaskStateEntity taskState = TaskStateEntity.builder()
                .id(1L)
                .tasks(new ArrayList<>(List.of(taskToDelete)))
                .project(ProjectEntity.builder()
                        .id(1L)
                        .admin(user)
                        .build())
                .build();

        taskToDelete.setTaskState(taskState);

        initSecurityContext();
        when(serviceHelper.findTaskByIdOrThrowException(taskId)).thenReturn(taskToDelete);

        AckDto result = taskService.deleteTask(taskId);

        assertNotNull(result);
        assertTrue(result.getAnswer());

        verify(taskRepository).delete(taskToDelete);
        verify(serviceHelper).replaceOldTaskPosition(taskToDelete);
        verify(taskHistoryRepository).save(any(TaskHistoryEntity.class));
        verify(taskStateRepository).save(taskState);
    }

    @Test
    void testDeleteTask_WhenTaskNotFound_ShouldThrowException() {
        Long taskId = 1L;

        initSecurityContext();

        when(serviceHelper.findTaskByIdOrThrowException(taskId)).thenThrow(
                new NotFoundException("Task not found", HttpStatus.NOT_FOUND));

        assertThrows(NotFoundException.class, () -> taskService.deleteTask(taskId));
        verifyNoInteractions(taskRepository, taskStateRepository, taskHistoryRepository);
    }

    @Test
    void testChangeTaskPosition_Success() {
        Long taskId = 1L;
        Long leftTaskId = 2L;

        TaskEntity taskToMove = TaskEntity.builder()
                .id(taskId)
                .name("TaskToMove")
                .build();

        TaskEntity leftTask = TaskEntity.builder()
                .id(leftTaskId)
                .name("LeftTask")
                .build();

        TaskStateEntity taskState = TaskStateEntity.builder()
                .id(1L)
                .build();

        taskToMove.setTaskState(taskState);
        leftTask.setTaskState(taskState);

        initSecurityContext();

        when(serviceHelper.findTaskByIdOrThrowException(taskId)).thenReturn(taskToMove);
        when(serviceHelper.findTaskByIdOrThrowException(leftTaskId)).thenReturn(leftTask);
        when(taskRepository.save(taskToMove)).thenReturn(taskToMove);
        when(taskDtoFactory.makeTaskDto(taskToMove)).thenReturn(
                TaskDto.builder()
                        .id(taskId)
                        .name("TaskToMove")
                        .build()
        );

        TaskDto result = taskService.changeTaskPosition(taskId, Optional.of(leftTaskId));

        assertNotNull(result);
        assertEquals("TaskToMove", result.getName());

        verify(serviceHelper).replaceOldTaskPosition(taskToMove);
        verify(taskRepository).save(taskToMove);
        verify(taskHistoryRepository).save(any(TaskHistoryEntity.class));
    }

    @Test
    void testChangeTaskPosition_SamePosition_NoChanges() {
        Long taskId = 1L;
        Long leftTaskId = 2L;

        TaskEntity taskToMove = TaskEntity.builder()
                .id(taskId)
                .name("TaskToMove")
                .build();

        TaskEntity leftTask = TaskEntity.builder()
                .id(leftTaskId)
                .name("LeftTask")
                .build();

        TaskDto expectedDto = TaskDto.builder()
                .id(taskId)
                .name("TaskToMove")
                .build();

        taskToMove.setLeftTask(leftTask);

        initSecurityContext();
        when(serviceHelper.findTaskByIdOrThrowException(taskId)).thenReturn(taskToMove);
        when(taskDtoFactory.makeTaskDto(taskToMove)).thenReturn(expectedDto);

        TaskDto result = taskService.changeTaskPosition(taskId, Optional.of(leftTaskId));

        assertEquals(expectedDto, result);

        verifyNoInteractions(taskRepository, taskHistoryRepository);
    }

    @Test
    void testChangeTaskPosition_SameTaskIdAsLeftTask_ShouldThrowException() {
        Long taskId = 1L;

        initSecurityContext();
        when(serviceHelper.findTaskByIdOrThrowException(taskId)).thenReturn(TaskEntity.builder().id(taskId).build());

        assertThrows(BadRequestException.class, () -> taskService.changeTaskPosition(taskId, Optional.of(taskId)));

        verifyNoInteractions(taskRepository, taskHistoryRepository);
    }

    @Test
    void testChangeTaskPosition_DifferentTaskState_ShouldThrowException() {
        Long taskId = 1L;
        Long leftTaskId = 2L;

        TaskEntity taskToMove = TaskEntity.builder()
                .id(taskId)
                .build();

        TaskEntity leftTask = TaskEntity.builder()
                .id(leftTaskId)
                .build();

        TaskStateEntity taskState1 = TaskStateEntity.builder().id(1L).build();
        TaskStateEntity taskState2 = TaskStateEntity.builder().id(2L).build();

        taskToMove.setTaskState(taskState1);
        leftTask.setTaskState(taskState2);

        initSecurityContext();
        when(serviceHelper.findTaskByIdOrThrowException(taskId)).thenReturn(taskToMove);
        when(serviceHelper.findTaskByIdOrThrowException(leftTaskId)).thenReturn(leftTask);

        assertThrows(BadRequestException.class, () -> taskService.changeTaskPosition(taskId, Optional.of(leftTaskId)));

        verifyNoInteractions(taskRepository, taskHistoryRepository);
    }

    @Test
    void testChangeTaskPosition_TaskNotFound_ShouldThrowException() {
        Long taskId = 1L;

        initSecurityContext();
        when(serviceHelper.findTaskByIdOrThrowException(taskId)).thenThrow(
                new NotFoundException("Task not found", HttpStatus.NOT_FOUND));

        assertThrows(NotFoundException.class, () -> taskService.changeTaskPosition(taskId, Optional.empty()));

        verifyNoInteractions(taskRepository, taskHistoryRepository);
    }

    @Test
    void testChangeTaskState_Success() {
        Long taskId = 1L;
        Long newTaskStateId = 2L;

        TaskStateEntity currentTaskState = TaskStateEntity.builder()
                .id(1L)
                .name("To Do")
                .build();

        TaskStateEntity newTaskState = TaskStateEntity.builder()
                .id(newTaskStateId)
                .name("In Progress")
                .tasks(new ArrayList<>())
                .build();

        TaskEntity taskToMove = TaskEntity.builder()
                .id(taskId)
                .name("Test Task")
                .taskState(currentTaskState)
                .build();

        currentTaskState.getTasks().add(taskToMove);

        TaskDto expectedDto = TaskDto.builder()
                .id(taskId)
                .name("Test Task")
                .build();

        initSecurityContext();
        when(serviceHelper.findTaskByIdOrThrowException(taskId)).thenReturn(taskToMove);
        when(serviceHelper.findTaskStateByIdOrThrowException(newTaskStateId)).thenReturn(newTaskState);
        when(taskRepository.save(taskToMove)).thenReturn(taskToMove);
        when(taskDtoFactory.makeTaskDto(taskToMove)).thenReturn(expectedDto);

        TaskDto actualDto = taskService.changeTaskState(taskId, newTaskStateId);

        assertEquals(expectedDto, actualDto);
        assertTrue(newTaskState.getTasks().contains(taskToMove));
        assertFalse(currentTaskState.getTasks().contains(taskToMove));

        verify(taskHistoryRepository).save(any(TaskHistoryEntity.class));
        verify(taskRepository).save(any(TaskEntity.class));
        verify(taskStateRepository).save(currentTaskState);
        verify(taskStateRepository).save(newTaskState);
    }

    @Test
    void testChangeTaskState_TaskWithSameNameExists_ShouldThrowException() {
        Long taskId = 1L;
        Long newTaskStateId = 2L;

        TaskStateEntity newTaskState = TaskStateEntity.builder()
                .id(newTaskStateId)
                .name("In Progress")
                .tasks(new ArrayList<>())
                .build();

        TaskEntity existingTask = TaskEntity.builder()
                .id(2L)
                .name("Test Task")
                .build();

        newTaskState.getTasks().add(existingTask);

        TaskEntity taskToMove = TaskEntity.builder()
                .id(taskId)
                .name("Test Task")
                .build();

        initSecurityContext();
        when(serviceHelper.findTaskByIdOrThrowException(taskId)).thenReturn(taskToMove);
        when(serviceHelper.findTaskStateByIdOrThrowException(newTaskStateId)).thenReturn(newTaskState);

        assertThrows(BadRequestException.class, () -> taskService.changeTaskState(taskId, newTaskStateId));
    }

    @Test
    void testChangeTaskState_TaskNotFound_ShouldThrowException() {
        Long taskId = 1L;
        Long newTaskStateId = 2L;

        initSecurityContext();
        when(serviceHelper.findTaskByIdOrThrowException(taskId)).thenThrow(
                new NotFoundException("Task not found", HttpStatus.NOT_FOUND));

        assertThrows(NotFoundException.class, () -> taskService.changeTaskState(taskId, newTaskStateId));
    }

    @Test
    void testChangeTaskState_TaskStateNotFound_ShouldThrowException() {
        Long taskId = 1L;
        Long newTaskStateId = 2L;

        TaskEntity taskToMove = TaskEntity.builder()
                .id(taskId)
                .name("Test Task")
                .build();

        initSecurityContext();
        when(serviceHelper.findTaskByIdOrThrowException(taskId)).thenReturn(taskToMove);
        when(serviceHelper.findTaskStateByIdOrThrowException(newTaskStateId)).thenThrow(
                new NotFoundException("Task state not found", HttpStatus.NOT_FOUND));

        assertThrows(NotFoundException.class, () -> taskService.changeTaskState(taskId, newTaskStateId));
    }

    @Test
    void testChangeTaskState_MoveToEmptyState() {
        Long taskId = 1L;
        Long newTaskStateId = 2L;

        TaskStateEntity currentTaskState = TaskStateEntity.builder()
                .id(1L)
                .name("To Do")
                .build();

        TaskStateEntity newTaskState = TaskStateEntity.builder()
                .id(newTaskStateId)
                .name("In Progress")
                .tasks(new ArrayList<>())
                .build();

        TaskEntity taskToMove = TaskEntity.builder()
                .id(taskId)
                .name("Test Task")
                .taskState(currentTaskState)
                .build();

        currentTaskState.getTasks().add(taskToMove);

        TaskDto expectedDto = TaskDto.builder()
                .id(taskId)
                .name("Test Task")
                .build();

        initSecurityContext();
        when(serviceHelper.findTaskByIdOrThrowException(taskId)).thenReturn(taskToMove);
        when(serviceHelper.findTaskStateByIdOrThrowException(newTaskStateId)).thenReturn(newTaskState);
        when(taskRepository.save(taskToMove)).thenReturn(taskToMove);
        when(taskDtoFactory.makeTaskDto(taskToMove)).thenReturn(expectedDto);

        TaskDto actualDto = taskService.changeTaskState(taskId, newTaskStateId);

        assertEquals(expectedDto, actualDto);
        assertTrue(newTaskState.getTasks().contains(taskToMove));
        assertFalse(currentTaskState.getTasks().contains(taskToMove));

        verify(taskHistoryRepository).save(any(TaskHistoryEntity.class));
        verify(taskRepository).save(taskToMove);
        verify(taskStateRepository).save(currentTaskState);
        verify(taskStateRepository).save(newTaskState);
    }

    @Test
    void testAssignTaskToUser_Success() {
        Long taskId = 1L;
        String username = "newUser";
        String currentUsername = "currentAdmin";

        TaskStateEntity taskState = TaskStateEntity.builder()
                .id(1L)
                .name("To Do")
                .build();

        UserEntity assignedUser = UserEntity.builder()
                .username(username)
                .email("newuser@example.com")
                .build();

        UserEntity currentUser = UserEntity.builder()
                .username(currentUsername)
                .build();

        ProjectEntity project = ProjectEntity.builder()
                .id(1L)
                .name("Test Project")
                .users(new ArrayList<>(List.of(assignedUser)))
                .build();

        TaskEntity task = TaskEntity.builder()
                .id(taskId)
                .name("Test Task")
                .taskState(taskState)
                .assignedUser(currentUser)
                .build();

        taskState.setProject(project);

        TaskDto expectedDto = TaskDto.builder()
                .id(taskId)
                .name("Test Task")
                .build();

        initSecurityContext();
        when(serviceHelper.findTaskByIdOrThrowException(taskId)).thenReturn(task);
        when(serviceHelper.findUserByUsernameOrThrowException(username)).thenReturn(assignedUser);
        when(taskRepository.save(task)).thenReturn(task);
        when(taskDtoFactory.makeTaskDto(task)).thenReturn(expectedDto);

        TaskDto actualDto = taskService.assignTaskToUser(taskId, username);

        assertEquals(expectedDto, actualDto);
        assertEquals(assignedUser, task.getAssignedUser());
        verify(taskHistoryRepository).save(any(TaskHistoryEntity.class));
        verify(emailService).sendEmail(eq("newuser@example.com"), anyString(), anyString());
    }

    @Test
    void testAssignTaskToUser_TaskNotFound_ShouldThrowException() {
        Long taskId = 1L;
        String username = "newUser";

        initSecurityContext();
        when(serviceHelper.findTaskByIdOrThrowException(taskId)).thenThrow(
                new NotFoundException("Task not found", HttpStatus.NOT_FOUND));

        assertThrows(NotFoundException.class, () -> taskService.assignTaskToUser(taskId, username));
    }

    @Test
    void testAssignTaskToUser_UserNotFound_ShouldThrowException() {
        Long taskId = 1L;
        String username = "newUser";

        TaskEntity task = TaskEntity.builder()
                .id(taskId)
                .build();

        TaskStateEntity taskState = TaskStateEntity.builder()
                .id(1L)
                .build();

        task.setTaskState(taskState);

        initSecurityContext();
        when(serviceHelper.findTaskByIdOrThrowException(taskId)).thenReturn(task);
        when(serviceHelper.findUserByUsernameOrThrowException(username)).thenThrow(
                new NotFoundException("User not found", HttpStatus.NOT_FOUND));

        assertThrows(NotFoundException.class, () -> taskService.assignTaskToUser(taskId, username));
    }

    @Test
    void testAssignTaskToUser_UserNotInProject_ShouldThrowException() {
        Long taskId = 1L;
        String username = "newUser";

        TaskStateEntity taskState = TaskStateEntity.builder()
                .id(1L)
                .build();

        ProjectEntity project = ProjectEntity.builder()
                .id(1L)
                .users(new ArrayList<>())
                .build();

        UserEntity user = UserEntity.builder()
                .username(username)
                .build();

        TaskEntity task = TaskEntity.builder()
                .id(taskId)
                .taskState(taskState)
                .build();

        taskState.setProject(project);

        initSecurityContext();
        when(serviceHelper.findTaskByIdOrThrowException(taskId)).thenReturn(task);
        when(serviceHelper.findUserByUsernameOrThrowException(username)).thenReturn(user);

        assertThrows(BadRequestException.class, () -> taskService.assignTaskToUser(taskId, username));
    }

    @Test
    void testAssignTaskToUser_TaskAlreadyAssignedToUser_ShouldNotChange() {
        Long taskId = 1L;
        String username = "currentUser";

        TaskStateEntity taskState = TaskStateEntity.builder()
                .id(1L)
                .build();

        UserEntity user = UserEntity.builder()
                .username(username)
                .build();

        ProjectEntity project = ProjectEntity.builder()
                .id(1L)
                .users(new ArrayList<>(List.of(user)))
                .build();

        TaskEntity task = TaskEntity.builder()
                .id(taskId)
                .taskState(taskState)
                .assignedUser(user)
                .build();

        taskState.setProject(project);

        initSecurityContext();
        when(serviceHelper.findTaskByIdOrThrowException(taskId)).thenReturn(task);
        when(serviceHelper.findUserByUsernameOrThrowException(username)).thenReturn(user);
        when(taskDtoFactory.makeTaskDto(task)).thenReturn(new TaskDto());

        taskService.assignTaskToUser(taskId, username);

        assertEquals(username, task.getAssignedUser().getUsername());
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    private static void initSecurityContext() {
        String currentUsername = "testUser";

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        when(authentication.getName()).thenReturn(currentUsername);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(SecurityContextHolder.getContext().getAuthentication().getName()).thenReturn(currentUsername);
    }
}