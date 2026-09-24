import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { getCurrentStep, submitSession } from '../api/session';
import type { StepInfo, CompletedStep } from '../types';
import PdfViewer from '../components/PdfViewer';
import StepWizard from '../components/StepWizard';
import StepChecklist from '../components/StepChecklist';

const TOTAL_STEPS = 6;

export default function SessionPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const sessionId = id ?? '';

  const token = localStorage.getItem('token') ?? '';
  const pdfUrl = `http://localhost:8080/api/sessions/${sessionId}/pdf`;

  const [step, setStep] = useState<StepInfo | null>(null);
  const [completedSteps, setCompletedSteps] = useState<CompletedStep[]>([]);
  const [isCompleted, setIsCompleted] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [downloadUrl, setDownloadUrl] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!sessionId) {
      navigate('/dashboard');
      return;
    }
    loadCurrentStep();
  }, [sessionId]);

  const loadCurrentStep = async () => {
    setLoading(true);
    setError('');
    try {
      const s = await getCurrentStep(sessionId);
      setStep(s);
    } catch {
      setError('Impossibile caricare lo step corrente.');
    } finally {
      setLoading(false);
    }
  };

  const handleStepComplete = (
    completed: CompletedStep,
    response: { nextStep: number | null; isCompleted: boolean }
  ) => {
    setCompletedSteps((prev) => [...prev, completed]);

    if (response.isCompleted) {
      setIsCompleted(true);
      return;
    }

    loadCurrentStep();
  };

  const handleSubmit = async () => {
    setSubmitting(true);
    setError('');
    try {
      const res = await submitSession(sessionId);
      setDownloadUrl(res.downloadUrl);
      setSubmitted(true);
    } catch {
      setError('Errore durante il submit. Riprova.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDownload = () => {
    window.open(`http://localhost:8080/api/sessions/${sessionId}/download`, '_blank');
  };

  return (
    <div className="session-layout">
      {/* Left panel: PDF viewer */}
      <div className="session-pdf-panel">
        <div className="session-pdf-header">
          730 Precompilato
        </div>
        <StepChecklist
          completedSteps={completedSteps}
          currentStepIndex={step?.stepIndex ?? 0}
          totalSteps={TOTAL_STEPS}
        />
        <PdfViewer url={pdfUrl} token={token} />
      </div>

      {/* Right panel: wizard */}
      <div className="session-wizard-panel">
        <div className="session-wizard-header">
          <Link to="/dashboard" className="back-link">
            ← Dashboard
          </Link>
          <h2>Verifica guidata</h2>
          <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>
            {completedSteps.length}/{TOTAL_STEPS} step
          </span>
        </div>

        <div className="session-wizard-content">
          {error && (
            <div className="alert alert-error" style={{ marginBottom: '20px' }}>{error}</div>
          )}

          {loading && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', color: 'var(--text-muted)' }}>
              <span className="spinner" />
              Caricamento step…
            </div>
          )}

          {!loading && isCompleted && !submitted && (
            <CompletionView
              completedSteps={completedSteps}
              submitting={submitting}
              onSubmit={handleSubmit}
            />
          )}

          {!loading && submitted && (
            <SubmittedView onDownload={handleDownload} />
          )}

          {!loading && !isCompleted && step && (
            <StepWizard
              sessionId={sessionId}
              step={step}
              onStepComplete={handleStepComplete}
            />
          )}
        </div>
      </div>
    </div>
  );
}

interface CompletionViewProps {
  completedSteps: CompletedStep[];
  submitting: boolean;
  onSubmit: () => void;
}

function CompletionView({ completedSteps, submitting, onSubmit }: CompletionViewProps) {
  return (
    <>
      <div className="completion-card">
        <h2>✓ Verifica completata</h2>
        <p style={{ color: 'var(--text-secondary)', marginBottom: '20px', fontSize: '14px' }}>
          Hai verificato tutti i campi. Di seguito il riepilogo dei valori confermati.
        </p>
        <div className="completion-values">
          {completedSteps.map((s) => (
            <div key={s.stepIndex} className="completion-row">
              <span className="completion-row-label">{s.stepName}</span>
              <span className="completion-row-value">{s.confirmedValue}</span>
            </div>
          ))}
        </div>
      </div>

      <div className="disclaimer">
        ⚠ Questi dati sono indicativi. I valori estratti automaticamente potrebbero contenere imprecisioni.
        Ti consigliamo di verificare con un CAF o un professionista fiscale prima di procedere all'invio definitivo.
      </div>

      <button
        className="btn btn-primary"
        style={{ width: '100%', padding: '14px' }}
        onClick={onSubmit}
        disabled={submitting}
      >
        {submitting ? <span className="spinner" /> : 'Invia e archivia'}
      </button>
    </>
  );
}

function SubmittedView({ onDownload }: { onDownload: () => void }) {
  return (
    <div className="completion-card">
      <h2 style={{ color: 'var(--accent)' }}>Dichiarazione inviata</h2>
      <p style={{ color: 'var(--text-secondary)', marginBottom: '24px', fontSize: '14px' }}>
        La tua dichiarazione è stata archiviata con successo. Puoi scaricare il riepilogo in formato HTML.
      </p>
      <button
        className="btn btn-primary"
        style={{ width: '100%', padding: '14px' }}
        onClick={onDownload}
      >
        Scarica riepilogo
      </button>
      <Link
        to="/dashboard"
        className="btn btn-secondary"
        style={{ display: 'block', textAlign: 'center', marginTop: '12px', padding: '12px', borderRadius: 'var(--radius)' }}
      >
        Torna alla dashboard
      </Link>
    </div>
  );
}
