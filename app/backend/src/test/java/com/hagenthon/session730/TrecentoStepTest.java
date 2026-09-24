package com.hagenthon.session730;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.hagenthon.session730.TrecentoStep.StepType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test unitari per l'enum {@link TrecentoStep}.
 * Verificano la struttura degli step, il tipo (StepType),
 * la navigazione per indice e i metodi di utilità.
 */
@DisplayName("TrecentoStep")
class TrecentoStepTest {

    // ─── totalSteps ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("totalSteps - restituisce il numero corretto di step (12)")
    void totalSteps_restituisceNumeroCorretto() {
        assertThat(TrecentoStep.totalSteps()).isEqualTo(12);
    }

    // ─── byIndex ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("byIndex - indice 0 restituisce NOME_COGNOME")
    void byIndex_indice0_restituisceNomeCognome() {
        assertThat(TrecentoStep.byIndex(0)).isEqualTo(TrecentoStep.NOME_COGNOME);
    }

    @Test
    @DisplayName("byIndex - indice 6 restituisce PENSIONE_INPS")
    void byIndex_indice6_restituiscePensioneInps() {
        assertThat(TrecentoStep.byIndex(6)).isEqualTo(TrecentoStep.PENSIONE_INPS);
    }

    @Test
    @DisplayName("byIndex - indice 11 restituisce ADDIZIONALE")
    void byIndex_indice11_restituisceAddizionale() {
        assertThat(TrecentoStep.byIndex(11)).isEqualTo(TrecentoStep.ADDIZIONALE);
    }

    @Test
    @DisplayName("byIndex - indice non valido lancia IllegalArgumentException")
    void byIndex_indiceNonValido_lanciaIllegalArgumentException() {
        assertThatThrownBy(() -> TrecentoStep.byIndex(99))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Step non trovato per indice: 99");
    }

