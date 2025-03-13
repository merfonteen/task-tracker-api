package by.sirius.task.tracker.core.services;

import by.sirius.task.tracker.api.dto.AckDto;
import by.sirius.task.tracker.api.dto.InvitationDto;
import by.sirius.task.tracker.api.exceptions.BadRequestException;
import by.sirius.task.tracker.core.factories.InvitationDtoFactory;
import by.sirius.task.tracker.core.services.helpers.ServiceHelper;
import by.sirius.task.tracker.store.entities.*;
import by.sirius.task.tracker.store.repositories.InvitationRepository;
import by.sirius.task.tracker.store.repositories.ProjectRoleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final EmailService emailService;
    private final InvitationDtoFactory invitationDtoFactory;
    private final InvitationRepository invitationRepository;
    private final ProjectRoleRepository projectRoleRepository;

    private final ServiceHelper serviceHelper;

    public List<InvitationDto> getUserInvitations(String username) {

       List<InvitationEntity> userInvitations = invitationRepository.findAllByInvitedUser_username(username);

        return userInvitations.stream()
                .map(invitation -> new InvitationDto(
                        invitation.getId(),
                        invitation.getInvitingAdmin().getUsername(),
                        invitation.getInvitedUser().getUsername(),
                        invitation.getStatus()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public InvitationDto sendInvitation(String invitingAdminName, String invitedUsername, Long projectId) {
        UserEntity admin = serviceHelper.findUserByUsernameOrThrowException(invitingAdminName);
        UserEntity user = serviceHelper.findUserByUsernameOrThrowException(invitedUsername);
        ProjectEntity project = serviceHelper.findProjectByIdOrThrowException(projectId);

        if(!project.getAdmin().getUsername().equals(invitingAdminName)) {
            throw new BadRequestException("Only project admin can send invitations", HttpStatus.BAD_REQUEST);
        }

        Optional<InvitationEntity> existingInvitation = invitationRepository
                .findByInvitedUserAndProjectAndStatus(user, project, InvitationStatus.SENT);

        if (existingInvitation.isPresent()) {
            throw new BadRequestException("The user has already received an invitation from you", HttpStatus.BAD_REQUEST);
        }

        InvitationEntity invitation = InvitationEntity.builder()
                .invitingAdmin(admin)
                .invitedUser(user)
                .project(project)
                .status(InvitationStatus.SENT)
                .build();

        InvitationEntity invitationToSave = invitationRepository.save(invitation);

        emailService.sendEmail(
                user.getEmail(),
                "Project Invitation",
                "You have been invited to join the project: " + invitation.getProject().getName()
        );

        return invitationDtoFactory.makeInvitationDto(invitationToSave);
    }

    @Transactional
    public AckDto acceptInvitation(Long invitationId, String username) {

        InvitationEntity invitation = serviceHelper.findInvitationByIdOrThrowException(invitationId);

        if(!invitation.getInvitedUser().getUsername().equals(username)) {
            throw new BadRequestException("You cannot accept an invitation that is not yours", HttpStatus.BAD_REQUEST);
        }

        if(invitation.getStatus().equals(InvitationStatus.DECLINED)) {
            throw new BadRequestException("This invitation has been declined", HttpStatus.BAD_REQUEST);
        }

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
    public AckDto declineInvitation(Long invitationId, String username) {

        InvitationEntity invitation = serviceHelper.findInvitationByIdOrThrowException(invitationId);

        if(!invitation.getInvitingAdmin().getUsername().equals(username)) {
            throw new BadRequestException("You cannot decline an invitation that is not yours", HttpStatus.BAD_REQUEST);
        }

        invitation.setStatus(InvitationStatus.DECLINED);
        invitationRepository.save(invitation);

        return AckDto.builder().answer(true).build();
    }
}
