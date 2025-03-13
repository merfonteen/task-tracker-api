package by.sirius.task.tracker.core.services;

import by.sirius.task.tracker.api.dto.AckDto;
import by.sirius.task.tracker.api.dto.ProjectDto;
import by.sirius.task.tracker.api.exceptions.BadRequestException;
import by.sirius.task.tracker.api.exceptions.NotFoundException;
import by.sirius.task.tracker.core.factories.ProjectDtoFactory;
import by.sirius.task.tracker.core.services.helpers.ServiceHelper;
import by.sirius.task.tracker.store.entities.ProjectEntity;
import by.sirius.task.tracker.store.entities.ProjectRoleEntity;
import by.sirius.task.tracker.store.entities.RoleEntity;
import by.sirius.task.tracker.store.entities.UserEntity;
import by.sirius.task.tracker.store.repositories.ProjectRepository;
import by.sirius.task.tracker.store.repositories.ProjectRoleRepository;
import by.sirius.task.tracker.store.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class ProjectService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectDtoFactory projectDtoFactory;
    private final ProjectRoleRepository projectRoleRepository;

    private final ServiceHelper serviceHelper;

    public ProjectDto getProjectById(Long projectId, String username) {
        ProjectEntity project = serviceHelper.findProjectByIdOrThrowException(projectId);

        if (!project.getAdmin().getUsername().equals(username)) {
            throw new BadRequestException("You are not authorized to view this project", HttpStatus.BAD_REQUEST);
        }

        return projectDtoFactory.makeProjectDto(project);
    }

    public List<ProjectDto> getProjects(String currentUsername) {
        log.debug("Getting all projects");

        UserEntity currentUser = serviceHelper.findUserByUsernameOrThrowException(currentUsername);
        List<ProjectEntity> userProjects = findUserProjects(currentUser);

        return userProjects.stream()
                .filter(project -> project.getId() != null)
                .sorted(Comparator.comparing(ProjectEntity::getId))
                .map(projectDtoFactory::makeProjectDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectDto createProject(String name, String currentUsername) {
        log.info("Creating project with name: {}", name);

        UserEntity admin = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> (new NotFoundException("User not found", HttpStatus.BAD_REQUEST)));

        if (name.trim().isEmpty()) {
            throw new BadRequestException("Name can't be empty", HttpStatus.BAD_REQUEST);
        }

        projectRepository
                .findByName(name)
                .ifPresent(project -> {
                    throw new BadRequestException(
                            String.format("Project \"%s\" already exists", name), HttpStatus.BAD_REQUEST);
                });

        ProjectEntity project = projectRepository.save(
                ProjectEntity.builder()
                        .name(name)
                        .admin(admin)
                        .build()
        );

        assignProjectAdminRole(admin, project);

        return projectDtoFactory.makeProjectDto(project);
    }

    @Transactional
    public ProjectDto editProject(Long projectId, String newProjectName, String username) {
        log.info("Editing project with ID: {} to new name: {}", projectId, newProjectName);

        UserEntity user = serviceHelper.findUserByUsernameOrThrowException(username);
        ProjectEntity project = serviceHelper.findProjectByIdOrThrowException(projectId);

        if (newProjectName.trim().isEmpty()) {
            throw new BadRequestException("Name can't be empty", HttpStatus.BAD_REQUEST);
        }

        if (!project.getAdmin().getUsername().equals(user.getUsername())) {
            throw new BadRequestException("You are not authorized to edit this project", HttpStatus.BAD_REQUEST);
        }

        projectRepository
                .findByName(newProjectName)
                .filter(anotherProject -> !Objects.equals(anotherProject.getId(), projectId))
                .ifPresent(anotherProject -> {
                    throw new BadRequestException(
                            String.format("Project \"%s\" already exists.", newProjectName), HttpStatus.BAD_REQUEST);
                });

        project.setName(newProjectName);

        ProjectEntity updatedProject = projectRepository.save(project);

        return projectDtoFactory.makeProjectDto(updatedProject);
    }

    @Transactional
    public AckDto deleteProject(Long projectId, String adminName) {
        log.warn("Deleting project with ID: {}", projectId);

        ProjectEntity project = serviceHelper.findProjectByIdOrThrowException(projectId);

        if (!project.getAdmin().getUsername().equals(adminName)) {
            throw new BadRequestException("You are not authorized to remove this project", HttpStatus.BAD_REQUEST);
        }

        projectRepository.delete(project);

        return AckDto.makeDefault(true);
    }

    @Transactional
    public AckDto removeUserFromProject(Long projectId, String usernameToRemove, String adminName) {
        log.warn("Removing user {} from project with ID: {}", usernameToRemove, projectId);

        ProjectEntity project = serviceHelper.findProjectByIdOrThrowException(projectId);
        UserEntity userToDelete = serviceHelper.findUserByUsernameOrThrowException(usernameToRemove);

        if (!project.getAdmin().getUsername().equals(adminName)) {
            throw new BadRequestException("You are not authorized to remove the user from the project", HttpStatus.BAD_REQUEST);
        }

        project.getUsers().remove(userToDelete);
        userToDelete.getMemberProjects().remove(project);

        projectRoleRepository.deleteByUserAndProject(userToDelete, project);

        projectRepository.save(project);
        userRepository.save(userToDelete);

        if (userToDelete.getMemberProjects().isEmpty()) {
            RoleEntity userRole = serviceHelper.getUserRoleOrThrowException();
            userToDelete.getRoles().remove(userRole);
        }

        return AckDto.builder().answer(true).build();
    }

    private void assignProjectAdminRole(UserEntity admin, ProjectEntity project) {
        RoleEntity projectAdminRole = serviceHelper.getAdminRoleOrThrowException();

        ProjectRoleEntity projectRole = ProjectRoleEntity.builder()
                .user(admin)
                .project(project)
                .role(projectAdminRole)
                .build();

        projectRoleRepository.save(projectRole);
    }

    private List<ProjectEntity> findUserProjects(UserEntity user) {
        List<ProjectEntity> allProjects = new ArrayList<>();
        allProjects.addAll(projectRepository.findAllByAdmin(user));
        allProjects.addAll(projectRepository.findAllByUsersContaining(user));
        return allProjects;
    }
}
