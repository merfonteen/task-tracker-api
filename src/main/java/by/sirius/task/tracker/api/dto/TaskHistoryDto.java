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
public class TaskHistoryDto {
    private Long id;

    @JsonProperty("task_id")
    private Long taskId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("change_type")
    private String changeType;

    @JsonProperty("field_name")
    private String fieldName;

    @JsonProperty("old_value")
    private String oldValue;

    @JsonProperty("new_value")
    private String newValue;

    @JsonProperty("timestamp")
    private Instant timestamp;
}
