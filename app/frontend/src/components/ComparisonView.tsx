import { useState } from 'react';

interface Props {
  value730: string | null;
  valueDocument: string | null;
  onConfirm: (value: string) => void;
  disabled?: boolean;
}

type DerivedState = 'PENDING' | 'OK' | 'MISMATCH';

function deriveState(value730: string | null, valueDocument: string | null): DerivedState {
  if (valueDocument === null) return 'PENDING';
  if (value730 === null) return 'OK';
  if (value730.trim() === valueDocument.trim()) return 'OK';
  return 'MISMATCH';
}

export default function ComparisonView({ value730, valueDocument, onConfirm, disabled = false }: Props) {
  const [showCustomInput, setShowCustomInput] = useState(false);
  const [customValue, setCustomValue] = useState('');

  const state = deriveState(value730, valueDocument);

  if (state === 'PENDING') {
    return (
      <div className="comparison-pending">
        <span className="spinner" />
        <span>Elaborazione in corso…</span>
      </div>
    );
  }

  if (state === 'OK') {
    return (
      <div className="comparison-ok">
        <div className="comparison-ok-check">✓</div>
        <div className="comparison-ok-title">Tutto corrisponde, perfetto!</div>
        {valueDocument && (
          <div className="comparison-ok-value">{valueDocument}</div>
        )}
        <button
          className="btn btn-primary comparison-ok-btn"
          onClick={() => onConfirm(valueDocument ?? value730 ?? '')}
          disabled={disabled}
        >
          {disabled ? <span className="spinner" /> : 'Continua'}
        </button>
      </div>
    );
  }

  // MISMATCH
  return (
    <div className="comparison-card">
      <h3 className="comparison-title">Abbiamo trovato due valori diversi</h3>
      <p className="comparison-subtitle">
        Controlla i valori qui sotto e scegli quello giusto.
      </p>

      <div className="comparison-values">
        <div className="comparison-value-box">
          <div className="comparison-value-label">Nel tuo 730 c'è scritto:</div>
          <div className="comparison-value-amount">{value730}</div>
        </div>
        <div className="comparison-value-divider">vs</div>
        <div className="comparison-value-box">
          <div className="comparison-value-label">Il documento dice:</div>
          <div className="comparison-value-amount">{valueDocument}</div>
        </div>
      </div>

      <p className="comparison-question">Quale è quello corretto?</p>

      <div className="comparison-actions">
        <button
          className="btn btn-secondary comparison-btn"
          onClick={() => { setShowCustomInput(false); onConfirm(value730 ?? ''); }}
          disabled={disabled}
        >
          <span className="comparison-btn-label">Usa il valore del 730</span>
          <span className="comparison-btn-value">{value730}</span>
        </button>
        <button
          className="btn btn-secondary comparison-btn"
          onClick={() => { setShowCustomInput(false); onConfirm(valueDocument ?? ''); }}
          disabled={disabled}
        >
          <span className="comparison-btn-label">Usa il valore del documento</span>
          <span className="comparison-btn-value">{valueDocument}</span>
        </button>
      </div>

      <button
        className="btn btn-ghost comparison-btn-other"
        onClick={() => setShowCustomInput((v) => !v)}
        disabled={disabled}
      >
        Inserisci un altro valore
      </button>

      {showCustomInput && (
        <div className="comparison-custom">
          <label className="comparison-custom-label" htmlFor="comparison-custom-input">
            Scrivi il valore corretto:
          </label>
          <input
            id="comparison-custom-input"
            className="value-input"
            value={customValue}
            onChange={(e) => setCustomValue(e.target.value)}
            placeholder="Inserisci il valore corretto"
            aria-label="Valore personalizzato"
          />
          <button
            className="btn btn-primary"
            style={{ minHeight: '52px', fontSize: '17px', marginTop: '12px' }}
            onClick={() => onConfirm(customValue.trim())}
            disabled={disabled || !customValue.trim()}
          >
            {disabled ? <span className="spinner" /> : 'Conferma questo valore'}
          </button>
        </div>
      )}
    </div>
  );
}
