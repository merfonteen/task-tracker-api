package by.sirius.task.tracker.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskDto {
    private Long id;
    private String name;

    @JsonProperty("assigned_user")
    private String assignedUser;

    @JsonProperty("left_task_id")
    private Long leftTaskId;

    @JsonProperty("right_task_id")
    private Long rightTaskId;

    @JsonProperty("created_at")
    private Instant createdAt;
}

