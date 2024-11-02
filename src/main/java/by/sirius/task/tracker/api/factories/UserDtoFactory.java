package by.sirius.task.tracker.api.factories;

import by.sirius.task.tracker.api.dto.RoleDto;
import by.sirius.task.tracker.api.dto.UserDto;
import by.sirius.task.tracker.api.store.entities.UserEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserDtoFactory {

    public UserDto makeUserDto(UserEntity user) {

        List<RoleDto> roleDtos = user.getRoles().stream()
                .map(role -> RoleDto.builder()
                        .id(role.getId())
                        .name(role.getName())
                        .build())
                .toList();

        return UserDto.builder()
                .username(user.getUsername())
                .roles(roleDtos)
                .enabled(user.isEnabled())
                .build();
    }
}
