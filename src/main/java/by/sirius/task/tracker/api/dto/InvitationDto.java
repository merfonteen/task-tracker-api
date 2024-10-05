package by.sirius.task.tracker.api.dto;

import by.sirius.task.tracker.store.entities.InvitationStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InvitationDto {
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
