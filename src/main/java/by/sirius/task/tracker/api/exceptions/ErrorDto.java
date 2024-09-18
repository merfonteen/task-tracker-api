package by.sirius.task.tracker.api.exceptions;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorDto {

    private String error;

    @JsonProperty("error_description")
    private String errorDescription;
}
