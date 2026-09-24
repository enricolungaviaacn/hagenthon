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
  const pdfUrl = `/api/sessions/${sessionId}/pdf`;

  const [step, setStep] = useState<StepInfo | null>(null);
  const [completedSteps, setCompletedSteps] = useState<CompletedStep[]>([]);
  const [isCompleted, setIsCompleted] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);
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
      setError('Non è stato possibile caricare il passo corrente. Controlla la connessione e riprova.');
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
      await submitSession(sessionId);
      setSubmitted(true);
    } catch {
      setError('Non è stato possibile salvare la dichiarazione. Controlla la connessione e riprova.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDownload = () => {
    window.open(`/api/sessions/${sessionId}/download`, '_blank');
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
          <span style={{ fontSize: '15px', color: 'var(--text-secondary)', fontWeight: 600 }}>
            {completedSteps.length} di {TOTAL_STEPS}
          </span>
        </div>

        {/* Barra di avanzamento */}
        <div>
          <div className="progress-label">
            <span>
              {isCompleted
                ? 'Tutti i passi completati'
                : step
                  ? `Passo ${step.stepIndex + 1} di ${TOTAL_STEPS}: ${step.stepName}`
                  : 'Caricamento…'}
            </span>
          </div>
          <div className="progress-bar-track">
            <div
              className="progress-bar-fill"
              style={{ width: `${(completedSteps.length / TOTAL_STEPS) * 100}%` }}
            />
          </div>
        </div>

        <div className="session-wizard-content">
          {error && (
            <div className="alert alert-error" style={{ marginBottom: '20px' }}>{error}</div>
          )}

          {loading && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', color: 'var(--text-muted)', fontSize: '16px' }}>
              <span className="spinner" />
              Caricamento in corso…
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
              totalSteps={TOTAL_STEPS}
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
        <h2>Verifica completata</h2>
        <p style={{ color: 'var(--text-secondary)', marginBottom: '20px', fontSize: '17px' }}>
          Ottimo lavoro! Hai controllato tutti i dati presenti nel tuo 730.
          Di seguito trovi il riepilogo dei valori che hai confermato.
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
        Attenzione: i valori qui mostrati sono stati letti dal documento caricato.
        Ti consigliamo di confrontarli con l'originale e, in caso di dubbio,
        di rivolgerti a un CAF o a un consulente fiscale prima di procedere.
      </div>

      <button
        className="btn btn-primary"
        style={{ width: '100%', padding: '16px', fontSize: '18px', minHeight: '56px' }}
        onClick={onSubmit}
        disabled={submitting}
      >
        {submitting ? <span className="spinner" /> : 'Salva e archivia'}
      </button>
    </>
  );
}

function SubmittedView({ onDownload }: { onDownload: () => void }) {
  return (
    <div className="completion-card">
      <h2 style={{ color: 'var(--accent)' }}>Tutto fatto!</h2>
      <p style={{ color: 'var(--text-secondary)', marginBottom: '24px', fontSize: '17px' }}>
        La tua dichiarazione è stata archiviata. Puoi scaricare il riepilogo per conservarlo.
        Se hai dubbi sui valori, puoi sempre rivolgerti a un CAF.
      </p>
      <button
        className="btn btn-primary"
        style={{ width: '100%', padding: '16px', fontSize: '18px', minHeight: '56px', marginBottom: '12px' }}
        onClick={onDownload}
      >
        Scarica riepilogo
      </button>
      <Link
        to="/dashboard"
        className="btn btn-secondary"
        style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', textAlign: 'center', padding: '14px', borderRadius: 'var(--radius)', fontSize: '16px', minHeight: '48px' }}
      >
        Torna alla pagina principale
      </Link>
    </div>
  );
}
