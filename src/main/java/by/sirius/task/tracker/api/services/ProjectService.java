package by.sirius.task.tracker.api.services;

import by.sirius.task.tracker.api.dto.AckDto;
import by.sirius.task.tracker.api.dto.ProjectDto;
import by.sirius.task.tracker.api.exceptions.BadRequestException;
import by.sirius.task.tracker.api.exceptions.NotFoundException;
import by.sirius.task.tracker.api.factories.ProjectDtoFactory;
import by.sirius.task.tracker.api.services.helpers.ServiceHelper;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Slf4j
@Service
public class ProjectService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectDtoFactory projectDtoFactory;
    private final ProjectRoleRepository projectRoleRepository;

    private final ServiceHelper serviceHelper;

    public List<ProjectDto> fetchProjects(Optional<String> optionalPrefixName) {
        log.info("Fetching projects with prefix: {}", optionalPrefixName.orElse("none"));

        optionalPrefixName = optionalPrefixName.filter(prefixName -> !prefixName.trim().isEmpty());

        Stream<ProjectEntity> projectStream = optionalPrefixName
                .map(projectRepository::streamAllByNameStartsWithIgnoreCase)
                .orElseGet(projectRepository::streamAllBy);

        return projectStream
                .map(projectDtoFactory::makeProjectDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectDto createProject(String name) {
        log.info("Creating project with name: {}", name);

        if (name.trim().isEmpty()) {
            throw new BadRequestException("Name can't be empty", HttpStatus.BAD_REQUEST);
        }

        projectRepository
                .findByName(name)
                .ifPresent(project -> {
                    throw new BadRequestException(String.format("Project \"%s\" already exists.", name), HttpStatus.BAD_REQUEST);
                });

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        UserEntity admin = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> (new NotFoundException("User not found", HttpStatus.BAD_REQUEST)));

        ProjectEntity project = projectRepository.saveAndFlush(
                ProjectEntity.builder()
                        .name(name)
                        .admin(admin)
                        .build()
        );

        assignProjectAdminRole(admin, project);

        return projectDtoFactory.makeProjectDto(project);
    }

    @Transactional
    public ProjectDto editProject(Long projectId, String name) {
        log.info("Editing project with ID: {} to new name: {}", projectId, name);

        if (name.trim().isEmpty()) {
            throw new BadRequestException("Name can't be empty", HttpStatus.BAD_REQUEST);
        }

        ProjectEntity project = serviceHelper.getProjectOrThrowException(projectId);

        projectRepository
                .findByName(name)
                .filter(anotherProject -> !Objects.equals(anotherProject.getId(), projectId))
                .ifPresent(anotherProject -> {
                    throw new BadRequestException(String.format("Project \"%s\" already exists.", name), HttpStatus.BAD_REQUEST);
                });

        project.setName(name);
        project = projectRepository.saveAndFlush(project);

        return projectDtoFactory.makeProjectDto(project);
    }

    @Transactional
    public AckDto deleteProject(Long projectId) {
        log.warn("Deleting project with ID: {}", projectId);
        serviceHelper.getProjectOrThrowException(projectId);
        projectRepository.deleteById(projectId);
        return AckDto.makeDefault(true);
    }

    @Transactional
    public AckDto removeUserFromProject(Long projectId, String username) {
        log.warn("Removing user {} from project with ID: {}", username, projectId);

        ProjectEntity project = serviceHelper.getProjectOrThrowException(projectId);
        UserEntity userToDelete = serviceHelper.getUserOrThrowException(username);

        project.getUsers().remove(userToDelete);
        userToDelete.getMemberProjects().remove(project);

        projectRepository.save(project);
        userRepository.save(userToDelete);

        if (userToDelete.getMemberProjects().isEmpty()) {
            RoleEntity userRole = serviceHelper.getUserRoleOrThrowException();
            userToDelete.getRoles().remove(userRole);
        }

        return AckDto.builder().answer(true).build();
    }

    public ProjectEntity getProjectById(Long projectId) {
        return serviceHelper.getProjectOrThrowException(projectId);
    }

    public boolean isAdmin(UserEntity user, ProjectEntity project) {
        return project.getAdmin().equals(user);
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
}
