package com.ecommerce.auth.controller;

import com.ecommerce.auth.dto.*;
import com.ecommerce.auth.model.Session;
import com.ecommerce.auth.model.User;
import com.ecommerce.auth.repository.SessionRepository;
import com.ecommerce.auth.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;

    public AuthController(UserRepository userRepository, SessionRepository sessionRepository) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        Optional<User> userOpt = userRepository.findByUsernameAndPassword(req.username(), req.password());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid credentials"));
        }

        User user = userOpt.get();
        String token = UUID.randomUUID().toString();
        String createdAt = LocalDateTime.now().toString();

        Session session = new Session(token, user.getId(), createdAt);
        sessionRepository.save(session);

        return ResponseEntity.ok(new LoginResponse(token, user.getId(), user.getUsername()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody LogoutRequest req) {
        if (req.token() != null && sessionRepository.existsById(req.token())) {
            sessionRepository.deleteById(req.token());
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe(@RequestHeader(value = "token", required = false) String token) {
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Missing token"));
        }

        Optional<Session> sessionOpt = sessionRepository.findById(token);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
        }

        Session session = sessionOpt.get();
        Optional<User> userOpt = userRepository.findById(session.getUserId());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
        }

        User user = userOpt.get();
        return ResponseEntity.ok(new UserInfoResponse(user.getId(), user.getUsername(), user.getWalletBalance()));
    }
}
