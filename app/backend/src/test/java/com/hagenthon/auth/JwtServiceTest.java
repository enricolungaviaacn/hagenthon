package com.hagenthon.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService")
class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET = "YourSuperSecretKeyForHS256ThatIsAtLeast32BytesLong!!";
    private static final long EXPIRATION_MS = 86400000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", EXPIRATION_MS);
    }

    // ─── generateToken ────────────────────────────────────────────────────────

    @Test
    @DisplayName("JwtService - generateToken - input validi producono token non nullo")
    void generateToken_validInputs_returnsNonNullToken() {
        // given
        UUID userId = UUID.randomUUID();
        String email = "test@test.com";

        // when
        String token = jwtService.generateToken(userId, email);

        // then
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("JwtService - generateToken - token contiene tre segmenti JWT")
    void generateToken_validInputs_tokenHasThreeSegments() {
        // given
        UUID userId = UUID.randomUUID();

        // when
        String token = jwtService.generateToken(userId, "user@test.com");

        // then
        assertThat(token.split("\\.")).hasSize(3);
    }

    // ─── isTokenValid ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("JwtService - isTokenValid - token valido restituisce true")
    void isTokenValid_validToken_returnsTrue() {
        // given
        String token = jwtService.generateToken(UUID.randomUUID(), "test@test.com");

        // when
        boolean result = jwtService.isTokenValid(token);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("JwtService - isTokenValid - stringa malformata restituisce false")
    void isTokenValid_malformedToken_returnsFalse() {
        // given
        String badToken = "questa.non.ejwt";

        // when
        boolean result = jwtService.isTokenValid(badToken);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("JwtService - isTokenValid - token con firma alterata restituisce false")
    void isTokenValid_tamperedSignature_returnsFalse() {
        // given
        String token = jwtService.generateToken(UUID.randomUUID(), "test@test.com");
        int lastDot = token.lastIndexOf('.');
        String tampered = token.substring(0, lastDot + 1) + "INVALIDSIGNATUREXXXX";

        // when
        boolean result = jwtService.isTokenValid(tampered);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("JwtService - isTokenValid - token già scaduto restituisce false")
    void isTokenValid_expiredToken_returnsFalse() {
        // given - expiration nel passato
        ReflectionTestUtils.setField(jwtService, "expiration", -1000L);
        String token = jwtService.generateToken(UUID.randomUUID(), "test@test.com");

        // when
        boolean result = jwtService.isTokenValid(token);

        // then
        assertThat(result).isFalse();
    }

    // ─── extractEmail ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("JwtService - extractEmail - restituisce l'email corretta dal token valido")
    void extractEmail_validToken_returnsEmail() {
        // given
        String email = "mario.rossi@example.com";
        String token = jwtService.generateToken(UUID.randomUUID(), email);

        // when
        String extracted = jwtService.extractEmail(token);

        // then
        assertThat(extracted).isEqualTo(email);
    }

    // ─── extractUserId ────────────────────────────────────────────────────────

    @Test
    @DisplayName("JwtService - extractUserId - restituisce l'UUID corretto dal token valido")
    void extractUserId_validToken_returnsUserId() {
        // given
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "test@test.com");

        // when
        UUID extracted = jwtService.extractUserId(token);

        // then
        assertThat(extracted).isEqualTo(userId);
    }
}
