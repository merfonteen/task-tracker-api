package by.sirius.task.tracker.api.dto;

import by.sirius.task.tracker.store.entities.InvitationStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InvitationDto {

    private Long id;

    @JsonProperty("inviting_admin")
    private String invitingAdmin;

    @JsonProperty("invited_user")
    private String invitedUser;

    private InvitationStatus status;

}
