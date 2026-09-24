package com.hagenthon.auth;

import com.hagenthon.auth.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest req) {
        log.info("AuthController.register: tentativo registrazione per email={}", req.email());
        LoginResponse response = authService.register(req);
        log.info("AuthController.register: utente registrato con successo email={}", req.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        log.info("AuthController.login: tentativo login per email={}", req.email());
        LoginResponse response = authService.login(req);
        log.info("AuthController.login: login riuscito per email={}", req.email());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        log.info("AuthController.forgotPassword: richiesta reset password per email={}", req.email());
        authService.forgotPassword(req);
        return ResponseEntity.ok(Map.of("message",
                "Se l'email è registrata, riceverai un link per il reset della password"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        log.info("AuthController.resetPassword: tentativo reset password con token");
        authService.resetPassword(req);
        log.info("AuthController.resetPassword: password reimpostata con successo");
        return ResponseEntity.ok(Map.of("message", "Password reimpostata con successo"));
    }
}
