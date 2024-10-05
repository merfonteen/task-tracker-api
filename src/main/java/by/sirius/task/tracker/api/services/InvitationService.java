package by.sirius.task.tracker.api.services;

import by.sirius.task.tracker.api.dto.AckDto;
import by.sirius.task.tracker.api.dto.InvitationDto;
import by.sirius.task.tracker.api.exceptions.BadRequestException;
import by.sirius.task.tracker.api.factories.InvitationDtoFactory;
import by.sirius.task.tracker.store.entities.*;
import by.sirius.task.tracker.store.repositories.InvitationRepository;
import by.sirius.task.tracker.store.repositories.ProjectRepository;
import by.sirius.task.tracker.store.repositories.RoleRepository;
import by.sirius.task.tracker.store.repositories.UserRepository;
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

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ProjectRepository projectRepository;
    private final InvitationDtoFactory invitationDtoFactory;
    private final InvitationRepository invitationRepository;

    public List<InvitationDto> getUserInvitations(Principal principal) {

       List<InvitationEntity> allInvitations = invitationRepository.findAllByInvitedUser_username(principal.getName());

        if(allInvitations.isEmpty()) {
            throw new BadRequestException("There are no invitations.", HttpStatus.BAD_REQUEST);
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

        UserEntity admin = userRepository.findByUsername(invitingAdminUsername)
                .orElseThrow(() -> new BadRequestException("Admin not found", HttpStatus.BAD_REQUEST));

        UserEntity user = userRepository.findByUsername(invitedUsername)
                .orElseThrow(() -> new BadRequestException("User not found", HttpStatus.BAD_REQUEST));

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BadRequestException("Project not found", HttpStatus.BAD_REQUEST));

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
        InvitationEntity invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new BadRequestException("Invitation not found", HttpStatus.BAD_REQUEST));

        invitation.setStatus(InvitationStatus.ACCEPTED);

        UserEntity invitedUser = invitation.getInvitedUser();

        ProjectEntity project = invitation.getProject();
        project.getUsers().add(invitedUser);

        invitedUser.getMemberProjects().add(project);

        RoleEntity newRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new BadRequestException("User role not found" , HttpStatus.BAD_REQUEST));

        if(!invitedUser.getRoles().contains(newRole)) {
            invitedUser.getRoles().add(newRole);
        }

        userRepository.save(invitedUser);
        invitationRepository.save(invitation);

        return AckDto.builder().answer(true).build();
    }

    @Transactional
    public AckDto declineInvitation(Long invitationId) {
        InvitationEntity invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new BadRequestException("Invitation not found", HttpStatus.BAD_REQUEST));

        invitation.setStatus(InvitationStatus.DECLINED);

        invitationRepository.saveAndFlush(invitation);

        return AckDto.builder().answer(true).build();
    }
}
