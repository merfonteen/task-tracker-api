package by.sirius.task.tracker.api.controllers;

import by.sirius.task.tracker.api.dto.AckDto;
import by.sirius.task.tracker.api.dto.InvitationDto;
import by.sirius.task.tracker.api.services.InvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    private static final String GET_ALL_USERS_INVITATIONS = "/api/invitations";
    private static final String ACCEPT_INVITATION = "/api/invitations/accept/{invitation_id}";
    private static final String DECLINE_INVITATION = "/api/invitations/decline/{invitation_id}";

    @GetMapping(GET_ALL_USERS_INVITATIONS)
    public List<InvitationDto> getUserInvitations(Principal principal) {
        return invitationService.getUserInvitations(principal);
    }

    @PostMapping(ACCEPT_INVITATION)
    public AckDto acceptInvitation(@PathVariable("invitation_id") Long invitationId) {
        return invitationService.acceptInvitation(invitationId);
    }

    @PostMapping(DECLINE_INVITATION)
    public AckDto declineInvitation(@PathVariable("invitation_id") Long invitationId) {
        return invitationService.declineInvitation(invitationId);
    }
}
