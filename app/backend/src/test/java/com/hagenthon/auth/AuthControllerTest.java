package com.hagenthon.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hagenthon.auth.dto.*;
import com.hagenthon.config.JwtAuthFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(AuthControllerTest.TestSecurityConfig.class)
@DisplayName("AuthController")
class AuthControllerTest {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void configureFilterPassThrough() throws Exception {
        doAnswer(inv -> {
            FilterChain chain = inv.getArgument(2);
            chain.doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

    // ─── POST /api/auth/register ──────────────────────────────────────────────

    @Test
    @DisplayName("AuthController - register - richiesta valida restituisce 201 con token")
    void register_validRequest_returns201WithToken() throws Exception {
        // given
        RegisterRequest req = new RegisterRequest("mario@test.com", "password123", "Mario");
        LoginResponse resp = new LoginResponse("jwt-token", "mario@test.com", "Mario");
        when(authService.register(any(RegisterRequest.class))).thenReturn(resp);

        // when / then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.email").value("mario@test.com"))
                .andExpect(jsonPath("$.nome").value("Mario"));
    }

    @Test
    @DisplayName("AuthController - register - email non valida restituisce 400")
    void register_invalidEmail_returns400() throws Exception {
        // given
        RegisterRequest req = new RegisterRequest("non-una-email", "password123", "Mario");

        // when / then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
        verify(authService, never()).register(any());
    }

    @Test
    @DisplayName("AuthController - register - nome vuoto restituisce 400")
    void register_emptyNome_returns400() throws Exception {
        // given
        RegisterRequest req = new RegisterRequest("mario@test.com", "password123", "");

        // when / then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
        verify(authService, never()).register(any());
    }

    @Test
    @DisplayName("AuthController - register - password troppo corta restituisce 400")
    void register_shortPassword_returns400() throws Exception {
        // given
        RegisterRequest req = new RegisterRequest("mario@test.com", "123", "Mario");

        // when / then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
        verify(authService, never()).register(any());
    }

    @Test
    @DisplayName("AuthController - register - email duplicata restituisce 400 con messaggio errore")
    void register_duplicateEmail_returns400WithErrorMessage() throws Exception {
        // given
        RegisterRequest req = new RegisterRequest("mario@test.com", "password123", "Mario");
        when(authService.register(any())).thenThrow(new IllegalArgumentException("Email già registrata"));

        // when / then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email già registrata"));
    }

    // ─── POST /api/auth/login ─────────────────────────────────────────────────

    @Test
    @DisplayName("AuthController - login - credenziali valide restituisce 200 con token")
    void login_validCredentials_returns200WithToken() throws Exception {
        // given
        LoginRequest req = new LoginRequest("mario@test.com", "password123");
        LoginResponse resp = new LoginResponse("jwt-token", "mario@test.com", "Mario");
        when(authService.login(any(LoginRequest.class))).thenReturn(resp);

        // when / then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.email").value("mario@test.com"));
    }

    @Test
    @DisplayName("AuthController - login - credenziali errate restituisce 400 con errore")
    void login_wrongCredentials_returns400WithError() throws Exception {
        // given
        LoginRequest req = new LoginRequest("mario@test.com", "wrong");
        when(authService.login(any())).thenThrow(new IllegalArgumentException("Credenziali non valide"));

        // when / then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Credenziali non valide"));
    }

    @Test
    @DisplayName("AuthController - login - email mancante nel body restituisce 400")
    void login_missingEmail_returns400() throws Exception {
        // given - email null non supera @Email @NotBlank
        String body = "{\"password\":\"password123\"}";

        // when / then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
        verify(authService, never()).login(any());
    }

    // ─── POST /api/auth/forgot-password ──────────────────────────────────────

    @Test
    @DisplayName("AuthController - forgotPassword - email valida restituisce 200 con messaggio")
    void forgotPassword_validEmail_returns200WithMessage() throws Exception {
        // given
        ForgotPasswordRequest req = new ForgotPasswordRequest("mario@test.com");
        doNothing().when(authService).forgotPassword(any());

        // when / then
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("AuthController - forgotPassword - email non valida restituisce 400")
    void forgotPassword_invalidEmail_returns400() throws Exception {
        // given
        ForgotPasswordRequest req = new ForgotPasswordRequest("non-una-email");

        // when / then
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
        verify(authService, never()).forgotPassword(any());
    }

    // ─── POST /api/auth/reset-password ───────────────────────────────────────

    @Test
    @DisplayName("AuthController - resetPassword - token valido restituisce 200 con messaggio")
    void resetPassword_validToken_returns200WithMessage() throws Exception {
        // given
        ResetPasswordRequest req = new ResetPasswordRequest("valid-token", "newPass123");
        doNothing().when(authService).resetPassword(any());

        // when / then
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reimpostata con successo"));
    }

    @Test
    @DisplayName("AuthController - resetPassword - token non valido restituisce 400 con errore")
    void resetPassword_invalidToken_returns400WithError() throws Exception {
        // given
        ResetPasswordRequest req = new ResetPasswordRequest("bad-token", "newPass123");
        doThrow(new IllegalArgumentException("Token non valido"))
                .when(authService).resetPassword(any());

        // when / then
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Token non valido"));
    }

    @Test
    @DisplayName("AuthController - resetPassword - nuova password troppo corta restituisce 400")
    void resetPassword_shortNewPassword_returns400() throws Exception {
        // given
        ResetPasswordRequest req = new ResetPasswordRequest("valid-token", "123");

        // when / then
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
        verify(authService, never()).resetPassword(any());
    }
}
