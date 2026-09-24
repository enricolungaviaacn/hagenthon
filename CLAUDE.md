# 730 Facile

## Contesto del progetto
Applicazione web che guida Maria (72 anni, pensionata) passo per passo nella verifica del 730 precompilato,
spiegando ogni campo in linguaggio semplice, chiedendo i documenti di supporto e confrontando i valori.

## Stack tecnico
- Frontend: React 18 + TypeScript + Vite (porta 3000)
- Backend: Java 21 + Spring Boot 3.2.5 (porta 8080)
- Database: H2 su file — utenti, sessioni, documenti caricati
- Autenticazione: JWT con scadenza 24h, password con BCrypt
- Estrazione dati: Apache PDFBox + regole di pattern matching, **nessuna dipendenza da API esterne a runtime**

Il frontend chiama sempre URL relativi (`/api/...`): il proxy Vite instrada verso la porta 8080.

## Logica di verifica (il cuore del prodotto)

Per ogni campo il sistema fa tre cose:
1. Estrae il valore dal **730 caricato**
2. Ottiene il valore dall'**utente** — digitato a mano oppure estratto dal documento di supporto
3. **Confronta**: se coincidono l'esito è `OK`; se divergono è `MISMATCH` e l'utente sceglie quale valore è corretto

Gli esiti possibili sono `OK`, `MISMATCH`, `PENDING`.

## I 12 step

**Anagrafica (`MANUAL_ENTRY`)** — l'utente digita, il sistema confronta col frontespizio del 730:

| # | Step | Dove sta nel 730 |
|---|---|---|
| 0 | Nome e cognome | Frontespizio |
| 1 | Data di nascita | Frontespizio |
| 2 | Codice fiscale | Frontespizio |
| 3 | Sesso | Frontespizio |
| 4 | Comune di nascita | Frontespizio |
| 5 | Domicilio (via, CAP, comune, provincia) | Frontespizio |

**Voci fiscali (`DOCUMENT_UPLOAD`)** — l'utente carica un documento, il sistema ne estrae il valore e lo confronta:

| # | Step | Documento | Quadro |
|---|---|---|---|
| 6 | Pensione INPS | Certificazione Unica | RC1 |
| 7 | Reddito lavoro dipendente | CU del datore di lavoro | RC1 |
| 8 | Spese mediche | Fatture, scontrini | RP1 |
| 9 | Interessi mutuo | Certificato della banca | RP7 |
| 10 | Familiari a carico | Documento d'identità | RC6 |

**Automatico (`AUTOMATIC`)**:

| # | Step | Note |
|---|---|---|
| 11 | Addizionale regionale e comunale | Calcolata, nessun documento richiesto |

## Documenti di prova

In `test-data/` ci sono tre fac-simile coerenti tra loro, tutti intestati a ROSSI MARIA (`RSSMRA53C54H501Z`,
nata il 14/03/1953 a Roma): il 730 precompilato, la CU INPS e una fattura medica. Pensione `18.500,00` e
spese mediche `1.200,00` combaciano tra 730 e documenti, quindi il confronto dà `OK`.

Il testo già estratto da questi PDF è disponibile come fixture in
`app/backend/src/test/resources/fixtures/` — i test devono usare quelle, non stringhe inventate.

**Questi file non devono mai finire su `main` o `stable`** (hook `check-test-data`).

### Insidie note nell'estrazione
Chi tocca le regex di `DocumentDataExtractor` deve tenerne conto:
- Il 730 mette il valore **sulla riga sotto** l'etichetta; la CU affianca gruppi di etichette e gruppi di valori
- Ci sono **più date** e **più codici fiscali** per documento: ancorare sempre all'etichetta giusta, mai prendere la prima occorrenza
- Il simbolo dell'euro esce corrotto dall'estrazione: **nessuna regex deve dipendere dalla valuta** o dalle lettere accentate
- Nella fattura medica ci sono otto importi: solo quello dopo "TOTALE DOVUTO" è quello buono

## Regole di comportamento verso l'utente
- Tono semplice e paziente, mai tecnico
- Mai cifre presentate come definitive: "sembra corrispondere" oppure "c'è una differenza"
- Disclaimer all'avvio: per i dubbi importanti rivolgersi a un CAF
- Font minimo 16px per il testo, 20px per i titoli; pulsanti alti almeno 44px
- Messaggi di errore in italiano semplice, senza gergo

## Ciclo di sviluppo

Gli agenti lavorano in tre fasi, in quest'ordine e senza sovrapposizioni:

1. **Sviluppo** — `backend-developer` e `frontend-developer`, in parallelo. Si aspetta che **tutti** abbiano chiuso
2. **Test** — code review, poi `junit-tester` **una volta sola** su tutti i commit della fase
3. **Build** — solo a test verdi: build di backend e frontend, poi avvio del backend sulla 8080

Il coordinamento è di `agent-lead`, che è l'unico punto d'ingresso. Il motivo della sequenza: un tester che
gira su codice ancora in scrittura produce fallimenti falsi, e due processi Maven sulla stessa cartella
`target/` si corrompono a vicenda.

### Agenti attivi
`agent-lead` (coordinamento) · `backend-developer` · `frontend-developer` · `junit-tester` ·
`requirements-definer` · `requirement-analyzer` · `ux-tester-elderly`

## Branch
- `main` — release
- `stable` — integrazione di funzionalità verificate
- `test` — prima validazione
- `feature/X` — sviluppo attivo

Promozione: `feature` → `test` → `stable` → `main`.

## Hook attivi (.claude/settings.json)
Registrati sia sul tool Bash sia su PowerShell — altrimenti basta cambiare shell per aggirarli.

- `check-read-path` — blocca letture fuori dal repository
- `check-branch` — blocca push e merge diretti su main, stable, test
- `check-db-access` — blocca modifiche al DB per chi non è in `.claude/developers.txt`
- `check-agent-role` — richiede il marker `.claude/active-agent` per scrivere in `app/`
- `check-test-data` — tiene i file di prova fuori da main e stable

## Note operative
- Logging obbligatorio: `@Slf4j` su ogni `@Service` e `@RestController` — mai `System.out.println`
- Mai loggare password, token JWT o chiavi: usare `[REDACTED]`
- Nessun commento al codice se è auto-esplicativo
- Mai indebolire l'asserzione di un test per farlo passare: se fallisce, il problema è nel codice

## Regola linguaggio (obbligatoria per tutti gli agenti)
Nessun agente può mai usare o citare: AI, artificiale, intelligenza artificiale, giuria, gara, hackathon, contest, competizione — né in output verso l'utente finale né in commenti nel codice.
