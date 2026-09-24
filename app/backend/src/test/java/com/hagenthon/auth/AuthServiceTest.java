package com.hagenthon.auth;

import com.hagenthon.auth.dto.*;
import com.hagenthon.user.PasswordResetToken;
import com.hagenthon.user.PasswordResetTokenRepository;
import com.hagenthon.user.User;
import com.hagenthon.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JavaMailSender mailSender;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "mailFrom", "noreply@test.com");
        ReflectionTestUtils.setField(authService, "frontendUrl", "http://localhost:5173");
    }

    // ─── register ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("AuthService - register - email nuova crea utente e restituisce LoginResponse")
    void register_newEmail_returnsLoginResponse() {
        // given
        RegisterRequest req = new RegisterRequest("mario@test.com", "password123", "Mario");
        when(userRepository.existsByEmail("mario@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-pw");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(jwtService.generateToken(any(UUID.class), eq("mario@test.com"))).thenReturn("jwt-token");

        // when
        LoginResponse response = authService.register(req);

        // then
        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.email()).isEqualTo("mario@test.com");
        assertThat(response.nome()).isEqualTo("Mario");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("AuthService - register - email già registrata lancia IllegalArgumentException")
    void register_duplicateEmail_throwsIllegalArgumentException() {
        // given
        RegisterRequest req = new RegisterRequest("mario@test.com", "password123", "Mario");
        when(userRepository.existsByEmail("mario@test.com")).thenReturn(true);

        // when / then
        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("già registrata");
        verify(userRepository, never()).save(any());
    }

    // ─── login ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("AuthService - login - credenziali corrette restituisce LoginResponse")
    void login_validCredentials_returnsLoginResponse() {
        // given
        LoginRequest req = new LoginRequest("mario@test.com", "password123");
        User user = buildUser("mario@test.com", "encoded-pw", "Mario");
        when(userRepository.findByEmail("mario@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded-pw")).thenReturn(true);
        when(jwtService.generateToken(user.getId(), "mario@test.com")).thenReturn("jwt-token");

        // when
        LoginResponse response = authService.login(req);

        // then
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.email()).isEqualTo("mario@test.com");
        assertThat(response.nome()).isEqualTo("Mario");
    }

    @Test
    @DisplayName("AuthService - login - email non trovata lancia IllegalArgumentException")
    void login_emailNotFound_throwsIllegalArgumentException() {
        // given
        LoginRequest req = new LoginRequest("ghost@test.com", "password123");
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Credenziali non valide");
    }

    @Test
    @DisplayName("AuthService - login - password errata lancia IllegalArgumentException")
    void login_wrongPassword_throwsIllegalArgumentException() {
        // given
        LoginRequest req = new LoginRequest("mario@test.com", "wrong-password");
        User user = buildUser("mario@test.com", "encoded-pw", "Mario");
        when(userRepository.findByEmail("mario@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-pw")).thenReturn(false);

        // when / then
        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Credenziali non valide");
    }

    // ─── forgotPassword ──────────────────────────────────────────────────────

    @Test
    @DisplayName("AuthService - forgotPassword - email esistente salva token e invia email")
    void forgotPassword_existingEmail_savesTokenAndSendsEmail() {
        // given
        ForgotPasswordRequest req = new ForgotPasswordRequest("mario@test.com");
        User user = buildUser("mario@test.com", "encoded-pw", "Mario");
        when(userRepository.findByEmail("mario@test.com")).thenReturn(Optional.of(user));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        authService.forgotPassword(req);

        // then
        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("AuthService - forgotPassword - email non esistente non fa nulla")
    void forgotPassword_nonExistingEmail_doesNothing() {
        // given
        ForgotPasswordRequest req = new ForgotPasswordRequest("ghost@test.com");
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        // when
        authService.forgotPassword(req);

        // then
        verify(tokenRepository, never()).save(any());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    // ─── resetPassword ───────────────────────────────────────────────────────

    @Test
    @DisplayName("AuthService - resetPassword - token valido aggiorna la password")
    void resetPassword_validToken_updatesPassword() {
        // given
        ResetPasswordRequest req = new ResetPasswordRequest("valid-token", "newPass123");
        User user = buildUser("mario@test.com", "old-encoded", "Mario");
        PasswordResetToken resetToken = buildToken(user, "valid-token", false,
                LocalDateTime.now().plusHours(1));
        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("newPass123")).thenReturn("new-encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        authService.resetPassword(req);

        // then
        assertThat(user.getPassword()).isEqualTo("new-encoded");
        assertThat(resetToken.isUsed()).isTrue();
        verify(userRepository).save(user);
        verify(tokenRepository).save(resetToken);
    }

    @Test
    @DisplayName("AuthService - resetPassword - token non trovato lancia IllegalArgumentException")
    void resetPassword_tokenNotFound_throwsIllegalArgumentException() {
        // given
        ResetPasswordRequest req = new ResetPasswordRequest("ghost-token", "newPass123");
        when(tokenRepository.findByToken("ghost-token")).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> authService.resetPassword(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Token non valido");
    }

    @Test
    @DisplayName("AuthService - resetPassword - token già usato lancia IllegalArgumentException")
    void resetPassword_usedToken_throwsIllegalArgumentException() {
        // given
        ResetPasswordRequest req = new ResetPasswordRequest("used-token", "newPass123");
        User user = buildUser("mario@test.com", "old-encoded", "Mario");
        PasswordResetToken resetToken = buildToken(user, "used-token", true,
                LocalDateTime.now().plusHours(1));
        when(tokenRepository.findByToken("used-token")).thenReturn(Optional.of(resetToken));

        // when / then
        assertThatThrownBy(() -> authService.resetPassword(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("già utilizzato");
    }

    @Test
    @DisplayName("AuthService - resetPassword - token scaduto lancia IllegalArgumentException")
    void resetPassword_expiredToken_throwsIllegalArgumentException() {
        // given
        ResetPasswordRequest req = new ResetPasswordRequest("expired-token", "newPass123");
        User user = buildUser("mario@test.com", "old-encoded", "Mario");
        PasswordResetToken resetToken = buildToken(user, "expired-token", false,
                LocalDateTime.now().minusHours(2));
        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(resetToken));

        // when / then
        assertThatThrownBy(() -> authService.resetPassword(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scaduto");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private User buildUser(String email, String encodedPassword, String nome) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setPassword(encodedPassword);
        user.setNome(nome);
        return user;
    }

    private PasswordResetToken buildToken(User user, String tokenValue, boolean used,
                                          LocalDateTime expiresAt) {
        PasswordResetToken t = new PasswordResetToken();
        t.setId(UUID.randomUUID());
        t.setUser(user);
        t.setToken(tokenValue);
        t.setUsed(used);
        t.setExpiresAt(expiresAt);
        return t;
    }
}
