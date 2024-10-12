package by.sirius.task.tracker.api.services;

import by.sirius.task.tracker.api.dto.AckDto;
import by.sirius.task.tracker.api.dto.InvitationDto;
import by.sirius.task.tracker.api.exceptions.BadRequestException;
import by.sirius.task.tracker.api.exceptions.NotFoundException;
import by.sirius.task.tracker.api.factories.InvitationDtoFactory;
import by.sirius.task.tracker.api.services.helpers.ServiceHelper;
import by.sirius.task.tracker.store.entities.*;
import by.sirius.task.tracker.store.repositories.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final InvitationDtoFactory invitationDtoFactory;
    private final InvitationRepository invitationRepository;
    private final ProjectRoleRepository projectRoleRepository;

    private final ServiceHelper serviceHelper;

    public List<InvitationDto> getUserInvitations(Principal principal) {

       List<InvitationEntity> allInvitations = invitationRepository.findAllByInvitedUser_username(principal.getName());

        if(allInvitations.isEmpty()) {
            throw new NotFoundException("There are no invitations.", HttpStatus.NOT_FOUND);
        }

        return allInvitations.stream()
                .map(invitation -> new InvitationDto(
                        invitation.getId(),
                        invitation.getInvitingAdmin().getUsername(),
                        invitation.getInvitedUser().getUsername(),
                        invitation.getStatus()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public InvitationDto sendInvitation(String invitingAdminUsername, String invitedUsername, Long projectId) {

        UserEntity admin = serviceHelper.getUserOrThrowException(invitingAdminUsername);

        UserEntity user = serviceHelper.getUserOrThrowException(invitedUsername);

        ProjectEntity project = serviceHelper.getProjectOrThrowException(projectId);

        Optional<InvitationEntity> existingInvitation = invitationRepository
                .findByInvitedUserAndProjectAndStatus(user, project, InvitationStatus.SENT);

        if (existingInvitation.isPresent()) {
            throw new BadRequestException("Invitation has already sent.", HttpStatus.BAD_REQUEST);
        }

        InvitationEntity invitation = InvitationEntity.builder()
                .invitingAdmin(admin)
                .invitedUser(user)
                .project(project)
                .status(InvitationStatus.SENT)
                .build();

        InvitationEntity invitationToSave = invitationRepository.saveAndFlush(invitation);

        return invitationDtoFactory.makeInvitationDto(invitationToSave);
    }

    @Transactional
    public AckDto acceptInvitation(Long invitationId) {

        InvitationEntity invitation = serviceHelper.getInvitationOrThrowException(invitationId);

        invitation.setStatus(InvitationStatus.ACCEPTED);

        UserEntity invitedUser = invitation.getInvitedUser();
        ProjectEntity project = invitation.getProject();

        project.getUsers().add(invitedUser);
        invitedUser.getMemberProjects().add(project);

        RoleEntity roleUser = serviceHelper.getUserRoleOrThrowException();

        ProjectRoleEntity projectRole = ProjectRoleEntity.builder()
                .user(invitedUser)
                .project(project)
                .role(roleUser)
                .build();

        invitedUser.getRoles().add(roleUser);

        projectRoleRepository.save(projectRole);
        invitationRepository.save(invitation);

        return AckDto.builder().answer(true).build();
    }

    @Transactional
    public AckDto declineInvitation(Long invitationId) {

        InvitationEntity invitation = serviceHelper.getInvitationOrThrowException(invitationId);
        invitation.setStatus(InvitationStatus.DECLINED);

        invitationRepository.save(invitation);

        return AckDto.builder().answer(true).build();
    }
}
