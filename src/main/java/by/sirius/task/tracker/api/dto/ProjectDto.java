package by.sirius.task.tracker.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectDto implements Serializable {
    private Long id;
    private String name;
    private String owner;

    @JsonProperty("created_at")
    private Instant createdAt;

}
