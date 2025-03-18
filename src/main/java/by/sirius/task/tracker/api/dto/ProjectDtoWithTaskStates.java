package by.sirius.task.tracker.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectDtoWithTaskStates {
    private Long id;
    private String name;
    private String owner;

    @JsonProperty("created_at")
    private Instant createdAt;
    private List<TaskStateDto> taskStates;
}
