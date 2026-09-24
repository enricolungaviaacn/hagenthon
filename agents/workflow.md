# Flusso Agentico — 730 Facile

## Flusso Applicativo (Runtime)

```
Utente carica PDF 730
        |
        v
[Orchestratore - Sonnet]
  Valida upload, avvia sessione
        |
        v
[pdf-analyzer - Haiku]
  Estrae testo e coordinate delle 5 voci
  Output: JSON con valori e posizioni bbox
        |
        v
[Orchestratore]
  Seleziona la prima voce da analizzare
  Spiega la voce a Maria in linguaggio semplice
  Chiede il documento di verifica
        |
        v
[memory-manager - Haiku]
  Controlla se il documento e' gia' stato caricato
  Aggiorna lo stato della sessione
        |
        v
  Utente inserisce il valore dal documento
        |
        v
[document-comparator - Haiku]
  Confronta valore utente vs valore 730
  Risponde OK o ATTENZIONE in linguaggio semplice
        |
        v
[Orchestratore]
  Se tentativi >= 3 per questa voce -> ESCALATION CAF
  Se OK o ATTENZIONE accettata -> voce successiva
        |
        v
  Tutte le voci completate?
   NO -> torna a seleziona voce
   SI  -> [memory-manager] genera riepilogo finale
        |
        v
  Mostra riepilogo a Maria
```

## Flusso di Sviluppo (Development Workflow)

```
Idea nuova feature o problema segnalato
        |
        v
[requirements-definer - Opus 5.5]
  Struttura il requisito in user story
  Propone varianti e feature correlate
  Output: JSON REQ-NNN con criteri accettazione
        |
        v
[requirement-analyzer - Opus 5.5]
  Analizza edge cases e dipendenze
  Genera lista test_cases_junit
  Identifica impatto su agenti esistenti
  Output: analisi dettagliata con domande aperte
        |
        v
  Revisione umana (sviluppatore)
  Risposta alle domande aperte
        |
        v
[java-react-developer - Sonnet 4.6]
  PRIMA: scrive test JUnit per tutti i casi
  POI: implementa il codice
  Verifica: ./mvnw test (tutti i test devono passare)
        |
        v
[ux-tester-elderly - Sonnet 4.6]
  Simula Maria che usa la nuova feature
  Segnala confusioni e blocchi
  Output: lista problemi con priorita
        |
        v
  Revisione umana + fix problemi segnalati
  (ciclo java-react-developer <-> ux-tester-elderly)
        |
        v
  git commit su feature/nome-requisito
  Pull Request su test (hook blocca push diretto)
        |
        v
  PR approvata -> test -> stable -> main
```

## Regole di Escalation
- Max 3 tentativi per voce -> suggerisci CAF
- PDF non riconoscibile come 730 -> messaggio chiaro + stop
- Errore API Claude -> fallback su messaggio di attesa
- Sessione interrotta -> ripristino automatico da session_state.json
