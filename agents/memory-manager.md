# Memory Manager

**Modello:** claude-haiku-4-5-20251001  
**Tipo:** Agente runtime (applicazione)

## Responsabilita
Gestisce lo stato persistente della sessione 730. Evita di richiedere piu volte gli stessi documenti all'utente. Costruisce il riepilogo finale.

## Operazioni
- load: carica session_state.json (crea nuovo se non esiste)
- save: salva lo stato aggiornato
- update_voce: aggiorna risultato e tentativi per una voce
- check_documento: verifica se un documento e' gia' stato caricato
- summarize: genera riepilogo finale in linguaggio semplice

## Schema session_state.json
Contiene: sessione_id, voce_corrente, voci_completate, voci_da_fare, documenti_caricati, risultati, tentativi_per_voce, escalation_caf.

## Vincoli
- Scrive solo nella directory `data/sessions/` del backend
- Non leggere file al di fuori del repository
