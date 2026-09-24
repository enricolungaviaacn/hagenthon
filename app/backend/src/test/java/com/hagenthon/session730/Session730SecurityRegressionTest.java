package com.hagenthon.session730;

import com.hagenthon.config.JwtAuthFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Regression tests per la sicurezza degli endpoint /api/sessions.
 * Verifica che le risorse protette non siano accessibili senza autenticazione,
 * e che un utente autenticato con zero sessioni non riceva errori.
 */
@WebMvcTest(Session730Controller.class)
@DisplayName("Session730Controller - sicurezza")
class Session730SecurityRegressionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private Session730Service session730Service;

    @MockBean
    private SummaryGeneratorService summaryGeneratorService;

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

    @Test
    @DisplayName("Session730Controller - listSessions - richiesta senza token restituisce 401")
    void listSessions_withoutToken_returns401() throws Exception {
        // given - nessun token JWT nella richiesta

        // when / then - la sicurezza predefinita deve bloccare la richiesta non autenticata
        mockMvc.perform(get("/api/sessions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Session730Controller - listSessions - utente con zero sessioni non genera errori")
    void listSessions_userWithZeroSessions_returns200EmptyList() throws Exception {
        // given - regressione: DashboardPage mostrava "Impossibile caricare le sessioni"
        // quando il backend restituiva una lista vuota. Verifichiamo che il servizio
        // non lanci eccezioni e l'endpoint risponda correttamente.
        com.hagenthon.user.User user = new com.hagenthon.user.User();
        user.setId(UUID.randomUUID());
        user.setEmail("nuovo@test.com");
        user.setNome("Nuovo Utente");
        user.setPassword("encoded-pw");

        when(session730Service.listSessions(any())).thenReturn(Collections.emptyList());

        // when / then
        mockMvc.perform(get("/api/sessions")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                user, null, Collections.emptyList()))))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(session730Service).listSessions(any());
    }
}
