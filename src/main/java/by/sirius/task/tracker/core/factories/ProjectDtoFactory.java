package by.sirius.task.tracker.core.factories;

import by.sirius.task.tracker.api.dto.ProjectDto;
import by.sirius.task.tracker.api.dto.ProjectDtoWithTaskStates;
import by.sirius.task.tracker.store.entities.ProjectEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class ProjectDtoFactory {

    private final TaskStateDtoFactory taskStateDtoFactory;

    public ProjectDto makeProjectDto(ProjectEntity project) {
        return ProjectDto.builder()
                .id(project.getId())
                .name(project.getName())
                .owner(project.getAdmin().getUsername())
                .createdAt(project.getCreatedAt())
                .build();
    }

    public ProjectDtoWithTaskStates makeProjectDtoWithTaskStates(ProjectEntity project) {
        return ProjectDtoWithTaskStates.builder()
                .id(project.getId())
                .name(project.getName())
                .owner(project.getAdmin().getUsername())
                .taskStates(project.getTaskStates()
                        .stream()
                        .map(taskStateDtoFactory::makeTaskStateDto)
                        .collect(Collectors.toList()))
                .createdAt(project.getCreatedAt())
                .build();
    }
}
