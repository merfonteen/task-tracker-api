package by.sirius.task.tracker.api.services;

import by.sirius.task.tracker.api.dto.LoginRequestDto;
import by.sirius.task.tracker.api.dto.RegisterRequestDto;
import by.sirius.task.tracker.api.dto.AuthResponseDto;
import by.sirius.task.tracker.api.exceptions.BadRequestException;
import by.sirius.task.tracker.api.security.CustomUserDetailsService;
import by.sirius.task.tracker.api.security.JwtTokenUtil;
import by.sirius.task.tracker.api.store.entities.UserEntity;
import by.sirius.task.tracker.api.store.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;

    public ResponseEntity<?> register(RegisterRequestDto registerRequestDto) {
        if (userRepository.existsByUsername(registerRequestDto.getUsername())) {
            throw new BadRequestException("Username is already taken!", HttpStatus.BAD_REQUEST);
        }

        if (userRepository.existsByEmail(registerRequestDto.getEmail())) {
            throw new BadRequestException("Email is already in use!", HttpStatus.BAD_REQUEST);
        }

        UserEntity user = userService.createNewUser(registerRequestDto);

        return ResponseEntity.ok("User registered successfully!");
    }

    public ResponseEntity<?> login(LoginRequestDto loginRequestDto) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequestDto.getUsername(), loginRequestDto.getPassword()));
        }
        catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password!");
        }

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(loginRequestDto.getUsername());
        String token = jwtTokenUtil.generateToken(userDetails);

        return ResponseEntity.ok(new AuthResponseDto(token));
    }
}
