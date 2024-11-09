package by.sirius.task.tracker.api.dto;

import by.sirius.task.tracker.api.store.entities.InvitationStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InvitationDto implements Serializable {
    @NonNull
    private Long id;

    @NonNull
    @JsonProperty("inviting_admin")
    private String invitingAdmin;

    @NonNull
    @JsonProperty("invited_user")
    private String invitedUser;

    @NonNull
    private InvitationStatus status;
}
