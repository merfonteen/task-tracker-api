package by.sirius.task.tracker.api.services;

import by.sirius.task.tracker.api.dto.AckDto;
import by.sirius.task.tracker.api.dto.AuthRequestDto;
import by.sirius.task.tracker.api.dto.RoleDto;
import by.sirius.task.tracker.api.dto.UserDto;
import by.sirius.task.tracker.api.exceptions.BadRequestException;
import by.sirius.task.tracker.api.factories.UserDtoFactory;
import by.sirius.task.tracker.store.entities.RoleEntity;
import by.sirius.task.tracker.store.entities.UserEntity;
import by.sirius.task.tracker.store.repositories.RoleRepository;
import by.sirius.task.tracker.store.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class UserService {

    private final RoleService roleService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserDtoFactory userDtoFactory;
    private final PasswordEncoder passwordEncoder;

    public List<UserDto> getUsers() {
        List<UserEntity> users = userRepository.findAll();
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


    public UserDto assignRole(String username, String newRole) {

        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("User not found", HttpStatus.BAD_REQUEST));

        RoleEntity role = roleRepository.findByName(newRole)
                .orElseThrow(() -> new BadRequestException("Role not found", HttpStatus.BAD_REQUEST));

        user.getRoles().add(role);

        userRepository.saveAndFlush(user);

        return userDtoFactory.makeUserDto(user);
    }

    @Transactional
    public AckDto deleteUser(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException(("User not found"), HttpStatus.BAD_REQUEST));
        userRepository.delete(user);

        return AckDto.builder().answer(true).build();
    }

    public UserEntity findByUsername(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException(
                        String.format("User with name \"%s\" doesn't exist", username), HttpStatus.BAD_REQUEST));
        return user;
    }
}
