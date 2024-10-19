package by.sirius.task.tracker.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectDto {
    @NonNull
    private Long id;

    @NonNull
    private String name;

    @NonNull
    private String owner;

    @NonNull
    @JsonProperty("created_at")
    private Instant createdAt;
}
