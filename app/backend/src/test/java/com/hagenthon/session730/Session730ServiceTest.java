package com.hagenthon.session730;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hagenthon.document.UploadedDocument;
import com.hagenthon.document.UploadedDocumentRepository;
import com.hagenthon.pdf.DocumentDataExtractor;
import com.hagenthon.pdf.PdfAnalyzerService;
import com.hagenthon.session730.Session730.SessionStatus;
import com.hagenthon.session730.dto.*;
import com.hagenthon.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Session730Service")
class Session730ServiceTest {

    @Mock private Session730Repository sessionRepository;
    @Mock private UploadedDocumentRepository documentRepository;
    @Mock private PdfAnalyzerService pdfAnalyzerService;
    @Mock private DocumentDataExtractor documentDataExtractor;

    private Session730Service session730Service;

    @BeforeEach
    void setUp() {
        ObjectMapper realObjectMapper = new ObjectMapper();
        session730Service = new Session730Service(
                sessionRepository, documentRepository, pdfAnalyzerService, documentDataExtractor, realObjectMapper);
        ReflectionTestUtils.setField(session730Service, "uploadDir",
                System.getProperty("java.io.tmpdir") + "/hagenthon-test");
    }

    // ─── uploadPdf ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Session730Service - uploadPdf - file valido crea sessione e restituisce response")
    void uploadPdf_validFile_createsSessionAndReturnsResponse() throws IOException {
        // given
        User user = buildUser();
        MockMultipartFile file = new MockMultipartFile(
                "file", "modello730.pdf", "application/pdf", "PDF content".getBytes());
        when(pdfAnalyzerService.extractText(any())).thenReturn("testo estratto dal pdf");
        when(documentDataExtractor.analyzePdf730(anyString())).thenReturn(Map.of("PENSIONE_INPS", "1000.00"));
        when(sessionRepository.save(any())).thenAnswer(inv -> {
            Session730 s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        // when
        UploadPdfResponse response = session730Service.uploadPdf(user, file);

        // then
        assertThat(response).isNotNull();
        assertThat(response.sessionId()).isNotNull();
        assertThat(response.currentStepIndex()).isGreaterThanOrEqualTo(0);
        assertThat(response.currentStep()).isNotBlank();
        verify(sessionRepository, atLeastOnce()).save(any());
    }

    // ─── getCurrentStep ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Session730Service - getCurrentStep - sessione valida al primo step restituisce StepResponse")
    void getCurrentStep_validSession_returnsStepResponse() {
        // given — step 0 = NOME_COGNOME (MANUAL_ENTRY), nessun documento da cercare
        User user = buildUser();
        UUID sessionId = UUID.randomUUID();
        Session730 session = buildSession(user, 0, SessionStatus.IN_PROGRESS);
        when(sessionRepository.findByIdAndUser(sessionId, user)).thenReturn(Optional.of(session));

        // when
        StepResponse response = session730Service.getCurrentStep(user, sessionId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.isCompleted()).isFalse();
        assertThat(response.stepIndex()).isEqualTo(0);
        assertThat(response.stepName()).isEqualTo("Nome e Cognome");
        assertThat(response.stepType()).isEqualTo("MANUAL_ENTRY");
        assertThat(response.alreadyUploaded()).isFalse();
        // Nessuna interazione col repository documenti per step MANUAL_ENTRY
        verifyNoInteractions(documentRepository);
    }

    @Test
    @DisplayName("Session730Service - getCurrentStep - sessione non trovata lancia IllegalArgumentException")
    void getCurrentStep_sessionNotFound_throwsIllegalArgumentException() {
        // given
        User user = buildUser();
        UUID sessionId = UUID.randomUUID();
        when(sessionRepository.findByIdAndUser(sessionId, user)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> session730Service.getCurrentStep(user, sessionId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sessione non trovata");
    }

    @Test
    @DisplayName("Session730Service - getCurrentStep - sessione completata restituisce isCompleted true")
    void getCurrentStep_completedSession_returnsCompletedResponse() {
        // given
        User user = buildUser();
        UUID sessionId = UUID.randomUUID();
        Session730 session = buildSession(user, TrecentoStep.totalSteps(), SessionStatus.COMPLETED);
        when(sessionRepository.findByIdAndUser(sessionId, user)).thenReturn(Optional.of(session));

        // when
        StepResponse response = session730Service.getCurrentStep(user, sessionId);

        // then
        assertThat(response.isCompleted()).isTrue();
        assertThat(response.stepName()).isEqualTo("Completato");
    }

    @Test
    @DisplayName("Session730Service - getCurrentStep - documento già caricato per step DOCUMENT_UPLOAD restituisce alreadyUploaded true")
    void getCurrentStep_documentAlreadyUploaded_returnsAlreadyUploadedTrue() {
        // given — step 6 = PENSIONE_INPS (DOCUMENT_UPLOAD)
        User user = buildUser();
        UUID sessionId = UUID.randomUUID();
        Session730 session = buildSession(user, 6, SessionStatus.IN_PROGRESS);
        UploadedDocument doc = new UploadedDocument();
        doc.setExtractedValue("1000.00");
        when(sessionRepository.findByIdAndUser(sessionId, user)).thenReturn(Optional.of(session));
        when(documentRepository.findTopByUserAndDocTypeOrderByUploadedAtDesc(any(), any()))
                .thenReturn(Optional.of(doc));

        // when
        StepResponse response = session730Service.getCurrentStep(user, sessionId);

        // then
        assertThat(response.alreadyUploaded()).isTrue();
        assertThat(response.previewValue()).isEqualTo("1000.00");
        assertThat(response.stepType()).isEqualTo("DOCUMENT_UPLOAD");
    }

    // ─── submitManualValue ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Session730Service - submitManualValue - valori coincidenti restituisce OK")
    void submitManualValue_valoriCoincidenti_restituisceOk() {
        // given — step 0 = NOME_COGNOME; nel 730 c'è "MARIO ROSSI"
        User user = buildUser();
        UUID sessionId = UUID.randomUUID();
        Session730 session = buildSession(user, 0, SessionStatus.IN_PROGRESS);
        session.setStepsData("{\"NOME_COGNOME\":\"MARIO ROSSI\"}");
        when(sessionRepository.findByIdAndUser(sessionId, user)).thenReturn(Optional.of(session));

        // when
        Map<String, Object> result = session730Service.submitManualValue(user, sessionId, "Mario Rossi");

        // then
        assertThat(result.get("comparison")).isEqualTo("OK");
        assertThat(result.get("value730")).isEqualTo("MARIO ROSSI");
        assertThat(result.get("valueUser")).isEqualTo("Mario Rossi");
    }

    @Test
    @DisplayName("Session730Service - submitManualValue - valori divergenti restituisce MISMATCH")
    void submitManualValue_valoriDivergenti_restituisceMismatch() {
        // given
        User user = buildUser();
        UUID sessionId = UUID.randomUUID();
        Session730 session = buildSession(user, 0, SessionStatus.IN_PROGRESS);
        session.setStepsData("{\"NOME_COGNOME\":\"MARIO BIANCHI\"}");
        when(sessionRepository.findByIdAndUser(sessionId, user)).thenReturn(Optional.of(session));

        // when
        Map<String, Object> result = session730Service.submitManualValue(user, sessionId, "Mario Rossi");

        // then
        assertThat(result.get("comparison")).isEqualTo("MISMATCH");
    }

    @Test
    @DisplayName("Session730Service - submitManualValue - valore730 assente restituisce PENDING")
    void submitManualValue_valore730Assente_restituiscePending() {
        // given — stepsData vuoto, nessun valore 730 per NOME_COGNOME
        User user = buildUser();
        UUID sessionId = UUID.randomUUID();
        Session730 session = buildSession(user, 0, SessionStatus.IN_PROGRESS);
        when(sessionRepository.findByIdAndUser(sessionId, user)).thenReturn(Optional.of(session));

        // when
        Map<String, Object> result = session730Service.submitManualValue(user, sessionId, "Mario Rossi");

        // then
        assertThat(result.get("comparison")).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("Session730Service - submitManualValue - step DOCUMENT_UPLOAD lancia IllegalArgumentException")
    void submitManualValue_stepDocumentUpload_throwsIllegalArgumentException() {
        // given — step 6 = PENSIONE_INPS (DOCUMENT_UPLOAD)
        User user = buildUser();
        UUID sessionId = UUID.randomUUID();
        Session730 session = buildSession(user, 6, SessionStatus.IN_PROGRESS);
        when(sessionRepository.findByIdAndUser(sessionId, user)).thenReturn(Optional.of(session));

        // when / then
        assertThatThrownBy(() -> session730Service.submitManualValue(user, sessionId, "valore"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inserimento manuale");
    }

    // ─── uploadDocument ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Session730Service - uploadDocument - file valido restituisce extractedValue")
    void uploadDocument_validFile_returnsExtractedValue() throws IOException {
        // given — step 6 = PENSIONE_INPS (DOCUMENT_UPLOAD)
        User user = buildUser();
        UUID sessionId = UUID.randomUUID();
        Session730 session = buildSession(user, 6, SessionStatus.IN_PROGRESS);
        MockMultipartFile file = new MockMultipartFile(
                "file", "cu.pdf", "application/pdf", "Contenuto documento CU".getBytes());
        when(sessionRepository.findByIdAndUser(sessionId, user)).thenReturn(Optional.of(session));
        when(pdfAnalyzerService.extractText(any())).thenReturn("testo del documento");
        when(documentDataExtractor.extractValueFromDocument(anyString(), anyString(), anyString()))
                .thenReturn("1500.00");
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        Map<String, Object> result = session730Service.uploadDocument(user, sessionId, file);

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("extractedValue");
        assertThat(result.get("extractedValue")).isEqualTo("1500.00");
        assertThat(result).containsKey("comparison");
        verify(documentRepository).save(any(UploadedDocument.class));
    }

    @Test
    @DisplayName("Session730Service - uploadDocument - step MANUAL_ENTRY lancia IllegalArgumentException")
    void uploadDocument_stepManualEntry_throwsIllegalArgumentException() throws IOException {
        // given — step 0 = NOME_COGNOME (MANUAL_ENTRY)
        User user = buildUser();
        UUID sessionId = UUID.randomUUID();
        Session730 session = buildSession(user, 0, SessionStatus.IN_PROGRESS);
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "content".getBytes());
        when(sessionRepository.findByIdAndUser(sessionId, user)).thenReturn(Optional.of(session));

        // when / then
        assertThatThrownBy(() -> session730Service.uploadDocument(user, sessionId, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("documento");
    }

    @Test
    @DisplayName("Session730Service - uploadDocument - step già completati lancia IllegalArgumentException")
    void uploadDocument_allStepsCompleted_throwsIllegalArgumentException() throws IOException {
        // given
        User user = buildUser();
        UUID sessionId = UUID.randomUUID();
        Session730 session = buildSession(user, TrecentoStep.totalSteps(), SessionStatus.COMPLETED);
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "content".getBytes());
        when(sessionRepository.findByIdAndUser(sessionId, user)).thenReturn(Optional.of(session));

        // when / then
        assertThatThrownBy(() -> session730Service.uploadDocument(user, sessionId, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("già completati");
    }

    // ─── confirmStep ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Session730Service - confirmStep - valore confermato avanza al prossimo step")
    void confirmStep_validValue_advancesToNextStep() {
        // given — step 0 = NOME_COGNOME, il successivo è DATA_NASCITA (step 1)
        User user = buildUser();
        UUID sessionId = UUID.randomUUID();
        Session730 session = buildSession(user, 0, SessionStatus.IN_PROGRESS);
        when(sessionRepository.findByIdAndUser(sessionId, user)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        Map<String, Object> result = session730Service.confirmStep(user, sessionId, "Mario Rossi");

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("isCompleted");
        assertThat(result.get("isCompleted")).isEqualTo(false);
        assertThat(result.get("nextStep")).isEqualTo("DATA_NASCITA");
    }

    @Test
    @DisplayName("Session730Service - confirmStep - step già completati lancia IllegalArgumentException")
    void confirmStep_allStepsCompleted_throwsIllegalArgumentException() {
        // given
        User user = buildUser();
        UUID sessionId = UUID.randomUUID();
        Session730 session = buildSession(user, TrecentoStep.totalSteps(), SessionStatus.COMPLETED);
        when(sessionRepository.findByIdAndUser(sessionId, user)).thenReturn(Optional.of(session));

        // when / then
        assertThatThrownBy(() -> session730Service.confirmStep(user, sessionId, "value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("già completati");
    }

    @Test
    @DisplayName("Session730Service - confirmStep - conferma ultimo step manuale segna sessione completata")
    void confirmStep_lastManualStep_marksSessionCompleted() {
        // given — step 10 (DETRAZIONE_FAMILIARI), il successivo (11=ADDIZIONALE) è automatico
        User user = buildUser();
        UUID sessionId = UUID.randomUUID();
        Session730 session = buildSession(user, 10, SessionStatus.IN_PROGRESS);
        when(sessionRepository.findByIdAndUser(sessionId, user)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(documentRepository.findTopByUserAndDocTypeOrderByUploadedAtDesc(any(), any()))
                .thenReturn(Optional.empty());

        // when
        Map<String, Object> result = session730Service.confirmStep(user, sessionId, "Figlio Mario");

        // then
        assertThat(result).containsEntry("isCompleted", true);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
    }

    // ─── submit ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Session730Service - submit - sessione valida restituisce SubmitResponse con downloadUrl")
    void submit_validSession_returnsSubmitResponseWithDownloadUrl() {
        // given
        User user = buildUser();
        UUID sessionId = UUID.randomUUID();
        Session730 session = buildSession(user, 0, SessionStatus.IN_PROGRESS);
        when(sessionRepository.findByIdAndUser(sessionId, user)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        SubmitResponse response = session730Service.submit(user, sessionId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.downloadUrl()).contains(sessionId.toString());
        assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        verify(sessionRepository).save(session);
    }

    @Test
    @DisplayName("Session730Service - submit - sessione non trovata lancia IllegalArgumentException")
    void submit_sessionNotFound_throwsIllegalArgumentException() {
        // given
        User user = buildUser();
        UUID sessionId = UUID.randomUUID();
        when(sessionRepository.findByIdAndUser(sessionId, user)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> session730Service.submit(user, sessionId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sessione non trovata");
    }

    // ─── Regression ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Session730Service - listSessions - utente senza sessioni restituisce lista vuota senza eccezioni")
    void listSessions_userWithNoSessions_returnsEmptyListWithoutException() {
        // given - regressione: DashboardPage mostrava errore quando la lista era vuota
        User user = buildUser();
        when(sessionRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(Collections.emptyList());

        // when
        var result = session730Service.listSessions(user);

        // then
        assertThat(result).isNotNull().isEmpty();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private User buildUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("mario@test.com");
        user.setNome("Mario Rossi");
        user.setPassword("encoded-pw");
        return user;
    }

    private Session730 buildSession(User user, int stepIndex, SessionStatus status) {
        Session730 session = new Session730();
        session.setId(UUID.randomUUID());
        session.setUser(user);
        session.setCurrentStepIndex(stepIndex);
        session.setStatus(status);
        session.setStepsData("{}");
        return session;
    }
}
