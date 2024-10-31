package by.sirius.task.tracker.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegisterRequestDto {

    @Email
    @NotBlank(message = "Email is required!")
    @Size(max = 50)
    private String email;

    @NotBlank(message = "Username is required!")
    @Size(min = 3, max = 20)
    private String username;

    @NotBlank(message = "Password is required!")
    @Size(min = 6, max = 40)
    private String password;

}
