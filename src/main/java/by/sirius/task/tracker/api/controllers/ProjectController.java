package by.sirius.task.tracker.api.controllers;

import by.sirius.task.tracker.api.dto.AckDto;
import by.sirius.task.tracker.api.dto.InvitationDto;
import by.sirius.task.tracker.api.dto.ProjectDto;
import by.sirius.task.tracker.api.exceptions.BadRequestException;
import by.sirius.task.tracker.api.store.entities.ProjectEntity;
import by.sirius.task.tracker.api.store.entities.UserEntity;
import by.sirius.task.tracker.api.services.InvitationService;
import by.sirius.task.tracker.api.services.ProjectService;
import by.sirius.task.tracker.api.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RequiredArgsConstructor
@RestController
@Slf4j
public class ProjectController {

    private final UserService userService;
    private final ProjectService projectService;
    private final InvitationService invitationService;

    public static final String GET_PROJECTS = "/api/projects";
    public static final String CREATE_PROJECT = "/api/projects";
    public static final String EDIT_PROJECT = "/api/projects/{project_id}";
    public static final String DELETE_PROJECT = "/api/projects/{project_id}";
    public static final String REMOVE_USER_FROM_PROJECT = "/api/projects/{project_id}/users/{username}";
    public static final String SEND_INVITATION_TO_PROJECT = "/api/projects/{project_id}/invitations/send";

    @PreAuthorize("isAuthenticated()")
    @GetMapping(GET_PROJECTS)
    public List<ProjectDto> getProjects(Principal principal) {
        log.info("Getting all projects");
        return projectService.getProjects(principal.getName());
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping(CREATE_PROJECT)
    public ProjectDto createProject(@RequestParam String name, Principal principal) {
        log.info("Creating project with name: {}", name);
        return projectService.createProject(name, principal.getName());
    }

    @PreAuthorize("@projectSecurityService.hasProjectPermission(#projectId, 'WRITE')")
    @PatchMapping(EDIT_PROJECT)
    public ProjectDto editProject(@PathVariable("project_id") Long projectId, @RequestParam String name) {
        log.info("Editing project with ID: {}, new name: {}", projectId, name);
        return projectService.editProject(projectId, name);
    }

    @PreAuthorize("@projectSecurityService.hasProjectPermission(#projectId, 'WRITE')")
    @DeleteMapping(DELETE_PROJECT)
    public AckDto deleteProject(@PathVariable("project_id") Long projectId) {
        log.warn("Deleting project with ID: {}", projectId);
        return projectService.deleteProject(projectId);
    }

    @PreAuthorize("@projectSecurityService.hasProjectPermission(#projectId, 'WRITE')")
    @DeleteMapping(REMOVE_USER_FROM_PROJECT)
    public AckDto removeUserFromProject(@PathVariable("project_id") Long projectId,
                                        @PathVariable String username) {
        log.warn("Removing user {} from project with ID: {}", username, projectId);
        return projectService.removeUserFromProject(projectId, username);
    }

    @PostMapping(SEND_INVITATION_TO_PROJECT)
    public InvitationDto sendInvitation(@PathVariable("project_id") Long projectId,
                                        @RequestParam String username,
                                        Principal principal) {
        log.info("User {} is inviting {} to project with ID: {}", principal.getName(), username, projectId);

        UserEntity invitingAdmin = userService.findByUsername(principal.getName());
        ProjectEntity project = projectService.getProjectById(projectId);

        if (!projectService.isAdmin(invitingAdmin, project)) {
            throw new BadRequestException("Only project admin can send invitations.", HttpStatus.BAD_REQUEST);
        }

        return invitationService.sendInvitation(invitingAdmin.getUsername(), username, projectId);
    }
}
