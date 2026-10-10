package com.rideapp.rideservice.security;

import com.rideapp.rideservice.dto.AuthResponse;
import com.rideapp.rideservice.dto.LoginRequest;
import com.rideapp.rideservice.dto.RegisterRequest;
import com.rideapp.rideservice.model.User;
import com.rideapp.rideservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());

        User saved = userRepository.save(user);
        log.info("Registered {} {}", saved.getRole(), saved.getId());
        return toResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        // Same message for unknown email and wrong password, so emails can't be enumerated
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .filter(u -> passwordEncoder.matches(request.getPassword(), u.getPassword()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(
                jwtService.generateToken(user),
                "Bearer",
                jwtService.getExpirationMs(),
                user.getId(),
                user.getRole()
        );
    }
}
