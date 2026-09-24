import { useState, useEffect } from 'react';
import type { StepInfo, CompletedStep } from '../types';
import { uploadDocument, confirmStep } from '../api/session';
import DocumentUpload from './DocumentUpload';

interface Props {
  sessionId: string;
  step: StepInfo;
  onStepComplete: (completed: CompletedStep, response: { nextStep: number | null; isCompleted: boolean }) => void;
}

export default function StepWizard({ sessionId, step, onStepComplete }: Props) {
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
      setError('Errore durante il caricamento del documento. Riprova.');
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
      setError('Errore durante la conferma. Riprova.');
    } finally {
      setConfirmLoading(false);
    }
  };

  // Automatic step (no document required, value already known)
  if (step.isAutomatic || (!step.documentRequired && step.previewValue)) {
    return (
      <div className="step-card">
        <span className="step-badge">Valore automatico</span>
        <h3>{step.stepName}</h3>
        <p>Questo valore è stato calcolato automaticamente dal sistema.</p>
        {error && <div className="alert alert-error">{error}</div>}
        <div className="value-box">
          <div className="value-box-label">Valore rilevato</div>
          <div className="value-box-value">{step.previewValue}</div>
        </div>
        <div className="step-actions">
          <button
            className="btn btn-primary"
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
        <span className="step-badge">Documento già presente</span>
        <h3>{step.stepName}</h3>
        <p>Hai già caricato un documento per questo step.</p>
        {error && <div className="alert alert-error">{error}</div>}
        <div className="value-box">
          <div className="value-box-label">Valore estratto</div>
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
            onClick={() => handleConfirm(step.previewValue)}
            disabled={confirmLoading}
          >
            {confirmLoading ? <span className="spinner" /> : 'Conferma e vai avanti'}
          </button>
        </div>
      </div>
    );
  }

  // Normal upload flow
  return (
    <div className="step-card">
      <span className="step-badge">Step {step.stepIndex + 1}</span>
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
            <div className="value-box-label">Valore estratto</div>
            <div className="value-box-value" style={{ marginBottom: '10px' }}>{extractedValue}</div>
          </div>
          <p style={{ marginBottom: '8px', marginTop: 0 }}>Correggi se necessario:</p>
          <input
            className="value-input"
            value={editedValue}
            onChange={(e) => setEditedValue(e.target.value)}
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
              onClick={() => handleConfirm(editedValue)}
              disabled={confirmLoading || !editedValue.trim()}
            >
              {confirmLoading ? <span className="spinner" /> : 'Conferma e vai avanti'}
            </button>
          </div>
        </>
      )}
    </div>
  );
}
