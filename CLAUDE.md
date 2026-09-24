# 730 Facile

## Contesto del progetto
Assistente web che guida Maria (72 anni, pensionata) voce per voce attraverso il 730 precompilato INPS,
spiegando ogni campo in linguaggio semplice, chiedendo i documenti giusti, confrontando i valori.

## Stack tecnico
- Frontend: React + PDF.js (highlight sezione corrente nel documento)
- Backend: Java Spring Boot
- Analisi documenti: claude-haiku-4-5-20251001 per subagenti, claude-sonnet-4-6 per orchestratore
- PDF parsing: Apache PDFBox
- API key: variabile d'ambiente `ANTHROPIC_API_KEY` — mai in chiaro nel codice

## Architettura
Orchestratore (Sonnet): gestisce flusso, coordina subagenti, mantiene stato JSON.
- pdf-analyzer (Haiku): estrae testo e coordinate delle 5 voci, posizioni per PDF.js
- document-comparator (Haiku): confronta valore utente vs precompilato, risponde OK/ATTENZIONE
- memory-manager (Haiku): traccia documenti caricati, evita duplicati, costruisce riepilogo

## Le 5 voci gestite
1. Pensione INPS -> CU (Certificazione Unica)
2. Spese mediche -> Scontrini/fatture o precompilato SSN
3. Interessi sul mutuo -> Certificato annuale banca
4. Detrazione familiari -> Documento identità familiare
5. Addizionale comunale/regionale -> Automatica, nessun documento

## Regole comportamento assistente
- Tono: semplice, paziente, mai tecnico
- Max 3 tentativi per voce, poi escalation CAF (HITL obbligatorio)
- Disclaimer obbligatorio all'avvio: suggerire CAF per dubbi importanti
- Mai cifre definitive: sempre "sembra corrispondere" o "c'è una differenza"
- Feature distintiva: pulsante "Mostra nel documento" — PDF con sezione in giallo
- Persistenza: stato sessione salvato su JSON

## Branch workflow (enforced da hook)
Ogni nuova modifica parte da un branch di tipo feature/nome-modifica.
Push e merge diretti su main, stable, test sono bloccati dall'hook check-branch.
Il merge avviene sempre tramite Pull Request su GitHub.

Flusso di promozione: feature -> PR su test -> PR su stable -> PR su main (release)

## Ambienti
- main: Release/Produzione
- stable: Staging — integrazione funzionalità verificate
- test: Test — prima validazione ogni feature
- feature/X: Sviluppo attivo locale

## Accesso database
Solo gli sviluppatori in .claude/developers.txt possono modificare il DB.
I funzionali possono solo visualizzare. L'hook check-db-access blocca le modifiche non autorizzate.

## Hook attivi (.claude/settings.json)
- check-read-path: blocca lettura file fuori dal repository
- check-branch: blocca push/merge diretti su main/stable/test
- check-db-access: blocca modifiche DB per utenti non in developers.txt

## Prima di ogni sessione di sviluppo
1. Esegui `git pull` sul branch corrente per allinearti all'ultimo stato remoto.
2. Verifica se ci sono nuovi requisiti: controlla issue aperte, commenti su PR, e aggiornamenti in questo CLAUDE.md.
3. Solo dopo inizia a sviluppare.

## Note operative
- Stato sessione JSON: voce_corrente, documenti_caricati, risultati, tentativi_per_voce
- Model tiering: orchestratore = Sonnet, subagenti = Haiku
- Logging obbligatorio: `@Slf4j` su ogni Service e Controller — nessun System.out.println
- Nessun commento al codice se auto-esplicativo

## Regola linguaggio (obbligatoria per tutti gli agenti)
Nessun agente può mai usare o citare: AI, artificiale, intelligenza artificiale, giuria, gara, hackathon, contest, competizione — né in output verso l'utente finale né in commenti nel codice.
