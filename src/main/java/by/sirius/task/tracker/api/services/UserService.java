package by.sirius.task.tracker.api.services;

import by.sirius.task.tracker.api.dto.RegisterRequestDto;
import by.sirius.task.tracker.api.dto.RoleDto;
import by.sirius.task.tracker.api.dto.UserDto;
import by.sirius.task.tracker.api.services.helpers.ServiceHelper;
import by.sirius.task.tracker.api.store.entities.UserEntity;
import by.sirius.task.tracker.api.store.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class UserService {

    private final RoleService roleService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final ServiceHelper serviceHelper;

    public List<UserDto> getUsers(Long projectId) {
        log.info("Getting all users from project with id: {}", projectId);
        List<UserEntity> users = userRepository.findAllByMemberProjects_Id(projectId);
        return users.stream()
                .map(user -> new UserDto(
                        user.getEmail(),
                        user.getUsername(),
                        user.getRoles().stream()
                                .map(role -> RoleDto.builder()
                                        .id(role.getId())
                                        .name(role.getName())
                                        .build())
                                .collect(Collectors.toList()),
                        user.isEnabled()))
                .collect(Collectors.toList());
    }

    @Transactional
    public UserEntity createNewUser(RegisterRequestDto registerRequestDto) {
        log.info("Creating user for register...");
        UserEntity user = new UserEntity();
        user.setEmail(registerRequestDto.getEmail());
        user.setUsername(registerRequestDto.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequestDto.getPassword()));
        user.setRoles(List.of(roleService.getAdminRole()));
        user.setEnabled(true);
        return userRepository.saveAndFlush(user);
    }

    public UserEntity findByUsername(String username) {
        return serviceHelper.getUserOrThrowException(username);
    }
}
