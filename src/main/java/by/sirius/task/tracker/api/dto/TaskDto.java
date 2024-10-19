package by.sirius.task.tracker.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskDto {
    @NonNull
    private Long id;

    @NonNull
    private String name;

    private String assignedUser;

    @JsonProperty("left_task_id")
    private Long leftTaskId;

    @JsonProperty("right_task_id")
    private Long rightTaskId;

    @NonNull
    @JsonProperty("created_at")
    private Instant createdAt;
}
