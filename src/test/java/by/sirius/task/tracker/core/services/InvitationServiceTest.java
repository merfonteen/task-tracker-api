package by.sirius.task.tracker.core.services;

import by.sirius.task.tracker.api.dto.AckDto;
import by.sirius.task.tracker.api.dto.InvitationDto;
import by.sirius.task.tracker.api.exceptions.BadRequestException;
import by.sirius.task.tracker.api.exceptions.NotFoundException;
import by.sirius.task.tracker.core.factories.InvitationDtoFactory;
import by.sirius.task.tracker.core.services.helpers.ServiceHelper;
import by.sirius.task.tracker.store.entities.*;
import by.sirius.task.tracker.store.repositories.InvitationRepository;
import by.sirius.task.tracker.store.repositories.ProjectRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock
    private EmailService emailService;

    @Mock
    private ServiceHelper serviceHelper;

    @Mock
    private InvitationDtoFactory invitationDtoFactory;

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private ProjectRoleRepository projectRoleRepository;

    @InjectMocks
    private InvitationService invitationService;

    @Test
    void testGetUserInvitations_Success() {
        String invitingUsername = "Inviting User";
        String invitedUsername = "Invited User";

        UserEntity invitingUser = UserEntity.builder()
                .id(1L)
                .username(invitingUsername)
                .build();

        UserEntity invitedUser = UserEntity.builder()
                .id(2L)
                .username(invitedUsername)
                .build();

        InvitationEntity invitation = InvitationEntity.builder()
                .id(1L)
                .invitedUser(invitedUser)
                .invitingAdmin(invitingUser)
                .build();

        InvitationDto expectedDto = InvitationDto.builder()
                .id(1L)
                .invitedUser(invitedUsername)
                .invitingAdmin(invitingUsername)
                .build();

        List<InvitationEntity> invitations = new ArrayList<>(List.of(invitation));

        when(invitationRepository.findAllByInvitedUser_username(invitedUsername)).thenReturn(invitations);

        List<InvitationDto> actual = invitationService.getUserInvitations(invitedUsername);

        assertNotNull(actual);
        assertEquals(1, actual.size());
        assertEquals(expectedDto, actual.get(0));
    }

    @Test
    void testSendInvitation_Success() {
        Long projectId = 1L;
        String invitedUsername = "Invited User";
        String invitingAdminUsername = "Inviting User";

        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .name("Test Project")
                .build();

        UserEntity invitedUser = UserEntity.builder()
                .id(1L)
                .username(invitedUsername)
                .email("invited@gmail.com")
                .build();

        UserEntity invitingUser = UserEntity.builder()
                .id(2L)
                .username(invitingAdminUsername)
                .email("admin@gmail.com")
                .build();

        InvitationEntity invitation = InvitationEntity.builder()
                .id(1L)
                .invitingAdmin(invitingUser)
                .invitedUser(invitedUser)
                .build();

        InvitationDto expectedDto = InvitationDto.builder()
                .id(1L)
                .invitingAdmin(invitingUser.getUsername())
                .invitedUser(invitedUser.getUsername())
                .build();

        when(serviceHelper.findProjectByIdOrThrowException(projectId)).thenReturn(project);
        when(serviceHelper.findUserByUsernameOrThrowException(invitedUsername)).thenReturn(invitedUser);
        when(serviceHelper.findUserByUsernameOrThrowException(invitingAdminUsername)).thenReturn(invitingUser);
        when(invitationRepository.findByInvitedUserAndProjectAndStatus(invitedUser, project, InvitationStatus.SENT))
                .thenReturn(Optional.empty());
        when(invitationRepository.save(any(InvitationEntity.class))).thenReturn(invitation);
        when(invitationDtoFactory.makeInvitationDto(invitation)).thenReturn(expectedDto);

        InvitationDto actualDto = invitationService.sendInvitation(invitingAdminUsername, invitedUsername, projectId);

        assertNotNull(actualDto);
        assertEquals(expectedDto, actualDto);
        verify(emailService).sendEmail(
                eq("invited@gmail.com"),
                eq("Project Invitation"),
                eq("You have been invited to join the project: Test Project")
        );
    }

    @Test
    void testSendInvitations_WhenInvitationAlreadyExists_ShouldThrowException() {
        Long projectId = 1L;
        String invitedUsername = "Invited User";
        String invitingAdminUsername = "Inviting User";

        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .build();

        UserEntity invitedUser = UserEntity.builder()
                .id(1L)
                .username(invitedUsername)
                .build();

        UserEntity invitingUser = UserEntity.builder()
                .id(2L)
                .username(invitingAdminUsername)
                .build();

        when(serviceHelper.findProjectByIdOrThrowException(projectId)).thenReturn(project);
        when(serviceHelper.findUserByUsernameOrThrowException(invitedUsername)).thenReturn(invitedUser);
        when(serviceHelper.findUserByUsernameOrThrowException(invitingAdminUsername)).thenReturn(invitingUser);
        when(invitationRepository.findByInvitedUserAndProjectAndStatus(invitedUser, project, InvitationStatus.SENT))
                .thenReturn(Optional.of(new InvitationEntity()));

        assertThrows(BadRequestException.class,
                () -> invitationService.sendInvitation(invitingAdminUsername, invitedUsername, projectId));

        verifyNoInteractions(emailService);
        verify(invitationRepository, never()).save(any());
    }

    @Test
    void testSendInvitation_WhenUserNotFound_ShouldThrowException() {
        String invitingAdminUsername = "adminUser";
        String invitedUsername = "nonExistentUser";
        Long projectId = 1L;

        UserEntity invitingAdmin = UserEntity.builder()
                .id(1L)
                .username(invitingAdminUsername)
                .build();

        when(serviceHelper.findUserByUsernameOrThrowException(invitingAdminUsername)).thenReturn(invitingAdmin);
        when(serviceHelper.findUserByUsernameOrThrowException(invitedUsername)).thenThrow(
                new NotFoundException("User not found", HttpStatus.NOT_FOUND));

        assertThrows(NotFoundException.class,
                () -> invitationService.sendInvitation(invitingAdminUsername, invitedUsername, projectId));

        verifyNoInteractions(emailService);
        verify(invitationRepository, never()).save(any());
    }

    @Test
    void testSendInvitation_WhenProjectNotFound_ShouldThrowException() {
        String invitingAdminUsername = "adminUser";
        String invitedUsername = "invitedUser";
        Long projectId = 99L;

        UserEntity invitingAdmin = UserEntity.builder()
                .id(1L)
                .username(invitingAdminUsername)
                .build();

        UserEntity invitedUser = UserEntity.builder()
                .id(2L)
                .username(invitedUsername)
                .build();

        when(serviceHelper.findUserByUsernameOrThrowException(invitingAdminUsername)).thenReturn(invitingAdmin);
        when(serviceHelper.findUserByUsernameOrThrowException(invitedUsername)).thenReturn(invitedUser);
        when(serviceHelper.findProjectByIdOrThrowException(projectId)).thenThrow(
                new NotFoundException("Project not found", HttpStatus.NOT_FOUND));

        assertThrows(NotFoundException.class,
                () -> invitationService.sendInvitation(invitingAdminUsername, invitedUsername, projectId));

        verifyNoInteractions(emailService);
        verify(invitationRepository, never()).save(any());
    }

    @Test
    void testAcceptInvitation_Success() {
        Long invitationId = 1L;
        String username = "Test User";

        UserEntity user = UserEntity.builder()
                .id(1L)
                .username(username)
                .memberProjects(new ArrayList<>())
                .roles(new ArrayList<>())
                .build();

        ProjectEntity project = ProjectEntity.builder()
                .id(1L)
                .users(new ArrayList<>())
                .build();

        InvitationEntity invitation = InvitationEntity.builder()
                .id(invitationId)
                .invitedUser(user)
                .project(project)
                .status(InvitationStatus.SENT)
                .build();

        RoleEntity role = RoleEntity.builder()
                .id(1L)
                .name("ROLE_USER")
                .build();

        ProjectRoleEntity projectRole = ProjectRoleEntity.builder()
                .user(user)
                .project(project)
                .role(role)
                .build();

        when(serviceHelper.findInvitationByIdOrThrowException(invitationId)).thenReturn(invitation);
        when(serviceHelper.getUserRoleOrThrowException()).thenReturn(role);
        when(projectRoleRepository.save(any(ProjectRoleEntity.class))).thenReturn(projectRole);
        when(invitationRepository.save(any(InvitationEntity.class))).thenReturn(invitation);

        AckDto result = invitationService.acceptInvitation(invitationId, username);

        assertNotNull(result);
        assertTrue(result.getAnswer());
        assertEquals(InvitationStatus.ACCEPTED, invitation.getStatus());
        verify(projectRoleRepository).save(any(ProjectRoleEntity.class));
        verify(invitationRepository).save(invitation);
    }

    @Test
    void testAcceptInvitation_WhenInvitationIsNotForUser_ShouldThrowException() {
        Long invitationId = 1L;
        String username = "wrongUser";

        UserEntity invitedUser = UserEntity.builder()
                .id(1L)
                .username("correctedUser")
                .build();

        InvitationEntity invitation = InvitationEntity.builder()
                .id(invitationId)
                .invitedUser(invitedUser)
                .status(InvitationStatus.SENT)
                .build();

        when(serviceHelper.findInvitationByIdOrThrowException(invitationId)).thenReturn(invitation);

        assertThrows(BadRequestException.class,
                () -> invitationService.acceptInvitation(invitationId, username));

        verifyNoInteractions(projectRoleRepository);
        verifyNoInteractions(invitationRepository);
    }

    @Test
    void testAcceptInvitation_WhenInvitationIsDeclined_ShouldThrowException() {
        Long invitationId = 1L;
        String username = "testUser";

        UserEntity invitedUser = UserEntity.builder()
                .username(username)
                .build();

        InvitationEntity invitation = InvitationEntity.builder()
                .id(invitationId)
                .invitedUser(invitedUser)
                .status(InvitationStatus.DECLINED)
                .build();

        when(serviceHelper.findInvitationByIdOrThrowException(invitationId)).thenReturn(invitation);

        assertThrows(BadRequestException.class,
                () -> invitationService.acceptInvitation(invitationId, username));

        verifyNoInteractions(projectRoleRepository);
        verifyNoInteractions(invitationRepository);
    }

    @Test
    void testAcceptInvitation_WhenInvitationNotFound_ShouldThrowException() {
        Long invitationId = 1L;
        String username = "testUser";

        when(serviceHelper.findInvitationByIdOrThrowException(invitationId))
                .thenThrow(new NotFoundException("Invitation not found", HttpStatus.NOT_FOUND));

        assertThrows(NotFoundException.class,
                () -> invitationService.acceptInvitation(invitationId, username));

        verifyNoInteractions(projectRoleRepository);
        verifyNoInteractions(invitationRepository);
    }

    @Test
    void testDeclineInvitation_Success() {
        Long invitationId = 1L;
        String username = "testUser";

        InvitationEntity invitation = InvitationEntity.builder()
                .id(invitationId)
                .invitingAdmin(UserEntity.builder().username(username).build())
                .status(InvitationStatus.SENT)
                .build();

        when(serviceHelper.findInvitationByIdOrThrowException(invitationId)).thenReturn(invitation);

        AckDto result = invitationService.declineInvitation(invitationId, username);

        assertNotNull(result);
        assertTrue(result.getAnswer());
        assertEquals(InvitationStatus.DECLINED, invitation.getStatus());
        verify(invitationRepository).save(invitation);
    }

    @Test
    void testDeclineInvitation_InvitationNotOwnedByUser_ShouldThrowException() {
        Long invitationId = 1L;
        String username = "testUser";
        String otherUsername = "otherAdmin";

        InvitationEntity invitation = InvitationEntity.builder()
                .id(invitationId)
                .invitingAdmin(UserEntity.builder().username(otherUsername).build())
                .status(InvitationStatus.SENT)
                .build();

        when(serviceHelper.findInvitationByIdOrThrowException(invitationId)).thenReturn(invitation);

        assertThrows(BadRequestException.class,
                () -> invitationService.declineInvitation(invitationId, username));

        verify(invitationRepository, never()).save(any());
    }

    @Test
    void testDeclineInvitation_InvitationNotFound_ShouldThrowException() {
        Long invitationId = 1L;
        String username = "testUser";

        when(serviceHelper.findInvitationByIdOrThrowException(invitationId)).thenThrow(
                new NotFoundException("Invitation not found", HttpStatus.NOT_FOUND));

        assertThrows(NotFoundException.class,
                () -> invitationService.declineInvitation(invitationId, username));

        verify(invitationRepository, never()).save(any());
    }
}