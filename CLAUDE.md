# Hagenthon 2026 — 730 Facile

## Contesto del progetto
Assistente web che guida Maria (72 anni, pensionata) voce per voce attraverso il 730 precompilato INPS,
spiegando ogni campo in linguaggio semplice, chiedendo i documenti giusti, confrontando i valori.

**Tema:** Tema 01 - Accessibilita Digitale (Accenture Hagenthon 2026)

## Stack tecnico
- Frontend: React + PDF.js (highlight sezione corrente nel documento)
- Backend: Java Spring Boot
- AI: claude-haiku-4-5-20251001 per subagenti, claude-sonnet-4-6 per orchestratore
- PDF parsing: Apache PDFBox
- API key: variabile d'ambiente `ANTHROPIC_API_KEY` — mai in chiaro nel codice

## Architettura agentica
Orchestratore (Sonnet): gestisce flusso, coordina subagenti, mantiene stato JSON.
- pdf-analyzer (Haiku): estrae testo e coordinate delle 5 voci, posizioni per PDF.js
- document-comparator (Haiku): confronta valore utente vs precompilato, risponde OK/ATTENZIONE
- memory-manager (Haiku): traccia documenti caricati, evita duplicati, costruisce riepilogo

## Le 5 voci gestite
1. Pensione INPS -> CU (Certificazione Unica)
2. Spese mediche -> Scontrini/fatture o precompilato SSN
3. Interessi sul mutuo -> Certificato annuale banca
4. Detrazione familiari -> Documento identita familiare
5. Addizionale comunale/regionale -> Automatica, nessun documento

## Regole comportamento assistente
- Tono: semplice, paziente, mai tecnico
- Max 3 tentativi per voce, poi escalation CAF (HITL obbligatorio)
- Disclaimer obbligatorio all'avvio: suggerire CAF per dubbi importanti
- Mai cifre definitive: sempre "sembra corrispondere" o "c'e una differenza"
- Feature distintiva: pulsante "Mostra nel documento" — PDF con sezione in giallo
- Persistenza: stato sessione salvato su JSON

## Branch workflow (enforced da hook)
Ogni nuova modifica parte da un branch di tipo feature/nome-modifica.
Push e merge diretti su main, stable, test sono bloccati dall'hook check-branch.
Il merge avviene sempre tramite Pull Request su GitHub.

Flusso di promozione: feature -> PR su test -> PR su stable -> PR su main (release)

## Ambienti
- main: Release/Produzione — demo finale giuria
- stable: Staging — integrazione funzionalita verificate
- test: Test — prima validazione ogni feature
- feature/X: Sviluppo attivo locale

## Accesso database
Solo gli sviluppatori in .claude/developers.txt possono modificare il DB.
I funzionali possono solo visualizzare. L'hook check-db-access blocca le modifiche non autorizzate.

## Hook attivi (.claude/settings.json)
- check-read-path: blocca lettura file fuori dal repository
- check-branch: blocca push/merge diretti su main/stable/test
- check-db-access: blocca modifiche DB per utenti non in developers.txt

## Criteri valutazione giuria (priorita sviluppo)
- 24% Profondita agentica: orchestrazione, 3 subagenti, stato JSON, output strutturati
- 19% Qualita istruzioni: scope chiaro, JSON schema output, vincoli, no overlap
- 15% Robustezza: fallback CAF dopo 3 tentativi, error handling PDF, timeout API
- 12% Efficienza token: Haiku per subagenti, Sonnet solo per orchestratore
- 12% Qualita tecnica: error handling, retry, secrets in env, model tiering
- 11% Adeguatezza strumenti: 3 subagenti distinti non ridondanti
-  7% Documentazione: README, flusso agentico, prerequisiti
-  0% Qualita idea (non pesa)

## Deliverable obbligatori
1. Prototipo funzionante (demo 2-3 voci)
2. Presentazione HTML 5 min (brand Accenture: viola #A100FF, sfondo #050008)
3. Evidenza validazione (prima/dopo)
4. Nota processo AI

## Prima di ogni sessione di sviluppo
1. Esegui `git pull` sul branch corrente per allinearti all'ultimo stato remoto.
2. Verifica se ci sono nuovi requisiti: controlla issue aperte, commenti su PR, e aggiornamenti in questo CLAUDE.md.
3. Solo dopo inizia a sviluppare.

## Note operative
- Stato sessione JSON: voce_corrente, documenti_caricati, risultati, tentativi_per_voce
- Model tiering: orchestratore = Sonnet, subagenti = Haiku
- Nessun commento al codice se auto-esplicativo
