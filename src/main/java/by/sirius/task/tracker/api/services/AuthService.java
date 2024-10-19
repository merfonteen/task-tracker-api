package by.sirius.task.tracker.api.services;

import by.sirius.task.tracker.api.dto.AuthRequestDto;
import by.sirius.task.tracker.api.dto.AuthResponseDto;
import by.sirius.task.tracker.api.exceptions.BadRequestException;
import by.sirius.task.tracker.security.CustomUserDetailsService;
import by.sirius.task.tracker.security.JwtTokenUtil;
import by.sirius.task.tracker.store.entities.UserEntity;
import by.sirius.task.tracker.store.repositories.UserRepository;
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

    public ResponseEntity<?> register(AuthRequestDto authRequestDto) {

        if (userRepository.existsByUsername(authRequestDto.getUsername())) {
            throw new BadRequestException("Username is already taken!", HttpStatus.BAD_REQUEST);
        }

        UserEntity user = userService.createNewUser(authRequestDto);

        return ResponseEntity.ok("User registered successfully!");
    }

    public ResponseEntity<?> login(AuthRequestDto authRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword()));
        }
        catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password!");
        }

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(authRequest.getUsername());
        String token = jwtTokenUtil.generateToken(userDetails);

        return ResponseEntity.ok(new AuthResponseDto(token));
    }
}
