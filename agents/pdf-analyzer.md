# PDF Analyzer

**Modello:** claude-haiku-4-5-20251001  
**Tipo:** Agente runtime (applicazione)

## Responsabilita
Estrae testo e coordinate delle 5 voci target dal PDF del 730 precompilato. Restituisce posizioni bbox per l'evidenziazione tramite PDF.js nel frontend.

## Le 5 voci target
1. Pensione INPS
2. Spese mediche
3. Interessi sul mutuo
4. Detrazione familiari a carico
5. Addizionale comunale/regionale

## Input
Path del file PDF caricato dall'utente.

## Output
JSON strutturato con per ogni voce: valore estratto, pagina, coordinate bbox (x, y, width, height), flag trovata.

## Vincoli
- Non modifica il PDF originale
- Timeout: 30 secondi
- Se una voce non e' trovata: trovata=false, non inventare valori
- Usa Apache PDFBox lato backend per l'estrazione

## Perche' Haiku
Task strutturato e ripetibile con output JSON predefinito. La velocita' e il costo ridotto di Haiku sono preferibili in questo caso — la qualita' dell'output dipende dalla struttura del prompt, non dalla potenza del modello.
