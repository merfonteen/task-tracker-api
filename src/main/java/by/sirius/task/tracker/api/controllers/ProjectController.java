package by.sirius.task.tracker.api.controllers;

import by.sirius.task.tracker.api.dto.AckDto;
import by.sirius.task.tracker.api.dto.InvitationDto;
import by.sirius.task.tracker.api.dto.ProjectDto;
import by.sirius.task.tracker.api.exceptions.BadRequestException;
import by.sirius.task.tracker.store.entities.ProjectEntity;
import by.sirius.task.tracker.store.entities.UserEntity;
import by.sirius.task.tracker.api.services.InvitationService;
import by.sirius.task.tracker.api.services.ProjectService;
import by.sirius.task.tracker.api.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@RestController
public class ProjectController {

    private final UserService userService;
    private final ProjectService projectService;
    private final InvitationService invitationService;

    public static final String FETCH_PROJECTS = "/api/projects";
    public static final String CREATE_PROJECT = "/api/projects";
    public static final String EDIT_PROJECT = "/api/projects/{project_id}";
    public static final String DELETE_PROJECT = "/api/projects/{project_id}";
    public static final String REMOVE_USER_FROM_PROJECT = "/api/projects/{project_id}/users/{username}";
    private static final String SEND_INVITATION_TO_PROJECT = "/api/projects/{project_id}/invitations/send";

    @PreAuthorize("isAuthenticated()")
    @GetMapping(FETCH_PROJECTS)
    public List<ProjectDto> fetchProjects(
            @RequestParam(value = "prefix_name", required = false) Optional<String> optionalPrefixName) {
       return projectService.fetchProjects(optionalPrefixName);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping(CREATE_PROJECT)
    public ProjectDto createProject(@RequestParam("project_name") String name) {
       return projectService.createProject(name);
    }

    @PreAuthorize("@projectSecurityService.hasProjectPermission(#projectId, 'WRITE')")
    @PatchMapping(EDIT_PROJECT)
    public ProjectDto editProject(@PathVariable("project_id") Long projectId, @RequestParam String name) {
        return projectService.editProject(projectId, name);
    }

    @PreAuthorize("@projectSecurityService.hasProjectPermission(#projectId, 'WRITE')")
    @DeleteMapping(DELETE_PROJECT)
    public AckDto deleteProject(@PathVariable("project_id") Long projectId) {
        return projectService.deleteProject(projectId);
    }

    @PreAuthorize("@projectSecurityService.hasProjectPermission(#projectId, 'WRITE')")
    @DeleteMapping(REMOVE_USER_FROM_PROJECT)
    public AckDto removeUserFromProject(@PathVariable("project_id") Long projectId,
                                        @PathVariable("username") String username) {
        return projectService.removeUserFromProject(projectId, username);
    }

    @PostMapping(SEND_INVITATION_TO_PROJECT)
    public InvitationDto sendInvitation(@PathVariable("project_id") Long projectId,
                                        @RequestParam("invited_username") String username,
                                        Principal principal) {

        UserEntity invitingAdmin = userService.findByUsername(principal.getName());

        ProjectEntity project = projectService.getProjectById(projectId);
        if(!projectService.isAdmin(invitingAdmin, project)) {
            throw new BadRequestException("Only project admin can send invitations.", HttpStatus.BAD_REQUEST);
        }

        return invitationService.sendInvitation(invitingAdmin.getUsername(), username, projectId);
    }
}
