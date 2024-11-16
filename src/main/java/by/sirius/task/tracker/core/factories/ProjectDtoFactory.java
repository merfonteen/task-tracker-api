package by.sirius.task.tracker.core.factories;

import by.sirius.task.tracker.api.dto.ProjectDto;
import by.sirius.task.tracker.store.entities.ProjectEntity;
import org.springframework.stereotype.Component;

@Component
public class ProjectDtoFactory {

    public ProjectDto makeProjectDto(ProjectEntity projectEntity) {
        return ProjectDto.builder()
                .id(projectEntity.getId())
                .name(projectEntity.getName())
                .owner(projectEntity.getAdmin().getUsername())
                .createdAt(projectEntity.getCreatedAt())
                .build();
    }
}
