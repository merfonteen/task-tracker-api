package by.sirius.task.tracker.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskDto implements Serializable {

    private Long id;
    private String name;
    private String assignedUser;
    private Long leftTaskId;

    @JsonProperty("right_task_id")
    private Long rightTaskId;

    @JsonProperty("created_at")
    private Instant createdAt;

}

