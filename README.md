# 730 Facile — Hagenthon 2026

Assistente web che guida Maria (72 anni, pensionata) voce per voce attraverso il 730 precompilato INPS, spiegando ogni campo in linguaggio semplice e verificando i valori con i documenti dell'utente.

**Tema:** Accessibilita Digitale — Accenture Hagenthon 2026  
**Team:** Giuliana Russo, Enrico Lunga Via

---

## Struttura repository

```
/
+-- app/              Soluzione sviluppata
|   +-- frontend/     React + TypeScript + PDF.js
|   +-- backend/      Java 21 Spring Boot
+-- agents/           Struttura agentica (documentazione giuria)
+-- presentation/     Presentazione HTML (brand Accenture)
+-- .claude/          Configurazione Claude Code
|   +-- agents/       Definizioni subagenti operativi
|   +-- hooks/        Script hook di enforcement
|   +-- settings.json Hook e permessi
|   +-- developers.txt Email sviluppatori autorizzati DB
+-- CLAUDE.md         Contesto progetto per Claude Code
+-- README.md
```

---

## Setup

### Prerequisiti
- Java 21+
- Node.js 20+
- Maven 3.9+
- Git

### Variabili d'ambiente obbligatorie
```bash
ANTHROPIC_API_KEY=sk-ant-...
```

### Avvio backend
```bash
cd app/backend
./mvnw spring-boot:run
```

### Avvio frontend
```bash
cd app/frontend
npm install
npm run dev
```

---

## Branch Workflow

```
main    <- Release/Produzione (demo finale giuria)
stable  <- Staging (integrazione verificata)
test    <- Test (prima validazione)
feature/nome <- Sviluppo attivo
```

**Ogni modifica segue questo flusso:**
1. `git checkout -b feature/nome-modifica`
2. Sviluppo con agenti: requirements-definer -> analyzer -> developer -> ux-tester
3. Commit sul branch feature
4. Pull Request su `test` (l'hook blocca push diretti)
5. PR approvata -> `stable` -> `main`

---

## Architettura Agentica

### Agenti Runtime (applicazione)
| Agente | Modello | Ruolo |
|--------|---------|-------|
| Orchestratore | Sonnet 4.6 | Flusso conversazionale, coordinamento |
| pdf-analyzer | Haiku 4.5 | Estrazione dati e coordinate dal PDF 730 |
| document-comparator | Haiku 4.5 | Confronto valori utente vs precompilato |
| memory-manager | Haiku 4.5 | Stato sessione, riepilogo finale |

### Agenti Development Workflow
| Agente | Modello | Ruolo |
|--------|---------|-------|
| requirements-definer | Opus 5.5 | Struttura requisiti e user stories |
| requirement-analyzer | Opus 5.5 | Analisi tecnica, edge cases, test JUnit |
| java-react-developer | Sonnet 4.6 | Implementazione (test first obbligatorio) |
| ux-tester-elderly | Sonnet 4.6 | Simulazione utente anziano (Maria) |

---

## Hook Attivi

| Hook | Trigger | Comportamento |
|------|---------|---------------|
| check-read-path | Read, Edit, Write | Blocca accesso a file fuori dal repo |
| check-branch | Bash (git push/merge) | Blocca push diretti su main/stable/test |
| check-db-access | Bash, Edit, Write | Blocca modifiche DB per non-sviluppatori |

---

## Regola JUnit
Per ogni nuova implementazione Java: scrivere PRIMA i test (happy path + edge cases + errori), POI il codice. Copertura minima: 90% branch coverage sul codice nuovo.

---

## Le 5 Voci Gestite
| Voce | Documento di verifica |
|------|-----------------------|
| Pensione INPS | CU (Certificazione Unica) |
| Spese mediche | Scontrini/fatture o precompilato SSN |
| Interessi sul mutuo | Certificato annuale banca |
| Detrazione familiari a carico | Documento identita familiare |
| Addizionale comunale/regionale | Automatica — nessun documento |
