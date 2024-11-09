package by.sirius.task.tracker.api.services;

import by.sirius.task.tracker.api.dto.AckDto;
import by.sirius.task.tracker.api.dto.ProjectDto;
import by.sirius.task.tracker.api.exceptions.BadRequestException;
import by.sirius.task.tracker.api.exceptions.NotFoundException;
import by.sirius.task.tracker.api.factories.ProjectDtoFactory;
import by.sirius.task.tracker.api.services.helpers.ServiceHelper;
import by.sirius.task.tracker.api.store.entities.ProjectEntity;
import by.sirius.task.tracker.api.store.entities.ProjectRoleEntity;
import by.sirius.task.tracker.api.store.entities.RoleEntity;
import by.sirius.task.tracker.api.store.entities.UserEntity;
import by.sirius.task.tracker.api.store.repositories.ProjectRepository;
import by.sirius.task.tracker.api.store.repositories.ProjectRoleRepository;
import by.sirius.task.tracker.api.store.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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

    @Cacheable(value = "projects", key = "#currentUsername")
    public List<ProjectDto> getProjects(String currentUsername) {
        log.debug("Getting all projects");

        UserEntity currentUser = serviceHelper.getUserOrThrowException(currentUsername);
        List<ProjectEntity> userProjects = findUserProjects(currentUser);

        return userProjects.stream()
                .filter(project -> project.getId() != null)
                .sorted(Comparator.comparing(ProjectEntity::getId))
                .map(projectDtoFactory::makeProjectDto)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "projects", key = "#projectId")
    public ProjectEntity getProjectById(Long projectId) {
        log.debug("Getting project by id: {}", projectId);
        return serviceHelper.getProjectOrThrowException(projectId);
    }

    @CacheEvict(value = "projects", key = "#currentUsername")
    @Transactional
    public ProjectDto createProject(String name, String currentUsername) {
        log.info("Creating project with name: {}", name);

        if (name.trim().isEmpty()) {
            throw new BadRequestException("Name can't be empty", HttpStatus.BAD_REQUEST);
        }

        projectRepository
                .findByName(name)
                .ifPresent(project -> {
                    throw new BadRequestException(
                            String.format("Project \"%s\" already exists", name), HttpStatus.BAD_REQUEST);
                });

        UserEntity admin = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> (new NotFoundException("User not found", HttpStatus.BAD_REQUEST)));

        ProjectEntity project = projectRepository.save(
                ProjectEntity.builder()
                        .name(name)
                        .admin(admin)
                        .build()
        );

        assignProjectAdminRole(admin, project);

        return projectDtoFactory.makeProjectDto(project);
    }

    @CacheEvict(value = "projects", key = "#projectId")
    @Transactional
    public ProjectDto editProject(Long projectId, String newProjectName) {
        log.info("Editing project with ID: {} to new name: {}", projectId, newProjectName);

        if (newProjectName.trim().isEmpty()) {
            throw new BadRequestException("Name can't be empty", HttpStatus.BAD_REQUEST);
        }

        ProjectEntity project = serviceHelper.getProjectOrThrowException(projectId);

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

    @CacheEvict(value = "projects", key = "#projectId")
    @Transactional
    public AckDto deleteProject(Long projectId) {
        log.warn("Deleting project with ID: {}", projectId);
        serviceHelper.getProjectOrThrowException(projectId);
        projectRepository.deleteById(projectId);
        return AckDto.makeDefault(true);
    }

    @CacheEvict(value = "projects", key = "#projectId")
    @Transactional
    public AckDto removeUserFromProject(Long projectId, String username) {
        log.warn("Removing user {} from project with ID: {}", username, projectId);

        ProjectEntity project = serviceHelper.getProjectOrThrowException(projectId);
        UserEntity userToDelete = serviceHelper.getUserOrThrowException(username);

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

    public boolean isAdmin(UserEntity user, ProjectEntity project) {
        return project.getAdmin().equals(user);
    }
}
