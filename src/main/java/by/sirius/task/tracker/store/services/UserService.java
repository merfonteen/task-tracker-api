package by.sirius.task.tracker.store.services;

import by.sirius.task.tracker.api.dto.AuthRequestDto;
import by.sirius.task.tracker.api.exceptions.BadRequestException;
import by.sirius.task.tracker.store.entities.UserEntity;
import by.sirius.task.tracker.store.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserService {

    private final RoleService roleService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<UserEntity> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public void saveUser(UserEntity user) {
        if(userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new BadRequestException(
                    String.format("User with name \"%s\" already exists.", user.getUsername()), HttpStatus.BAD_REQUEST);
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.saveAndFlush(user);
    }

    public UserEntity createNewUser(AuthRequestDto authRequestDto) {
        UserEntity user = new UserEntity();
        user.setUsername(authRequestDto.getUsername());
        user.setPassword(passwordEncoder.encode(authRequestDto.getPassword()));
        user.setRoles(List.of(roleService.getUserRole()));
        user.setEnabled(true);
        return userRepository.saveAndFlush(user);
    }
}