    @Test
    @DisplayName("byIndex - indice negativo lancia IllegalArgumentException")
    void byIndex_indiceNegativo_lanciaIllegalArgumentException() {
        assertThatThrownBy(() -> TrecentoStep.byIndex(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ─── StepType ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("NOME_COGNOME - stepType è MANUAL_ENTRY")
    void nomeCognome_stepTypeManualEntry() {
        assertThat(TrecentoStep.NOME_COGNOME.getStepType()).isEqualTo(MANUAL_ENTRY);
    }

    @Test
    @DisplayName("DATA_NASCITA - stepType è MANUAL_ENTRY")
    void dataNascita_stepTypeManualEntry() {
        assertThat(TrecentoStep.DATA_NASCITA.getStepType()).isEqualTo(MANUAL_ENTRY);
    }

    @Test
    @DisplayName("CODICE_FISCALE - stepType è MANUAL_ENTRY")
    void codiceFiscale_stepTypeManualEntry() {
        assertThat(TrecentoStep.CODICE_FISCALE.getStepType()).isEqualTo(MANUAL_ENTRY);
    }

    @Test
    @DisplayName("SESSO - stepType è MANUAL_ENTRY")
    void sesso_stepTypeManualEntry() {
        assertThat(TrecentoStep.SESSO.getStepType()).isEqualTo(MANUAL_ENTRY);
    }

    @Test
    @DisplayName("COMUNE_NASCITA - stepType è MANUAL_ENTRY")
    void comuneNascita_stepTypeManualEntry() {
        assertThat(TrecentoStep.COMUNE_NASCITA.getStepType()).isEqualTo(MANUAL_ENTRY);
    }

    @Test
    @DisplayName("DOMICILIO - stepType è MANUAL_ENTRY")
    void domicilio_stepTypeManualEntry() {
        assertThat(TrecentoStep.DOMICILIO.getStepType()).isEqualTo(MANUAL_ENTRY);
    }

    @Test
    @DisplayName("PENSIONE_INPS - stepType è DOCUMENT_UPLOAD")
    void pensioneInps_stepTypeDocumentUpload() {
        assertThat(TrecentoStep.PENSIONE_INPS.getStepType()).isEqualTo(DOCUMENT_UPLOAD);
    }

    @Test
    @DisplayName("REDDITO_LAVORO_DIPENDENTE - stepType è DOCUMENT_UPLOAD")
    void redditoLavoroDipendente_stepTypeDocumentUpload() {
        assertThat(TrecentoStep.REDDITO_LAVORO_DIPENDENTE.getStepType()).isEqualTo(DOCUMENT_UPLOAD);
    }

    @Test
    @DisplayName("ADDIZIONALE - stepType è AUTOMATIC")
    void addizionale_stepTypeAutomatic() {
        assertThat(TrecentoStep.ADDIZIONALE.getStepType()).isEqualTo(AUTOMATIC);
    }

    // ─── isAutomatic ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("isAutomatic - ADDIZIONALE restituisce true")
    void isAutomatic_addizionale_restituisceTrue() {
        assertThat(TrecentoStep.ADDIZIONALE.isAutomatic()).isTrue();
    }

    @Test
    @DisplayName("isAutomatic - NOME_COGNOME restituisce false")
    void isAutomatic_nomeCognome_restituisceFalse() {
        assertThat(TrecentoStep.NOME_COGNOME.isAutomatic()).isFalse();
    }

    @Test
    @DisplayName("isAutomatic - PENSIONE_INPS restituisce false")
    void isAutomatic_pensioneInps_restituisceFalse() {
        assertThat(TrecentoStep.PENSIONE_INPS.isAutomatic()).isFalse();
    }

    // ─── Conteggio per tipo ───────────────────────────────────────────────────

    @Test
    @DisplayName("conteggio step MANUAL_ENTRY - sono esattamente 6")
    void conteggioStepManualEntry_sono6() {
        long count = Arrays.stream(TrecentoStep.values())
                .filter(s -> s.getStepType() == MANUAL_ENTRY)
                .count();
        assertThat(count).isEqualTo(6);
    }

    @Test
    @DisplayName("conteggio step DOCUMENT_UPLOAD - sono esattamente 5")
    void conteggioStepDocumentUpload_sono5() {
        long count = Arrays.stream(TrecentoStep.values())
                .filter(s -> s.getStepType() == DOCUMENT_UPLOAD)
                .count();
        assertThat(count).isEqualTo(5);
    }

    @Test
    @DisplayName("conteggio step AUTOMATIC - è esattamente 1")
    void conteggioStepAutomatic_e1() {
        long count = Arrays.stream(TrecentoStep.values())
                .filter(s -> s.getStepType() == AUTOMATIC)
                .count();
        assertThat(count).isEqualTo(1);
    }

    // ─── Accessori ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("NOME_COGNOME - getIndex restituisce 0")
    void nomeCognome_getIndex_restituisce0() {
        assertThat(TrecentoStep.NOME_COGNOME.getIndex()).isEqualTo(0);
    }

    @Test
    @DisplayName("ADDIZIONALE - getIndex restituisce 11")
    void addizionale_getIndex_restituisce11() {
        assertThat(TrecentoStep.ADDIZIONALE.getIndex()).isEqualTo(11);
    }

    @Test
    @DisplayName("PENSIONE_INPS - getDocumentRequired non è null per step DOCUMENT_UPLOAD")
    void pensioneInps_getDocumentRequired_nonNull() {
        assertThat(TrecentoStep.PENSIONE_INPS.getDocumentRequired()).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("NOME_COGNOME - getDocumentRequired è null per step MANUAL_ENTRY")
    void nomeCognome_getDocumentRequired_null() {
        assertThat(TrecentoStep.NOME_COGNOME.getDocumentRequired()).isNull();
    }

    @Test
    @DisplayName("tutti gli step - getDisplayName non è mai blank")
    void tuttiGliStep_getDisplayName_maiBlank() {
        List<String> nomiBlank = Arrays.stream(TrecentoStep.values())
                .filter(s -> s.getDisplayName() == null || s.getDisplayName().isBlank())
                .map(Enum::name)
                .toList();
        assertThat(nomiBlank).isEmpty();
    }

    @Test
    @DisplayName("tutti gli step - getDescription non è mai blank")
    void tuttiGliStep_getDescription_maiBlank() {
        List<String> descBlank = Arrays.stream(TrecentoStep.values())
                .filter(s -> s.getDescription() == null || s.getDescription().isBlank())
                .map(Enum::name)
                .toList();
        assertThat(descBlank).isEmpty();
    }

    @Test
    @DisplayName("tutti gli step - getIndex è univoco")
    void tuttiGliStep_getIndex_univoco() {
        long distinctCount = Arrays.stream(TrecentoStep.values())
                .mapToInt(TrecentoStep::getIndex)
                .distinct()
                .count();
        assertThat(distinctCount).isEqualTo(TrecentoStep.totalSteps());
    }
}
