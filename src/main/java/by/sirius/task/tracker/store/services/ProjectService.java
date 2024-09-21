package by.sirius.task.tracker.store.services;

import by.sirius.task.tracker.api.dto.AckDto;
import by.sirius.task.tracker.api.dto.ProjectDto;
import by.sirius.task.tracker.api.exceptions.BadRequestException;
import by.sirius.task.tracker.api.factories.ProjectDtoFactory;
import by.sirius.task.tracker.store.entities.ProjectEntity;
import by.sirius.task.tracker.store.repositories.ProjectRepository;
import by.sirius.task.tracker.store.services.helpers.ServiceHelper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Transactional
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectDtoFactory projectDtoFactory;

    private final ServiceHelper serviceHelper;

    public List<ProjectDto> fetchProjects(Optional<String> optionalPrefixName) {

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

        if (name.trim().isEmpty()) {
            throw new BadRequestException("Name can't be empty", HttpStatus.BAD_REQUEST);
        }

        projectRepository
                .findByName(name)
                .ifPresent(project -> {
                    throw new BadRequestException(String.format("Project \"%s\" already exists.", name), HttpStatus.BAD_REQUEST);
                });

        ProjectEntity project = projectRepository.saveAndFlush(
                ProjectEntity.builder()
                        .name(name)
                        .build()
        );

        return projectDtoFactory.makeProjectDto(project);
    }

    @Transactional
    public ProjectDto editProject(Long projectId, String name) {

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

        serviceHelper.getProjectOrThrowException(projectId);

        projectRepository.deleteById(projectId);

        return AckDto.makeDefault(true);
    }
}
