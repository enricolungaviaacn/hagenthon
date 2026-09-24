import { useState, useEffect } from 'react';
import type { StepInfo, CompletedStep } from '../types';
import { uploadDocument, confirmStep } from '../api/session';
import DocumentUpload from './DocumentUpload';

interface Props {
  sessionId: string;
  step: StepInfo;
  totalSteps: number;
  onStepComplete: (completed: CompletedStep, response: { nextStep: number | null; isCompleted: boolean }) => void;
}

export default function StepWizard({ sessionId, step, totalSteps, onStepComplete }: Props) {
  const [extractedValue, setExtractedValue] = useState('');
  const [editedValue, setEditedValue] = useState('');
  const [uploadLoading, setUploadLoading] = useState(false);
  const [confirmLoading, setConfirmLoading] = useState(false);
  const [showUploadNew, setShowUploadNew] = useState(false);
  const [error, setError] = useState('');
  const [phase, setPhase] = useState<'initial' | 'extracted' | 'confirming'>('initial');

  useEffect(() => {
    setExtractedValue('');
    setEditedValue('');
    setError('');
    setShowUploadNew(false);
    setPhase('initial');
  }, [step.stepIndex]);

  const handleFileUpload = async (file: File) => {
    setError('');
    setUploadLoading(true);
    try {
      const res = await uploadDocument(sessionId, file);
      setExtractedValue(res.extractedValue);
      setEditedValue(res.extractedValue);
      setPhase('extracted');
    } catch {
      setError('Non è stato possibile leggere il documento. Assicurati che il file sia leggibile e riprova.');
    } finally {
      setUploadLoading(false);
    }
  };

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

  const stepLabel = `Passo ${step.stepIndex + 1} di ${totalSteps}`;

  // Automatic step (no document required, value already known)
  if (step.isAutomatic || (!step.documentRequired && step.previewValue)) {
    return (
      <div className="step-card">
        <span className="step-badge">{stepLabel}</span>
        <h3>{step.stepName}</h3>
        <p>Il valore per questo campo è già disponibile nel tuo documento. Controlla che sia corretto e premi Avanti.</p>
        {error && <div className="alert alert-error">{error}</div>}
        <div className="value-box">
          <div className="value-box-label">Valore rilevato</div>
          <div className="value-box-value">{step.previewValue}</div>
        </div>
        <div className="step-actions">
          <button
            className="btn btn-primary"
            style={{ minHeight: '52px', fontSize: '17px' }}
            onClick={() => handleConfirm(step.previewValue)}
            disabled={confirmLoading}
          >
            {confirmLoading ? <span className="spinner" /> : 'Avanti'}
          </button>
        </div>
      </div>
    );
  }

  // Already uploaded — show existing value
  if (step.alreadyUploaded && !showUploadNew) {
    return (
      <div className="step-card">
        <span className="step-badge">{stepLabel}</span>
        <h3>{step.stepName}</h3>
        <p>Hai già caricato un documento per questo passo. Puoi confermare il valore oppure caricare un documento aggiornato.</p>
        {error && <div className="alert alert-error">{error}</div>}
        <div className="value-box">
          <div className="value-box-label">Valore estratto dal documento</div>
          <div className="value-box-value">{step.previewValue}</div>
        </div>
        <div className="step-actions">
          <button
            className="btn btn-secondary"
            onClick={() => setShowUploadNew(true)}
            disabled={confirmLoading}
          >
            Ricarica documento
          </button>
          <button
            className="btn btn-primary"
            style={{ minHeight: '52px', fontSize: '17px' }}
            onClick={() => handleConfirm(step.previewValue)}
            disabled={confirmLoading}
          >
            {confirmLoading ? <span className="spinner" /> : 'Conferma'}
          </button>
        </div>
      </div>
    );
  }

  // Normal upload flow
  return (
    <div className="step-card">
      <span className="step-badge">{stepLabel}</span>
      <h3>{step.stepName}</h3>
      <p>{step.documentDescription || 'Carica il documento richiesto per continuare.'}</p>
      {error && <div className="alert alert-error">{error}</div>}

      {phase === 'initial' || (showUploadNew && phase !== 'extracted') ? (
        <DocumentUpload
          onFile={handleFileUpload}
          loading={uploadLoading}
          description={step.documentDescription}
        />
      ) : null}

      {phase === 'extracted' && (
        <>
          <div className="value-box">
            <div className="value-box-label">Valore estratto dal documento</div>
            <div className="value-box-value" style={{ marginBottom: '10px' }}>{extractedValue}</div>
          </div>
          <p style={{ marginBottom: '8px', marginTop: 0 }}>Se il valore non è corretto, puoi modificarlo qui sotto:</p>
          <input
            className="value-input"
            value={editedValue}
            onChange={(e) => setEditedValue(e.target.value)}
            aria-label="Valore da confermare"
          />
          <div className="step-actions">
            <button
              className="btn btn-secondary"
              onClick={() => {
                setPhase('initial');
                setShowUploadNew(true);
              }}
              disabled={confirmLoading}
            >
              Ricarica
            </button>
            <button
              className="btn btn-primary"
              style={{ minHeight: '52px', fontSize: '17px' }}
              onClick={() => handleConfirm(editedValue)}
              disabled={confirmLoading || !editedValue.trim()}
            >
              {confirmLoading ? <span className="spinner" /> : 'Conferma'}
            </button>
          </div>
        </>
      )}
    </div>
  );
}
