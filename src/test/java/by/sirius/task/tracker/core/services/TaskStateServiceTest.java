package by.sirius.task.tracker.core.services;

import by.sirius.task.tracker.api.dto.AckDto;
import by.sirius.task.tracker.api.dto.TaskStateDto;
import by.sirius.task.tracker.api.exceptions.BadRequestException;
import by.sirius.task.tracker.api.exceptions.NotFoundException;
import by.sirius.task.tracker.core.factories.TaskStateDtoFactory;
import by.sirius.task.tracker.core.services.helpers.ServiceHelper;
import by.sirius.task.tracker.store.entities.ProjectEntity;
import by.sirius.task.tracker.store.entities.TaskStateEntity;
import by.sirius.task.tracker.store.repositories.TaskStateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskStateServiceTest {

    @Mock
    private ServiceHelper serviceHelper;

    @Mock
    private TaskStateRepository taskStateRepository;

    @Mock
    private TaskStateDtoFactory taskStateDtoFactory;

    @InjectMocks
    private TaskStateService taskStateService;

    @Test
    void testGetTaskStates_Success() {
        Long projectId = 1L;

        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .build();

        TaskStateEntity taskState = TaskStateEntity.builder()
                .id(1L)
                .name("taskStateName")
                .build();

        TaskStateDto taskStateDto = TaskStateDto.builder()
                .id(1L)
                .name("taskStateName")
                .build();

        project.setTaskStates(List.of(taskState));

        when(serviceHelper.getProjectOrThrowException(projectId)).thenReturn(project);
        when(taskStateDtoFactory.makeTaskStateDto(taskState)).thenReturn(taskStateDto);

        List<TaskStateDto> taskStates = taskStateService.getTaskStates(projectId);

        assertEquals(1, taskStates.size());
        assertEquals("taskStateName", taskStates.get(0).getName());
    }

    @Test
    void testGetTaskStates_WhenProjectHasNoTaskStates_ShouldReturnEmptyList() {
        Long projectId = 1L;

        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .build();

        project.setTaskStates(List.of());

        when(serviceHelper.getProjectOrThrowException(projectId)).thenReturn(project);

        List<TaskStateDto> taskStates = taskStateService.getTaskStates(projectId);

        assertTrue(taskStates.isEmpty());
    }

    @Test
    void testGetTaskStates_WhenProjectNotFound_ShouldThrowException() {
        Long projectId = 1L;

        when(serviceHelper.getProjectOrThrowException(projectId)).thenThrow(
                new NotFoundException("Project not found", HttpStatus.NOT_FOUND));

        assertThrows(NotFoundException.class, () -> taskStateService.getTaskStates(projectId));
    }

    @Test
    void testCreateTaskState_Success() {
        Long projectId = 1L;
        String taskStateName = "taskStateName";

        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .build();

        TaskStateEntity taskState = TaskStateEntity.builder()
                .id(1L)
                .name(taskStateName)
                .build();

        TaskStateDto expected = TaskStateDto.builder()
                .id(1L)
                .name(taskStateName)
                .build();

        when(serviceHelper.getProjectOrThrowException(projectId)).thenReturn(project);
        when(taskStateDtoFactory.makeTaskStateDto(taskState)).thenReturn(expected);
        when(taskStateRepository.save(any(TaskStateEntity.class))).thenReturn(taskState);

        TaskStateDto actual = taskStateService.createTaskState(projectId, taskStateName);

        assertEquals(expected, actual);
        verify(taskStateRepository).save(taskState);
    }

    @Test
    void testCreateTaskState_WhenTaskStateNameIsEmpty_ShouldThrowException() {
        Long projectId = 1L;
        String taskStateName = "   ";

        assertThrows(BadRequestException.class, () -> taskStateService.createTaskState(projectId, taskStateName));
    }

    @Test
    void testCreateTaskState_WhenProjectNotFound_ShouldThrowException() {
        Long projectId = 1L;
        String taskStateName = "taskStateName";

        when(serviceHelper.getProjectOrThrowException(projectId)).thenThrow(
                new NotFoundException("Project not found", HttpStatus.NOT_FOUND));

        assertThrows(NotFoundException.class, () -> taskStateService.createTaskState(projectId, taskStateName));
    }

    @Test
    void testCreateTaskState_WhenTaskStateAlreadyExists_ShouldThrowException() {
        Long projectId = 1L;
        String taskStateName = "repeatingName";

        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .taskStates(new ArrayList<>())
                .build();

        TaskStateEntity existingTaskState = TaskStateEntity.builder()
                .id(1L)
                .name(taskStateName)
                .build();

        project.getTaskStates().add(existingTaskState);

        when(serviceHelper.getProjectOrThrowException(projectId)).thenReturn(project);

        assertThrows(BadRequestException.class, () -> taskStateService.createTaskState(projectId, taskStateName));
    }

    @Test
    void testEditTaskState_Success() {
        Long taskStateId = 1L;
        String newTaskStateName = "newName";

        ProjectEntity project = ProjectEntity.builder()
                .id(1L)
                .taskStates(new ArrayList<>())
                .build();

        TaskStateEntity taskState = TaskStateEntity.builder()
                .id(taskStateId)
                .name("oldName")
                .build();

        project.getTaskStates().add(taskState);
        taskState.setProject(project);

        TaskStateDto expected = TaskStateDto.builder()
                .id(taskStateId)
                .name(newTaskStateName)
                .build();

        when(serviceHelper.getTaskStateOrThrowException(taskStateId)).thenReturn(taskState);
        when(taskStateDtoFactory.makeTaskStateDto(taskState)).thenReturn(expected);
        when(taskStateRepository.findTaskStateEntityByProjectIdAndNameContainsIgnoreCase(
                taskState.getProject().getId(), newTaskStateName)).thenReturn(Optional.empty()
        );
        when(taskStateRepository.save(any(TaskStateEntity.class))).thenReturn(taskState);

        TaskStateDto actual = taskStateService.editTaskState(taskStateId, newTaskStateName);

        assertEquals(expected, actual);
        verify(taskStateRepository).save(taskState);
    }

    @Test
    void testEditTaskState_WhenNewTaskStateNameIsEmpty_ShouldThrowException() {
        Long taskStateId = 1L;
        String newTaskStateName = "  ";

        assertThrows(BadRequestException.class, () -> taskStateService.editTaskState(taskStateId, newTaskStateName));
    }

    @Test
    void testEditTaskState_WhenTaskStateNotFound_ShouldThrowException() {
        Long taskStateId = 1L;
        String newTaskStateName = "newName";

        when(serviceHelper.getTaskStateOrThrowException(taskStateId)).thenThrow(
                new NotFoundException("Task state not found", HttpStatus.NOT_FOUND)
        );

        assertThrows(NotFoundException.class, () -> taskStateService.editTaskState(taskStateId, newTaskStateName));
    }

    @Test
    void testEditTaskState_WhenTaskStateNameAlreadyExists_ShouldThrowException() {
        Long taskStateId = 1L;
        String name = "existingName";

        ProjectEntity project = ProjectEntity.builder()
                .id(1L)
                .taskStates(new ArrayList<>())
                .build();

        TaskStateEntity taskStateToUpdate = TaskStateEntity.builder()
                .id(taskStateId)
                .name(name)
                .build();

        TaskStateEntity existingTaskState = TaskStateEntity.builder()
                .id(2L)
                .name(name)
                .build();

        project.getTaskStates().add(taskStateToUpdate);
        taskStateToUpdate.setProject(project);

        when(serviceHelper.getTaskStateOrThrowException(taskStateId)).thenReturn(taskStateToUpdate);
        when(taskStateRepository.findTaskStateEntityByProjectIdAndNameContainsIgnoreCase(
                taskStateToUpdate.getProject().getId(), name)).thenReturn(Optional.of(existingTaskState)
        );

        assertThrows(BadRequestException.class, () -> taskStateService.editTaskState(taskStateId, name));
    }

    @Test
    void testDeleteTaskState_Success() {
        Long taskStateId = 1L;

        TaskStateEntity taskState = TaskStateEntity.builder()
                .id(taskStateId)
                .build();

        when(serviceHelper.getTaskStateOrThrowException(taskStateId)).thenReturn(taskState);

        AckDto result = taskStateService.deleteTaskState(taskStateId);

        assertNotNull(result);
        assertTrue(result.getAnswer());
        verify(taskStateRepository).deleteById(taskStateId);
    }

    @Test
    void testDeleteTaskState_WhenTaskStateNotFound_ShouldThrowException() {
        Long taskStateId = 1L;

        when(serviceHelper.getTaskStateOrThrowException(taskStateId)).thenThrow(
                new NotFoundException("Task state not found", HttpStatus.NOT_FOUND)
        );
    }

    @Test
    void testChangeTaskStatePosition_Success() {
        Long projectId = 1L;
        Long taskStateId = 1L;
        Long leftTaskStateId = 2L;

        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .build();

        TaskStateEntity currentTaskState = TaskStateEntity.builder()
                .id(taskStateId)
                .project(project)
                .build();

        TaskStateEntity leftTaskState = TaskStateEntity.builder()
                .id(leftTaskStateId)
                .project(project)
                .build();

        TaskStateDto expectedDto = new TaskStateDto();

        when(serviceHelper.getTaskStateOrThrowException(taskStateId)).thenReturn(currentTaskState);
        when(serviceHelper.getTaskStateOrThrowException(leftTaskStateId)).thenReturn(leftTaskState);
        when(taskStateDtoFactory.makeTaskStateDto(currentTaskState)).thenReturn(expectedDto);
        when(taskStateRepository.save(currentTaskState)).thenReturn(currentTaskState);

        TaskStateDto actualDto = taskStateService.changeTaskStatePosition(taskStateId, Optional.of(leftTaskStateId));

        assertEquals(expectedDto, actualDto);
        verify(taskStateRepository).save(currentTaskState);
        verify(taskStateRepository).save(leftTaskState);
    }

    @Test
    void testChangeTaskStatePosition_WhenTaskStateNotFound_ShouldThrowException() {
        Long taskStateId = 1L;
        Long leftTaskStateId = 2L;

        when(serviceHelper.getTaskStateOrThrowException(taskStateId)).thenThrow(
                new NotFoundException("Task state not found", HttpStatus.NOT_FOUND));

        assertThrows(NotFoundException.class,
                () -> taskStateService.changeTaskStatePosition(taskStateId, Optional.of(leftTaskStateId)));
    }

    @Test
    void testChangeTaskStatePosition_WhenLeftTaskStateIdEqualsTaskStateId_ShouldThrowException() {
        Long taskStateId = 1L;

        TaskStateEntity taskState = TaskStateEntity.builder()
                .id(taskStateId)
                .build();

        ProjectEntity project = ProjectEntity.builder()
                .id(1L)
                .taskStates(List.of(taskState))
                .build();

        when(serviceHelper.getTaskStateOrThrowException(taskStateId)).thenReturn(taskState);

        assertThrows(BadRequestException.class,
                () -> taskStateService.changeTaskStatePosition(taskStateId, Optional.of(taskStateId)));
    }

    @Test
    void testChangeTaskStatePosition_WhenLeftTaskStateInDifferentProject_ShouldThrowException() {
        Long taskStateId = 1L;
        Long leftTaskStateId = 2L;

        ProjectEntity project1 = ProjectEntity.builder()
                .id(1L)
                .build();

        ProjectEntity project2 = ProjectEntity.builder()
                .id(2L)
                .build();

        TaskStateEntity currentTaskState = TaskStateEntity.builder()
                .id(taskStateId)
                .project(project1)
                .build();

        TaskStateEntity leftTaskState = TaskStateEntity.builder()
                .id(leftTaskStateId)
                .project((project2))
                .build();

        when(serviceHelper.getTaskStateOrThrowException(taskStateId)).thenReturn(currentTaskState);
        when(serviceHelper.getTaskStateOrThrowException(leftTaskStateId)).thenReturn(leftTaskState);

        assertThrows(BadRequestException.class,
                () -> taskStateService.changeTaskStatePosition(taskStateId, Optional.of(leftTaskStateId)));
    }
}