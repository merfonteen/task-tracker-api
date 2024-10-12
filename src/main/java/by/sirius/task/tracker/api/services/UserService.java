package by.sirius.task.tracker.api.services;

import by.sirius.task.tracker.api.dto.AuthRequestDto;
import by.sirius.task.tracker.api.dto.RoleDto;
import by.sirius.task.tracker.api.dto.UserDto;
import by.sirius.task.tracker.api.services.helpers.ServiceHelper;
import by.sirius.task.tracker.store.entities.UserEntity;
import by.sirius.task.tracker.store.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class UserService {

    private final RoleService roleService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final ServiceHelper serviceHelper;

    public List<UserDto> getUsers(Long projectId) {
        List<UserEntity> users = userRepository.findAllByMemberProjects_Id(projectId);
        return users.stream()
                .map(user -> new UserDto(
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
    public UserEntity createNewUser(AuthRequestDto authRequestDto) {
        UserEntity user = new UserEntity();
        user.setUsername(authRequestDto.getUsername());
        user.setPassword(passwordEncoder.encode(authRequestDto.getPassword()));
        user.setRoles(List.of(roleService.getAdminRole()));
        user.setEnabled(true);
        return userRepository.saveAndFlush(user);
    }

    public UserEntity findByUsername(String username) {
        return serviceHelper.getUserOrThrowException(username);
    }
}
