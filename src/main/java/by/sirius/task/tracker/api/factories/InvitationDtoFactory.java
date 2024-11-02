package by.sirius.task.tracker.api.factories;

import by.sirius.task.tracker.api.dto.InvitationDto;
import by.sirius.task.tracker.api.store.entities.InvitationEntity;
import org.springframework.stereotype.Component;

@Component
public class InvitationDtoFactory {

    public InvitationDto makeInvitationDto(InvitationEntity invitation) {
        return InvitationDto.builder()
                .id(invitation.getId())
                .invitingAdmin(invitation.getInvitingAdmin().getUsername())
                .invitedUser(invitation.getInvitedUser().getUsername())
                .status(invitation.getStatus())
                .build();
    }
}
