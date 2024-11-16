package by.sirius.task.tracker.api.controllers;

import by.sirius.task.tracker.api.dto.UserDto;
import by.sirius.task.tracker.core.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;

    private final String GET_USERS = "/users/{project_id}";

    @PreAuthorize("@projectSecurityService.hasProjectPermission(#projectId, 'WRITE')")
    @GetMapping(GET_USERS)
    public List<UserDto> getUsers(@PathVariable("project_id") Long projectId) {
        return userService.getUsers(projectId);
    }
}
