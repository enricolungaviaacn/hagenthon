package com.hagenthon.pdf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test unitari per {@link DocumentDataExtractor}.
 * Coprono l'estrazione rule-based da testi di documenti fiscali italiani
 * senza dipendenze da servizi esterni.
 */
@DisplayName("DocumentDataExtractor")
class DocumentDataExtractorTest {

    private DocumentDataExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new DocumentDataExtractor();
    }

    // ─── analyzePdf730 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("analyzePdf730 - testo CU con pensione INPS - estrae importo pensione correttamente")
    void analyzePdf730_testoCuConPensione_estraeImportoPensione() {
        // given
        String testo = """
                CERTIFICAZIONE UNICA 2024
                Redditi da pensione
                pensione INPS corrisposta nell'anno: 14.400,00 euro
                Ritenute operate: 2.880,00
                """;

        // when
        Map<String, String> result = extractor.analyzePdf730(testo);

        // then
        assertThat(result).isNotEmpty();
        assertThat(result).containsKey("PENSIONE_INPS");
        assertThat(result.get("PENSIONE_INPS")).isEqualTo("14.400,00");
    }

    @Test
    @DisplayName("analyzePdf730 - testo vuoto - restituisce mappa vuota senza eccezioni")
    void analyzePdf730_testoVuoto_restituisceMappaVuota() {
        // when
        Map<String, String> result = extractor.analyzePdf730("");

        // then
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("analyzePdf730 - testo null - restituisce mappa vuota senza eccezioni")
    void analyzePdf730_testoNull_restituisceMappaVuota() {
        // when
        Map<String, String> result = extractor.analyzePdf730(null);

        // then
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("analyzePdf730 - testo non pertinente - restituisce mappa vuota")
    void analyzePdf730_testoNonPertinente_restituisceMappaVuota() {
        // given - testo senza alcun dato fiscale riconoscibile
        String testo = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                       "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.";

        // when
        Map<String, String> result = extractor.analyzePdf730(testo);

        // then
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("analyzePdf730 - testo con reddito lavoro dipendente - estrae importo reddito")
    void analyzePdf730_testoConRedditoLavoroDipendente_estraeImportoReddito() {
        // given
        String testo = """
                MODELLO 730 PRECOMPILATO 2024
                Reddito lavoro dipendente (RC1): 28.000,00
                Ritenute IRPEF: 5.600,00
                """;

        // when
        Map<String, String> result = extractor.analyzePdf730(testo);

        // then
        assertThat(result).containsKey("REDDITO_LAVORO_DIPENDENTE");
        assertThat(result.get("REDDITO_LAVORO_DIPENDENTE")).isEqualTo("28.000,00");
    }

    @Test
    @DisplayName("analyzePdf730 - testo con interessi mutuo - estrae importo interessi")
    void analyzePdf730_testoConInteressiMutuo_estraeImportoInteressi() {
        // given
        String testo = """
                Oneri deducibili e detraibili
                RP7 interessi passivi mutuo prima casa: 1.500,00
                """;

        // when
        Map<String, String> result = extractor.analyzePdf730(testo);

        // then
        assertThat(result).containsKey("INTERESSI_MUTUO");
        assertThat(result.get("INTERESSI_MUTUO")).isEqualTo("1.500,00");
    }

    // ─── extractValueFromDocument – Pensione / CU ─────────────────────────────

    @Test
    @DisplayName("extractValueFromDocument - CU con reddito pensione - estrae importo correttamente")
    void extractValueFromDocument_cuConRedditoPensione_estraeImporto() {
        // given
        String testo = """
                CERTIFICAZIONE UNICA — Datore di lavoro: INPS
                Punto 001 — Redditi di pensione
                reddito di pensione erogato: 18.600,00
                """;

        // when
        String result = extractor.extractValueFromDocument(testo, "Pensione INPS", "Certificazione Unica");

        // then
        assertThat(result).isNotBlank();
        assertThat(result).isEqualTo("18.600,00");
    }

    @Test
    @DisplayName("extractValueFromDocument - CU senza importo pensione riconoscibile - restituisce estratto testo grezzo")
    void extractValueFromDocument_cuSenzaImporto_restituisceEstrattTestoGrezzo() {
        // given - testo CU ma senza pattern numerici riconoscibili
        String testo = "Certificazione Unica anno fiscale precedente. " +
                       "Il contribuente non ha percepito redditi da pensione nel periodo.";

        // when
        String result = extractor.extractValueFromDocument(testo, "Pensione INPS", "Certificazione Unica");

        // then
        assertThat(result).isNotNull().isNotBlank();
        // nessun importo trovato: il fallback restituisce il testo (troncato a 200 car.)
        assertThat(result.length()).isLessThanOrEqualTo(203); // 200 + "..."
    }

    @Test
    @DisplayName("extractValueFromDocument - documento non leggibile (testo vuoto) - restituisce messaggio di errore")
    void extractValueFromDocument_documentoVuoto_restituisceMessaggioNonLeggibile() {
        // when
        String result = extractor.extractValueFromDocument("", "Pensione INPS", "Certificazione Unica");

        // then
        assertThat(result).isEqualTo("Documento non leggibile");
    }

    // ─── extractValueFromDocument – Spese mediche / scontrini ─────────────────

    @Test
    @DisplayName("extractValueFromDocument - scontrino con importo EUR - estrae importo spesa medica")
    void extractValueFromDocument_scontrinoConImportoEur_estraeImportoSpesaMedica() {
        // given
        String testo = """
                FARMACIA BIANCHI SRL
                Data: 15/03/2024
                Tachipirina 1000mg x 10 cpr
                € 3,90
                IVA: 0,00
                TOTALE: € 3,90
                """;

        // when
        String result = extractor.extractValueFromDocument(testo, "Spese sanitarie", "scontrino farmacia");

        // then
        assertThat(result).isNotBlank();
        assertThat(result).isEqualTo("3,90");
    }

    @Test
    @DisplayName("extractValueFromDocument - scontrino con totale testuale - estrae importo da campo totale")
    void extractValueFromDocument_scontrinoConTotaleTestuale_estraeImportoDaCampoTotale() {
        // given
        String testo = "Visita medica specialistica\nTotale da pagare: 120,00\nRicevuta n. 00123";

        // when
        String result = extractor.extractValueFromDocument(testo, "Spese mediche", "spese sanitarie");

        // then
        assertThat(result).isEqualTo("120,00");
    }

    @Test
    @DisplayName("extractValueFromDocument - scontrino senza importo riconoscibile - restituisce estratto testo grezzo")
    void extractValueFromDocument_scontrinoSenzaImporto_restituisceEstrattTestoGrezzo() {
        // given - nessun pattern numerico nel formato atteso
        String testo = "Servizio ambulatoriale erogato. Si rimanda alla struttura per il pagamento.";

        // when
        String result = extractor.extractValueFromDocument(testo, "Spese mediche", "spese sanitarie");

        // then
        assertThat(result).isNotNull().isNotBlank();
        assertThat(result.length()).isLessThanOrEqualTo(203);
    }

    // ─── extractValueFromDocument – Interessi mutuo ───────────────────────────

    @Test
    @DisplayName("extractValueFromDocument - estratto conto mutuo con interessi detraibili - estrae quota interessi")
    void extractValueFromDocument_estrattoContoMutuoConInteressi_estraeQuotaInteressi() {
        // given
        String testo = """
                BANCA ESEMPIO S.p.A.
                Estratto conto mutuo ipotecario n. 1234567
                Anno 2023 — Riepilogo rate
                quota interessi detraibili: 1.234,56
                quota capitale: 4.765,44
                """;

        // when
        String result = extractor.extractValueFromDocument(testo, "Interessi mutuo", "mutuo ipotecario");

        // then
        assertThat(result).isNotBlank();
        assertThat(result).isEqualTo("1.234,56");
    }

    @Test
    @DisplayName("extractValueFromDocument - documento senza mutuo - restituisce estratto testo grezzo")
    void extractValueFromDocument_documentoSenzaMutuo_restituisceEstrattTestoGrezzo() {
        // given - documento riguardante un affitto, non un mutuo
        String testo = "Contratto di locazione abitativa. " +
                       "Il conduttore corrisponde al locatore un canone mensile.";

        // when
        String result = extractor.extractValueFromDocument(testo, "Interessi mutuo", "mutuo");

        // then
        assertThat(result).isNotNull().isNotBlank();
        assertThat(result.length()).isLessThanOrEqualTo(203);
    }

    @Test
    @DisplayName("extractValueFromDocument - mutuo con interessi generici EUR - estrae importo via fallback generico")
    void extractValueFromDocument_mutuoConInteressiEur_estraeImportoViaFallbackGenerico() {
        // given - documento con importo in formato EUR ma senza la label "quota interessi"
        String testo = """
                Piano di ammortamento 2023
                Interessi corrisposti: € 980,00
                """;

        // when
        String result = extractor.extractValueFromDocument(testo, "Interessi mutuo", "mutuo");

        // then
        assertThat(result).isNotBlank();
        assertThat(result).isEqualTo("980,00");
    }

    // ─── extractNomeCognome ────────────────────────────────────────────────────

    @Test
    @DisplayName("extractNomeCognome - testo 730 con cognome e nome su riga unica - restituisce valore non blank")
    void extractNomeCognome_testoConCognome_restituisceValoreNonBlank() {
        // given — formato realistico del 730 precompilato su una sola riga
        String testo = "COGNOME ROSSI MARIO contribuente anno 2024 redditi 14000";

        // when
        var result = extractor.extractNomeCognome(testo);

        // then — il metodo deve trovare almeno un frammento del nome (comportamento regex con (?i))
        assertThat(result).isPresent();
        assertThat(result.get()).isNotBlank();
    }

    @Test
    @DisplayName("extractNomeCognome - testo vuoto - restituisce Optional vuoto")
    void extractNomeCognome_testoVuoto_restituisceOptionalVuoto() {
        assertThat(extractor.extractNomeCognome("")).isEmpty();
    }

    @Test
    @DisplayName("extractNomeCognome - testo senza dati anagrafici - restituisce Optional vuoto")
    void extractNomeCognome_testoNonPertinente_restituisceOptionalVuoto() {
        assertThat(extractor.extractNomeCognome("Redditi 2024 pensione 15.000,00")).isEmpty();
    }

    // ─── extractDataNascita ────────────────────────────────────────────────────

    @Test
    @DisplayName("extractDataNascita - testo con 'nato il' e data gg/mm/aaaa - restituisce data")
    void extractDataNascita_testoConDataNascita_restituisceData() {
        // given
        String testo = "nato il 15/03/1965 a Roma";

        // when
        var result = extractor.extractDataNascita(testo);

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("15/03/1965");
    }

    @Test
    @DisplayName("extractDataNascita - testo con 'data di nascita' - restituisce data")
    void extractDataNascita_testoConDataDiNascita_restituisceData() {
        // given
        String testo = "Data di nascita: 22/07/1980";

        // when
        var result = extractor.extractDataNascita(testo);

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("22/07/1980");
    }

    @Test
    @DisplayName("extractDataNascita - testo vuoto - restituisce Optional vuoto")
    void extractDataNascita_testoVuoto_restituisceOptionalVuoto() {
        assertThat(extractor.extractDataNascita("")).isEmpty();
    }

    @Test
    @DisplayName("extractDataNascita - testo senza data di nascita - restituisce Optional vuoto")
    void extractDataNascita_testoNonPertinente_restituisceOptionalVuoto() {
        assertThat(extractor.extractDataNascita("Redditi anno 2024 pensione INPS")).isEmpty();
    }

    // ─── extractCodiceFiscale ──────────────────────────────────────────────────

    @Test
    @DisplayName("extractCodiceFiscale - testo con CF valido a 16 caratteri - restituisce CF")
    void extractCodiceFiscale_cfValido_restituisceCf() {
        // given
        String testo = "Codice fiscale: RSSMRA65C15H501Z";

        // when
        var result = extractor.extractCodiceFiscale(testo);

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("RSSMRA65C15H501Z");
    }

    @Test
    @DisplayName("extractCodiceFiscale - testo vuoto - restituisce Optional vuoto")
    void extractCodiceFiscale_testoVuoto_restituisceOptionalVuoto() {
        assertThat(extractor.extractCodiceFiscale("")).isEmpty();
    }

    @Test
    @DisplayName("extractCodiceFiscale - testo senza CF - restituisce Optional vuoto")
    void extractCodiceFiscale_testoSenzaCf_restituisceOptionalVuoto() {
        assertThat(extractor.extractCodiceFiscale("Reddito pensione 14.000,00 euro")).isEmpty();
    }

    // ─── extractSesso ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("extractSesso - testo con 'sesso M' - restituisce M")
    void extractSesso_testoConSessoM_restituisceM() {
        // given
        String testo = "Sesso M\ncognome ROSSI";

        // when
        var result = extractor.extractSesso(testo);

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("M");
    }

    @Test
    @DisplayName("extractSesso - testo con 'sesso F' - restituisce F")
    void extractSesso_testoConSessoF_restituisceF() {
        // given
        String testo = "sesso F data nascita 01/01/1970";

        // when
        var result = extractor.extractSesso(testo);

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("F");
    }

    @Test
    @DisplayName("extractSesso - testo vuoto - restituisce Optional vuoto")
    void extractSesso_testoVuoto_restituisceOptionalVuoto() {
        assertThat(extractor.extractSesso("")).isEmpty();
    }

    // ─── extractComuneNascita ──────────────────────────────────────────────────

    @Test
    @DisplayName("extractComuneNascita - testo con 'comune di nascita' - restituisce valore non blank")
    void extractComuneNascita_testoConComune_restituisceValoreNonBlank() {
        // given — formato testuale standard; la regex (?i) può catturare frammenti del comune
        String testo = "comune di nascita ROMA - provincia RM - codice catastale";

        // when
        var result = extractor.extractComuneNascita(testo);

        // then — il metodo deve trovare almeno un frammento del valore
        assertThat(result).isPresent();
        assertThat(result.get()).isNotBlank();
    }

    @Test
    @DisplayName("extractComuneNascita - testo vuoto - restituisce Optional vuoto")
    void extractComuneNascita_testoVuoto_restituisceOptionalVuoto() {
        assertThat(extractor.extractComuneNascita("")).isEmpty();
    }

    @Test
    @DisplayName("extractComuneNascita - testo senza comune - restituisce Optional vuoto")
    void extractComuneNascita_testoNonPertinente_restituisceOptionalVuoto() {
        assertThat(extractor.extractComuneNascita("Redditi 2024 pensione INPS 14.000")).isEmpty();
    }

    // ─── extractDomicilio ─────────────────────────────────────────────────────

    @Test
    @DisplayName("extractDomicilio - testo con indirizzo via - restituisce indirizzo")
    void extractDomicilio_testoConVia_restituisceIndirizzo() {
        // given
        String testo = "Via Roma 12, 00100 Roma RM";

        // when
        var result = extractor.extractDomicilio(testo);

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).contains("Via Roma");
    }

    @Test
    @DisplayName("extractDomicilio - testo con 'piazza' - restituisce indirizzo")
    void extractDomicilio_testoConPiazza_restituisceIndirizzo() {
        // given
        String testo = "Piazza Navona 5, 00186 Roma";

        // when
        var result = extractor.extractDomicilio(testo);

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).contains("Piazza Navona");
    }

    @Test
    @DisplayName("extractDomicilio - testo vuoto - restituisce Optional vuoto")
    void extractDomicilio_testoVuoto_restituisceOptionalVuoto() {
        assertThat(extractor.extractDomicilio("")).isEmpty();
    }

    @Test
    @DisplayName("extractDomicilio - testo senza indirizzo - restituisce Optional vuoto")
    void extractDomicilio_testoNonPertinente_restituisceOptionalVuoto() {
        assertThat(extractor.extractDomicilio("Redditi 2024 pensione INPS 14.000,00")).isEmpty();
    }
}
