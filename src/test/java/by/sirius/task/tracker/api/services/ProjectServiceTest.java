package by.sirius.task.tracker.api.services;

import by.sirius.task.tracker.api.dto.AckDto;
import by.sirius.task.tracker.api.dto.ProjectDto;
import by.sirius.task.tracker.api.exceptions.BadRequestException;
import by.sirius.task.tracker.api.exceptions.NotFoundException;
import by.sirius.task.tracker.core.factories.ProjectDtoFactory;
import by.sirius.task.tracker.core.services.ProjectService;
import by.sirius.task.tracker.core.services.helpers.ServiceHelper;
import by.sirius.task.tracker.store.entities.ProjectEntity;
import by.sirius.task.tracker.store.entities.ProjectRoleEntity;
import by.sirius.task.tracker.store.entities.RoleEntity;
import by.sirius.task.tracker.store.entities.UserEntity;
import by.sirius.task.tracker.store.repositories.ProjectRepository;
import by.sirius.task.tracker.store.repositories.ProjectRoleRepository;
import by.sirius.task.tracker.store.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectRoleRepository projectRoleRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectDtoFactory projectDtoFactory;

    @Mock
    private ServiceHelper serviceHelper;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void testGetProjects_Success() {
        String username = "testUser";
        UserEntity user = new UserEntity();
        user.setUsername(username);

        ProjectEntity project1 = new ProjectEntity();
        project1.setId(1L);
        project1.setName("Project1");

        ProjectEntity project2 = new ProjectEntity();
        project2.setId(2L);
        project2.setName("Project2");

        ProjectDto projectDto1 = new ProjectDto();
        projectDto1.setId(1L);
        projectDto1.setName("Project1");
        projectDto1.setCreatedAt(Instant.now());

        ProjectDto projectDto2 = new ProjectDto();
        projectDto2.setId(2L);
        projectDto2.setName("Project2");
        projectDto2.setCreatedAt(Instant.now());

        when(serviceHelper.getUserOrThrowException(username)).thenReturn(user);
        when(projectRepository.findAllByAdmin(user)).thenReturn(List.of(project1));
        when(projectRepository.findAllByUsersContaining(user)).thenReturn(List.of(project2));
        when(projectDtoFactory.makeProjectDto(project1)).thenReturn(projectDto1);
        when(projectDtoFactory.makeProjectDto(project2)).thenReturn(projectDto2);

        List<ProjectDto> projects = projectService.getProjects(username);

        assertEquals(2, projects.size());
        assertEquals("Project1", projects.get(0).getName());
        assertEquals("Project2", projects.get(1).getName());
    }

    @Test
    void testGetProjects_WhenNoProjectsFound() {
        String username = "testUser";
        UserEntity user = new UserEntity();
        user.setUsername(username);

        when(serviceHelper.getUserOrThrowException(username)).thenReturn(user);
        when(projectRepository.findAllByAdmin(user)).thenReturn(List.of());
        when(projectRepository.findAllByUsersContaining(user)).thenReturn(List.of());

        List<ProjectDto> projects = projectService.getProjects(username);

        assertTrue(projects.isEmpty());
    }

    @Test
    void testGetProjects_WhenUserNotFound_ShouldThrowException() {
        String username = "nonExistentUsername";

        when(serviceHelper.getUserOrThrowException(username)).thenThrow(
                new NotFoundException("User not found", HttpStatus.NOT_FOUND));

        assertThrows(NotFoundException.class, () -> projectService.getProjects(username));
    }

    @Test
    void testCreateProject_Success() {
        String projectName = "New Project";
        String username = "testUsername";

        UserEntity user = new UserEntity();
        user.setUsername(username);

        ProjectEntity project = ProjectEntity.builder()
                .id(1L)
                .name(projectName)
                .admin(user)
                .build();

        ProjectDto expectedDto = ProjectDto.builder()
                .id(1L)
                .name(projectName)
                .owner(username)
                .createdAt(Instant.now())
                .build();

        RoleEntity role = new RoleEntity();
        role.setName("ROLE_ADMIN");

        ProjectRoleEntity projectRole = ProjectRoleEntity.builder()
                .user(user)
                .project(project)
                .role(role)
                .build();

        when(projectRepository.findByName(projectName)).thenReturn(Optional.empty());
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(projectRepository.save(any(ProjectEntity.class))).thenReturn(project);
        when(projectDtoFactory.makeProjectDto(project)).thenReturn(expectedDto);
        when(serviceHelper.getAdminRoleOrThrowException()).thenReturn(role);
        when(projectRoleRepository.save(any(ProjectRoleEntity.class))).thenReturn(projectRole);

        ProjectDto actualDto = projectService.createProject(projectName, username);

        assertEquals(expectedDto, actualDto);
    }

    @Test
    void testCreateProject_WhenNameIsEmpty_ShouldThrowException() {
        String username = "testUser";
        assertThrows(BadRequestException.class, () -> projectService.createProject("  ", username));
    }

    @Test
    void testCreateProject_WhenNameAlreadyExists_ShouldThrowException() {
        String username = "testUser";
        String projectName = "projectName";

        UserEntity user = new UserEntity();
        user.setUsername(username);

        ProjectEntity existingProject = new ProjectEntity();
        existingProject.setName(projectName);

        when(projectRepository.findByName(projectName)).thenReturn(Optional.of(existingProject));

        assertThrows(BadRequestException.class, () -> projectService.createProject(projectName, username));
    }

    @Test
    void testCreateProject_WhenUserNotFound_ShouldThrowException() {
        String projectName = "projectName";
        String username = "testUser";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> projectService.createProject(projectName, username));
    }

    @Test
    void testEditProject_Success() {
        Long projectId = 1L;
        String newProjectName = "updatedName";

        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .name("originalName")
                .build();

        ProjectDto projectDto = ProjectDto.builder()
                .id(projectId)
                .name(newProjectName)
                .createdAt(Instant.now())
                .build();

        when(serviceHelper.getProjectOrThrowException(projectId)).thenReturn(project);
        when(projectRepository.findByName(newProjectName)).thenReturn(Optional.empty());
        when(projectRepository.save(project)).thenReturn(project);
        when(projectDtoFactory.makeProjectDto(project)).thenReturn(projectDto);

        ProjectDto updatedProjectDto = projectService.editProject(projectId, newProjectName);

        assertEquals(newProjectName, updatedProjectDto.getName());
        verify(projectRepository).save(project);
    }

    @Test
    void testEdiProject_WhenNewProjectNameIsEmpty_ShouldThrowException() {
        Long projectId = 1L;
        String newProjectName = "   ";
        assertThrows(BadRequestException.class, () -> projectService.editProject(projectId, newProjectName));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void testEditProject_WhenNewProjectNameAlreadyExist_ShouldThrowException() {
        Long projectId = 1L;
        String newProjectName = "NewProjectName";

        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .name("originalName")
                .build();

        ProjectEntity existingProject = ProjectEntity.builder()
                .id(2L)
                .name(newProjectName)
                .build();

        when(serviceHelper.getProjectOrThrowException(projectId)).thenReturn(project);
        when(projectRepository.findByName(newProjectName)).thenReturn(Optional.of(existingProject));

        assertThrows(BadRequestException.class, () -> projectService.editProject(projectId, newProjectName));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void testEditProject_WhenProjectNotFound_ShouldThrowException() {
        Long projectId = 1L;
        String newProjectName = "newProjectName";

        when(serviceHelper.getProjectOrThrowException(projectId)).thenThrow(
                new NotFoundException(
                        String.format("Project with \"%d\" id doesn't exist", projectId), HttpStatus.NOT_FOUND)
        );

        assertThrows(NotFoundException.class, () -> projectService.editProject(projectId, newProjectName));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void testDeleteProject_Success() {
        Long projectId = 1L;

        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .build();

        when(serviceHelper.getProjectOrThrowException(projectId)).thenReturn(project);

        AckDto result = projectService.deleteProject(projectId);

        assertNotNull(result);
        assertTrue(result.getAnswer());
        verify(projectRepository).deleteById(projectId);
    }

    @Test
    void testDeleteProject_WhenProjectNotFound_ShouldThrowException() {
        Long projectId = 1L;

        when(serviceHelper.getProjectOrThrowException(projectId)).thenThrow(
                new NotFoundException(
                        String.format("Project with \"%d\" id doesn't exist", projectId), HttpStatus.NOT_FOUND)
        );

        assertThrows(NotFoundException.class, () -> projectService.deleteProject(projectId));
        verify(projectRepository, never()).deleteById(projectId);
    }

    @Test
    void testRemoveUserFromProject_Success() {
        Long projectId = 1L;
        String username = "testUser";

        ProjectEntity project = new ProjectEntity();
        project.setId(projectId);

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setMemberProjects(new ArrayList<>(List.of(project)));

        when(serviceHelper.getProjectOrThrowException(projectId)).thenReturn(project);
        when(serviceHelper.getUserOrThrowException(username)).thenReturn(user);

        AckDto result = projectService.removeUserFromProject(projectId, username);

        assertTrue(result.getAnswer());
        assertFalse(project.getUsers().contains(user));
        assertFalse(user.getMemberProjects().contains(project));
        verify(projectRoleRepository).deleteByUserAndProject(user, project);
        verify(projectRepository).save(project);
        verify(userRepository).save(user);
    }

    @Test
    void testRemoveUserFromProject_WhenProjectNotFound_ShouldThrowException() {
        Long projectId = 1L;
        String username = "testUsername";

        when(serviceHelper.getProjectOrThrowException(projectId)).thenThrow(
                new NotFoundException("Project not found", HttpStatus.NOT_FOUND));

        assertThrows(NotFoundException.class, () -> projectService.removeUserFromProject(projectId, username));
    }

    @Test
    void testRemoveUserFromProject_WhenUserNotFound_ShouldThrowException() {
        Long projectId = 1L;
        String username = "testUsername";

        ProjectEntity project = new ProjectEntity();
        project.setId(projectId);

        when(serviceHelper.getProjectOrThrowException(projectId)).thenReturn(project);
        when(serviceHelper.getUserOrThrowException(username)).thenThrow(
                new NotFoundException("User not found", HttpStatus.NOT_FOUND));

        assertThrows(NotFoundException.class, () -> projectService.removeUserFromProject(projectId, username));
    }

    @Test
    void testRemoveUserFromProject_WhenUserHasNoMoreProjects_ShouldRemoveUserRole() {
        Long projectId = 1L;
        String username = "testUser";

        ProjectEntity project = new ProjectEntity();
        project.setId(projectId);

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setMemberProjects(new ArrayList<>(List.of(project)));

        RoleEntity userRole = new RoleEntity();
        userRole.setName("ROLE_USER");

        when(serviceHelper.getProjectOrThrowException(projectId)).thenReturn(project);
        when(serviceHelper.getUserOrThrowException(username)).thenReturn(user);
        when(serviceHelper.getUserRoleOrThrowException()).thenReturn(userRole);

        projectService.removeUserFromProject(projectId, username);

        assertFalse(user.getRoles().contains(userRole));
        verify(userRepository).save(user);
    }
}