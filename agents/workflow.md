# Flusso Agentico — 730 Facile

## Principio architetturale
L'Orchestratore e' il centro di tutte le comunicazioni. Nessun agente comunica direttamente
con un altro agente: tutto passa attraverso l'Orchestratore, che decide cosa fare dopo ogni
risposta e a quale agente passare il controllo.

```
                    [ORCHESTRATORE - Sonnet]
                    Legge CLAUDE.md all'avvio
                           |
          _________________|_________________
         |          |          |             |
         v          v          v             v
    [Runtime]  [Funzionale] [Tecnico]   [UX Tester]
    pdf-analyzer requirements java-react  ux-tester
    doc-compar  definer/Opus  developer   elderly
    memory-mgr  req-analyzer  Sonnet      Sonnet
                Opus
```

---

## Flusso 1: Workflow di Sviluppo (Development)

```
ORCHESTRATORE riceve richiesta di nuova feature dal team
        |
        | legge CLAUDE.md per verificare coerenza
        v
[requirements-definer / Opus]
  Produce: user story + mockup testuale + voce di Maria
           + scenario fallimento + impatto 5 voci + stima token
        |
        | Orchestratore valida l'output e lo passa all'analisi
        v
[requirement-analyzer / Opus]
  Produce: edge cases + dipendenze + lista JUnit obbligatoria
           + impatto agenti + domande aperte
        |
        | Revisione umana del team (OK o modifica)
        v
[java-react-developer / Sonnet]
  STEP 1: Scrive test JUnit per TUTTI i casi (lista da requirement-analyzer)
          -> git commit su feature/REQ-NNN-step1-tests
          -> PR su test (hook blocca push diretto)
          -> Orchestratore notifica team: "PR pronta per review"

  STEP 2: Implementa codice backend (Spring Boot)
          -> git commit su feature/REQ-NNN-step2-backend
          -> PR su test

  STEP 3: Implementa codice frontend (React)
          -> git commit su feature/REQ-NNN-step3-frontend
          -> PR su test
        |
        | Orchestratore passa ogni step a UX Tester
        v
[ux-tester-elderly / Sonnet]
  Simula Maria su mockup testuale (STEP 1) e implementazione reale (STEP 2/3)
  Segnala: problemi bloccanti / fastidiosi / miglioramenti
        |
        | Orchestratore raccoglie feedback e decide se iterare o procedere
        v
  Fix problemi UX -> nuovo commit -> nuova PR
        |
        v
  PR approvata: test -> stable -> main
```

---

## Flusso 2: Runtime Applicativo (Conversazione con Maria)

```
Maria carica il PDF del 730
        |
        v
ORCHESTRATORE
  Legge CLAUDE.md, carica sessione via memory-manager
  Mostra disclaimer CAF obbligatorio
        |
        v
[pdf-analyzer / Haiku]
  Estrae le 5 voci con coordinate bbox
  Output: JSON strutturato con posizioni
        |
        v
ORCHESTRATORE
  Per ogni voce (in sequenza):
  1. Spiega la voce in linguaggio semplice
  2. Invia bbox al frontend -> highlight PDF.js (pulsante "Mostra nel documento")
  3. Controlla via memory-manager se il documento e' gia' stato caricato
  4. Chiede il valore dal documento di verifica
        |
        v
[document-comparator / Haiku]
  Confronta valore utente vs valore 730
  Output: OK / ATTENZIONE / ESCALATION_CAF (dopo 3 tentativi)
        |
        v
[memory-manager / Haiku]
  Salva risultato voce, aggiorna tentativi
  Se tutte le voci completate: genera riepilogo finale
        |
        v
ORCHESTRATORE
  Presenta risultato a Maria
  Se ESCALATION_CAF: suggerisce CAF, passa alla voce successiva
  Se tutte le voci finiscono: mostra riepilogo e chiude la sessione
```

---

## Regole di Escalation

| Condizione | Azione Orchestratore |
|------------|---------------------|
| Tentativi >= 3 per una voce | Suggerisce CAF per quella voce, avanza |
| PDF non riconosciuto come 730 | Messaggio semplice, chiede di ricaricare |
| Errore API Claude | Messaggio generico, riprova automaticamente |
| Sessione interrotta | Ripristino automatico da session_state.json |
| Domanda fiscale definitiva | Rimanda sempre al CAF, non risponde |

---

## Struttura PR per step (Development)

Ogni requisito viene sviluppato in PR separate e sequenziali:

```
feature/REQ-001-step1-junit-tests    -> PR su test (solo test JUnit)
feature/REQ-001-step2-backend        -> PR su test (implementazione Java)
feature/REQ-001-step3-frontend       -> PR su test (componenti React)
feature/REQ-001-step4-ux-fixes       -> PR su test (fix da ux-tester-elderly)
```

Ogni PR deve passare tutti i test prima di essere approvata.
La promozione avviene: test -> stable -> main (sempre via PR, mai push diretto).
