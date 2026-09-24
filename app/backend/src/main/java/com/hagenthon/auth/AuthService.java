package com.hagenthon.auth;

import com.hagenthon.auth.dto.*;
import com.hagenthon.user.PasswordResetToken;
import com.hagenthon.user.PasswordResetTokenRepository;
import com.hagenthon.user.User;
import com.hagenthon.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String mailFrom;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Transactional
    public LoginResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email già registrata");
        }
        User user = new User();
        user.setEmail(req.email());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setNome(req.nome());
        userRepository.save(user);
        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new LoginResponse(token, user.getEmail(), user.getNome());
    }

    public LoginResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new IllegalArgumentException("Credenziali non valide"));
        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new IllegalArgumentException("Credenziali non valide");
        }
        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new LoginResponse(token, user.getEmail(), user.getNome());
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest req) {
        userRepository.findByEmail(req.email()).ifPresent(user -> {
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUser(user);
            resetToken.setToken(UUID.randomUUID().toString());
            resetToken.setExpiresAt(LocalDateTime.now().plusHours(1));
            tokenRepository.save(resetToken);
            sendResetEmail(user.getEmail(), resetToken.getToken());
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        PasswordResetToken resetToken = tokenRepository.findByToken(req.token())
                .orElseThrow(() -> new IllegalArgumentException("Token non valido"));
        if (resetToken.isUsed()) {
            throw new IllegalArgumentException("Token già utilizzato");
        }
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token scaduto");
        }
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }

    private void sendResetEmail(String to, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(to);
            message.setSubject("Reset password - 730 Facile");
            message.setText("""
                    Ciao,

                    Hai richiesto il reset della password per il tuo account 730 Facile.
                    Clicca il link seguente per reimpostare la password:

                    %s/reset-password?token=%s

                    Il link scade tra 1 ora. Se non hai richiesto il reset, ignora questa email.

                    Il team di 730 Facile
                    """.formatted(frontendUrl, token));
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Impossibile inviare email di reset a {}: {}", to, e.getMessage());
        }
    }
}
