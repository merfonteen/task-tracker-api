package by.sirius.task.tracker.api.dto;

import lombok.*;

@Data
@Builder
public class AuthRequestDto {
    private String email;
    private String username;
    private String password;
}
