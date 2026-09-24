import { useState, useEffect } from 'react';
import type { StepInfo, CompletedStep } from '../types';
import { uploadDocument, confirmStep, submitManualValue } from '../api/session';
import DocumentUpload from './DocumentUpload';
import ComparisonView from './ComparisonView';

type WizardPhase = 'input' | 'comparison';

interface Props {
  sessionId: string;
  step: StepInfo;
  totalSteps: number;
  onStepComplete: (
    completed: CompletedStep,
    response: { nextStep: number | null; isCompleted: boolean }
  ) => void;
}

export default function StepWizard({ sessionId, step, totalSteps, onStepComplete }: Props) {
  const [manualInput, setManualInput] = useState('');
  const [phase, setPhase] = useState<WizardPhase>('input');
  const [value730, setValue730] = useState<string | null>(step.value730);
  const [valueDocument, setValueDocument] = useState<string | null>(null);
  const [uploadLoading, setUploadLoading] = useState(false);
  const [manualLoading, setManualLoading] = useState(false);
  const [confirmLoading, setConfirmLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    setManualInput('');
    setError('');
    setValue730(step.value730);
    if (step.alreadyUploaded && step.valueDocument !== null) {
      setValueDocument(step.valueDocument);
      setPhase('comparison');
    } else {
      setValueDocument(null);
      setPhase('input');
    }
  }, [step.stepIndex]);

  const handleConfirm = async (value: string) => {
    setError('');
    setConfirmLoading(true);
    try {
      const res = await confirmStep(sessionId, value);
      onStepComplete(
        { stepIndex: step.stepIndex, stepName: step.stepName, confirmedValue: value },
        res
      );
    } catch {
      setError('Non è stato possibile salvare la risposta. Controlla la connessione e riprova.');
    } finally {
      setConfirmLoading(false);
    }
  };

  const handleManualSubmit = async () => {
    if (!manualInput.trim()) return;
    setError('');
    setManualLoading(true);
    try {
      const res = await submitManualValue(sessionId, manualInput.trim());
      setValue730(res.value730);
      setValueDocument(res.valueUser);
      setPhase('comparison');
    } catch {
      setError('Non è stato possibile verificare il valore. Controlla la connessione e riprova.');
    } finally {
      setManualLoading(false);
    }
  };

  const handleFileUpload = async (file: File) => {
    setError('');
    setUploadLoading(true);
    try {
      const res = await uploadDocument(sessionId, file);
      setValueDocument(res.extractedValue);
      setPhase('comparison');
    } catch {
      setError('Non è stato possibile leggere il documento. Assicurati che il file sia leggibile e riprova.');
    } finally {
      setUploadLoading(false);
    }
  };

  const stepLabel = `Passo ${step.stepIndex + 1} di ${totalSteps}`;

  // AUTOMATIC step
  if (step.stepType === 'AUTOMATIC') {
    return (
      <div className="step-card">
        <span className="step-badge">{stepLabel}</span>
        <h3>{step.stepName}</h3>
        <p>Questo dato viene calcolato automaticamente dal tuo 730. Controlla che sia corretto e vai avanti.</p>
        {error && <div className="alert alert-error">{error}</div>}
        {step.value730 && (
          <div className="value-box">
            <div className="value-box-label">Valore nel tuo 730</div>
            <div className="value-box-value">{step.value730}</div>
          </div>
        )}
        <div className="step-actions">
          <button
            className="btn btn-primary"
            style={{ minHeight: '52px', fontSize: '17px' }}
            onClick={() => handleConfirm(step.value730 ?? '')}
            disabled={confirmLoading}
          >
            {confirmLoading ? <span className="spinner" /> : 'Continua'}
          </button>
        </div>
      </div>
    );
  }

  // MANUAL_ENTRY step
  if (step.stepType === 'MANUAL_ENTRY') {
    return (
      <div className="step-card">
        <span className="step-badge">{stepLabel}</span>
        <h3>{step.stepName}</h3>
        <p>{step.description || 'Inserisci il valore richiesto per continuare.'}</p>
        {error && <div className="alert alert-error">{error}</div>}

        {phase === 'input' ? (
          <>
            <input
              className="value-input"
              value={manualInput}
              onChange={(e) => setManualInput(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter') handleManualSubmit(); }}
              placeholder="Scrivi qui il tuo valore…"
              aria-label={step.stepName}
              style={{ marginBottom: '16px', fontSize: '20px', minHeight: '56px' }}
            />
            <div className="step-actions">
              <button
                className="btn btn-primary"
                style={{ minHeight: '52px', fontSize: '17px' }}
                onClick={handleManualSubmit}
                disabled={manualLoading || !manualInput.trim()}
              >
                {manualLoading ? <span className="spinner" /> : 'Verifica'}
              </button>
            </div>
          </>
        ) : (
          <>
            <ComparisonView
              value730={value730}
              valueDocument={valueDocument}
              onConfirm={handleConfirm}
              disabled={confirmLoading}
            />
            <button
              className="btn btn-ghost"
              style={{ marginTop: '12px', width: '100%', minHeight: '44px' }}
              onClick={() => { setPhase('input'); setError(''); }}
              disabled={confirmLoading}
            >
              Modifica il valore inserito
            </button>
          </>
        )}
      </div>
    );
  }

  // DOCUMENT_UPLOAD step (default)
  return (
    <div className="step-card">
      <span className="step-badge">{stepLabel}</span>
      <h3>{step.stepName}</h3>
      <p>{step.description || 'Carica il documento richiesto per continuare.'}</p>
      {error && <div className="alert alert-error">{error}</div>}

      {phase === 'input' ? (
        <DocumentUpload
          onFile={handleFileUpload}
          loading={uploadLoading}
          description={step.documentRequired ?? 'Trascina il documento qui'}
        />
      ) : (
        <>
          <ComparisonView
            value730={value730}
            valueDocument={valueDocument}
            onConfirm={handleConfirm}
            disabled={confirmLoading}
          />
          <button
            className="btn btn-ghost"
            style={{ marginTop: '12px', width: '100%', minHeight: '44px' }}
            onClick={() => { setPhase('input'); setError(''); }}
            disabled={confirmLoading}
          >
            Ricarica documento
          </button>
        </>
      )}
    </div>
  );
}
