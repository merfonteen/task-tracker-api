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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.Principal;
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

    @Cacheable(value = "invitations", key = "#username")
    public List<InvitationDto> getUserInvitations(String username) {

       List<InvitationEntity> allInvitations = invitationRepository.findAllByInvitedUser_username(username);

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

    @CacheEvict(value = "invitations", key = "#invitedUsername")
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

        emailService.sendEmail(
                user.getEmail(),
                "Project Invitation",
                "You have been invited to join the project: " + invitation.getProject().getName()
        );

        return invitationDtoFactory.makeInvitationDto(invitationToSave);
    }

    @CacheEvict(value = "invitations", key = "#username")
    @Transactional
    public AckDto acceptInvitation(Long invitationId, String username) {

        InvitationEntity invitation = serviceHelper.getInvitationOrThrowException(invitationId);

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

        InvitationEntity invitation = serviceHelper.getInvitationOrThrowException(invitationId);
        invitation.setStatus(InvitationStatus.DECLINED);

        invitationRepository.save(invitation);

        return AckDto.builder().answer(true).build();
    }
}
