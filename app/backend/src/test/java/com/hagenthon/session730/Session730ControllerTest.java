package com.hagenthon.session730;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hagenthon.config.JwtAuthFilter;
import com.hagenthon.session730.dto.*;
import com.hagenthon.user.User;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(Session730Controller.class)
@Import(Session730ControllerTest.TestSecurityConfig.class)
@DisplayName("Session730Controller")
class Session730ControllerTest {

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
    private Session730Service session730Service;

    @MockBean
    private SummaryGeneratorService summaryGeneratorService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    private User mockUser;
    private UUID sessionId;

    @BeforeEach
    void setUp() throws Exception {
        mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail("mario@test.com");
        mockUser.setNome("Mario Rossi");
        mockUser.setPassword("encoded-pw");
        sessionId = UUID.randomUUID();

        doAnswer(inv -> {
            FilterChain chain = inv.getArgument(2);
            chain.doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

    private RequestPostProcessor userAuth() {
        return authentication(new UsernamePasswordAuthenticationToken(
                mockUser, null, Collections.emptyList()));
    }

    // ─── GET /api/sessions ────────────────────────────────────────────────────

    @Test
    @DisplayName("Session730Controller - listSessions - utente autenticato restituisce 200 con lista")
    void listSessions_authenticatedUser_returns200WithList() throws Exception {
        // given
        when(session730Service.listSessions(any())).thenReturn(List.of());

        // when / then
        mockMvc.perform(get("/api/sessions").with(userAuth()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Session730Controller - listSessions - utente senza sessioni restituisce lista vuota")
    void listSessions_userWithNoSessions_returns200WithEmptyList() throws Exception {
        // given - regressione: lista vuota non deve causare errori
        when(session730Service.listSessions(any())).thenReturn(Collections.emptyList());

        // when / then
        mockMvc.perform(get("/api/sessions").with(userAuth()))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    // ─── GET /api/sessions/{sessionId}/current-step ───────────────────────────

    @Test
    @DisplayName("Session730Controller - getCurrentStep - sessione valida restituisce 200 con StepResponse")
    void getCurrentStep_validSession_returns200WithStepResponse() throws Exception {
        // given — step 0 = NOME_COGNOME (MANUAL_ENTRY)
        StepResponse stepResp = new StepResponse(0, "Nome e Cognome",
                "MANUAL_ENTRY", null, "Inserire nome e cognome",
                false, null, null, null, null, false);
        when(session730Service.getCurrentStep(any(), eq(sessionId))).thenReturn(stepResp);

        // when / then
        mockMvc.perform(get("/api/sessions/{id}/current-step", sessionId).with(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stepName").value("Nome e Cognome"))
                .andExpect(jsonPath("$.stepIndex").value(0))
                .andExpect(jsonPath("$.stepType").value("MANUAL_ENTRY"))
                .andExpect(jsonPath("$.isCompleted").value(false));
    }

    @Test
    @DisplayName("Session730Controller - getCurrentStep - sessione non trovata restituisce 400")
    void getCurrentStep_sessionNotFound_returns400() throws Exception {
        // given
        when(session730Service.getCurrentStep(any(), any()))
                .thenThrow(new IllegalArgumentException("Sessione non trovata o accesso negato"));

        // when / then
        mockMvc.perform(get("/api/sessions/{id}/current-step", sessionId).with(userAuth()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Sessione non trovata o accesso negato"));
    }

    // ─── POST /api/sessions/upload-730 ───────────────────────────────────────

    @Test
    @DisplayName("Session730Controller - uploadPdf - file valido restituisce 200 con sessionId")
    void uploadPdf_validFile_returns200WithSessionId() throws Exception {
        // given
        UploadPdfResponse uploadResp = new UploadPdfResponse(sessionId, "NOME_COGNOME", 0);
        when(session730Service.uploadPdf(any(), any())).thenReturn(uploadResp);
        MockMultipartFile file = new MockMultipartFile(
                "file", "modello730.pdf", "application/pdf", "contenuto".getBytes());

        // when / then
        mockMvc.perform(multipart("/api/sessions/upload-730").file(file).with(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
                .andExpect(jsonPath("$.currentStep").value("NOME_COGNOME"))
                .andExpect(jsonPath("$.currentStepIndex").value(0));
    }

    // ─── POST /api/sessions/{sessionId}/submit-manual-value ───────────────────

    @Test
    @DisplayName("Session730Controller - submitManualValue - valori coincidenti restituisce 200 con OK")
    void submitManualValue_valoriCoincidenti_returns200WithOk() throws Exception {
        // given
        ManualValueRequest req = new ManualValueRequest("Mario Rossi");
        Map<String, Object> serviceResult = Map.of(
                "value730", "MARIO ROSSI", "valueUser", "Mario Rossi", "comparison", "OK");
        when(session730Service.submitManualValue(any(), eq(sessionId), anyString())).thenReturn(serviceResult);

        // when / then
        mockMvc.perform(post("/api/sessions/{id}/submit-manual-value", sessionId)
                        .with(userAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comparison").value("OK"))
                .andExpect(jsonPath("$.value730").value("MARIO ROSSI"));
    }

    @Test
    @DisplayName("Session730Controller - submitManualValue - userValue nullo restituisce 400")
    void submitManualValue_nullUserValue_returns400() throws Exception {
        // given
        ManualValueRequest req = new ManualValueRequest(null);

        // when / then
        mockMvc.perform(post("/api/sessions/{id}/submit-manual-value", sessionId)
                        .with(userAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
        verify(session730Service, never()).submitManualValue(any(), any(), any());
    }

    @Test
    @DisplayName("Session730Controller - submitManualValue - step non manuale restituisce 400")
    void submitManualValue_stepNotManual_returns400() throws Exception {
        // given
        ManualValueRequest req = new ManualValueRequest("valore");
        when(session730Service.submitManualValue(any(), any(), anyString()))
                .thenThrow(new IllegalArgumentException("Lo step corrente non è di tipo inserimento manuale"));

        // when / then
        mockMvc.perform(post("/api/sessions/{id}/submit-manual-value", sessionId)
                        .with(userAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Lo step corrente non è di tipo inserimento manuale"));
    }

    // ─── POST /api/sessions/{sessionId}/upload-document ──────────────────────

    @Test
    @DisplayName("Session730Controller - uploadDocument - file valido restituisce 200 con extractedValue")
    void uploadDocument_validFile_returns200WithExtractedValue() throws Exception {
        // given
        Map<String, Object> serviceResult = Map.of(
                "extractedValue", "1500.00", "preview", "1500.00",
                "value730", "", "comparison", "PENDING");
        when(session730Service.uploadDocument(any(), eq(sessionId), any())).thenReturn(serviceResult);
        MockMultipartFile file = new MockMultipartFile(
                "file", "cu.pdf", "application/pdf", "contenuto".getBytes());

        // when / then
        mockMvc.perform(multipart("/api/sessions/{id}/upload-document", sessionId)
                        .file(file).with(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extractedValue").value("1500.00"))
                .andExpect(jsonPath("$.comparison").value("PENDING"));
    }

    @Test
    @DisplayName("Session730Controller - uploadDocument - step completati restituisce 400")
    void uploadDocument_allStepsCompleted_returns400() throws Exception {
        // given
        when(session730Service.uploadDocument(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Tutti gli step sono già completati"));
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "contenuto".getBytes());

        // when / then
        mockMvc.perform(multipart("/api/sessions/{id}/upload-document", sessionId)
                        .file(file).with(userAuth()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Tutti gli step sono già completati"));
    }

    // ─── POST /api/sessions/{sessionId}/confirm-step ─────────────────────────

    @Test
    @DisplayName("Session730Controller - confirmStep - valore confermato restituisce 200 con nextStep")
    void confirmStep_validRequest_returns200WithNextStep() throws Exception {
        // given — dal primo step NOME_COGNOME, il prossimo è DATA_NASCITA
        ConfirmStepRequest req = new ConfirmStepRequest("Mario Rossi");
        Map<String, Object> serviceResult = Map.of(
                "nextStep", "DATA_NASCITA", "isCompleted", false);
        when(session730Service.confirmStep(any(), eq(sessionId), anyString())).thenReturn(serviceResult);

        // when / then
        mockMvc.perform(post("/api/sessions/{id}/confirm-step", sessionId)
                        .with(userAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextStep").value("DATA_NASCITA"))
                .andExpect(jsonPath("$.isCompleted").value(false));
    }

    @Test
    @DisplayName("Session730Controller - confirmStep - confirmedValue nullo restituisce 400")
    void confirmStep_nullConfirmedValue_returns400() throws Exception {
        // given
        ConfirmStepRequest req = new ConfirmStepRequest(null);

        // when / then
        mockMvc.perform(post("/api/sessions/{id}/confirm-step", sessionId)
                        .with(userAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
        verify(session730Service, never()).confirmStep(any(), any(), any());
    }

    @Test
    @DisplayName("Session730Controller - confirmStep - sessione non trovata restituisce 400")
    void confirmStep_sessionNotFound_returns400() throws Exception {
        // given
        ConfirmStepRequest req = new ConfirmStepRequest("value");
        when(session730Service.confirmStep(any(), any(), anyString()))
                .thenThrow(new IllegalArgumentException("Sessione non trovata o accesso negato"));

        // when / then
        mockMvc.perform(post("/api/sessions/{id}/confirm-step", sessionId)
                        .with(userAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ─── POST /api/sessions/{sessionId}/submit ────────────────────────────────

    @Test
    @DisplayName("Session730Controller - submit - sessione valida restituisce 200 con downloadUrl")
    void submit_validSession_returns200WithDownloadUrl() throws Exception {
        // given
        SubmitResponse submitResp = new SubmitResponse("/api/sessions/" + sessionId + "/download");
        when(session730Service.submit(any(), eq(sessionId))).thenReturn(submitResp);

        // when / then
        mockMvc.perform(post("/api/sessions/{id}/submit", sessionId).with(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.downloadUrl").exists());
    }

    @Test
    @DisplayName("Session730Controller - submit - sessione non trovata restituisce 400")
    void submit_sessionNotFound_returns400() throws Exception {
        // given
        when(session730Service.submit(any(), any()))
                .thenThrow(new IllegalArgumentException("Sessione non trovata o accesso negato"));

        // when / then
        mockMvc.perform(post("/api/sessions/{id}/submit", sessionId).with(userAuth()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Sessione non trovata o accesso negato"));
    }

    // ─── GET /api/sessions/{sessionId}/summary ────────────────────────────────

    @Test
    @DisplayName("Session730Controller - getSummary - sessione valida restituisce 200 con riepilogo")
    void getSummary_validSession_returns200WithSummary() throws Exception {
        // given
        Map<String, Object> summary = Map.of(
                "sessionId", sessionId,
                "status", "IN_PROGRESS",
                "values", Map.of());
        when(session730Service.getSummary(any(), eq(sessionId))).thenReturn(summary);

        // when / then
        mockMvc.perform(get("/api/sessions/{id}/summary", sessionId).with(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("Session730Controller - getSummary - sessione non trovata restituisce 400")
    void getSummary_sessionNotFound_returns400() throws Exception {
        // given
        when(session730Service.getSummary(any(), any()))
                .thenThrow(new IllegalArgumentException("Sessione non trovata o accesso negato"));

        // when / then
        mockMvc.perform(get("/api/sessions/{id}/summary", sessionId).with(userAuth()))
                .andExpect(status().isBadRequest());
    }
}
