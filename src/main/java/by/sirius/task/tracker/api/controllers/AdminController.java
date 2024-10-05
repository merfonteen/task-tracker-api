package by.sirius.task.tracker.api.controllers;

import by.sirius.task.tracker.api.dto.AckDto;
import by.sirius.task.tracker.api.dto.UserDto;
import by.sirius.task.tracker.api.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminController {

    private final UserService userService;

    private final String GET_USERS = "/users";
    private final String CHANGE_USER_ROLE = "/users/{username}/change_role";
    private final String DELETE_USER = "/users/{username}";

    @GetMapping(GET_USERS)
    public List<UserDto> getUsers() {
        return userService.getUsers();
    }

    @PutMapping(CHANGE_USER_ROLE)
    public UserDto assignRoleToUser(@PathVariable(name = "username") String username,
                                    @RequestParam(name = "role") String role) {
        return userService.assignRole(username, role);
    }

    @DeleteMapping(DELETE_USER)
    public AckDto deleteUser(@PathVariable(name = "username") String username) {
        return userService.deleteUser(username);
    }
}
