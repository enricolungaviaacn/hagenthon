import { useState, useEffect, useRef, DragEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { getSessions, uploadPdf } from '../api/session';
import type { Session } from '../types';

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('it-IT', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  });
}

function statusLabel(status: Session['status']) {
  switch (status) {
    case 'IN_PROGRESS': return 'In corso';
    case 'COMPLETED': return 'Completato';
    case 'SUBMITTED': return 'Inviato';
  }
}

function statusClass(status: Session['status']) {
  switch (status) {
    case 'IN_PROGRESS': return 'status-in-progress';
    case 'COMPLETED': return 'status-completed';
    case 'SUBMITTED': return 'status-submitted';
  }
}

export default function DashboardPage() {
  const navigate = useNavigate();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [sessions, setSessions] = useState<Session[]>([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [dragOver, setDragOver] = useState(false);
  const [error, setError] = useState('');

  const user = JSON.parse(localStorage.getItem('user') ?? '{}') as { nome?: string; email?: string };

  useEffect(() => {
    getSessions()
      .then(setSessions)
      .catch(() => setError('Impossibile caricare le sessioni.'))
      .finally(() => setLoading(false));
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
  };

  const handleFile = async (file: File) => {
    if (!file.name.endsWith('.pdf')) {
      setError('Carica un file PDF valido.');
      return;
    }
    setError('');
    setUploading(true);
    try {
      const res = await uploadPdf(file);
      navigate(`/session/${res.sessionId}`);
    } catch {
      setError('Errore durante il caricamento del PDF. Riprova.');
    } finally {
      setUploading(false);
    }
  };

  const onDrop = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setDragOver(false);
    const file = e.dataTransfer.files[0];
    if (file) handleFile(file);
  };

  const onDragOver = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setDragOver(true);
  };

  const onDragLeave = () => setDragOver(false);

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <span className="dashboard-logo">730 Facile</span>
        <div className="dashboard-user">
          <span>Ciao, <strong>{user.nome ?? user.email}</strong></span>
          <button className="btn btn-ghost" style={{ padding: '8px 14px', fontSize: '13px' }} onClick={handleLogout}>
            Esci
          </button>
        </div>
      </header>

      <main className="dashboard-content">
        <div className="dashboard-hero">
          <h1>La tua dichiarazione 730</h1>
          <p>Carica il tuo 730 precompilato per avviare la verifica guidata.</p>
        </div>

        {error && <div className="alert alert-error" style={{ marginBottom: '24px' }}>{error}</div>}

        <div
          className={`upload-zone ${dragOver ? 'drag-over' : ''}`}
          onDrop={onDrop}
          onDragOver={onDragOver}
          onDragLeave={onDragLeave}
          onClick={() => !uploading && fileInputRef.current?.click()}
          role="button"
          tabIndex={0}
          onKeyDown={(e) => e.key === 'Enter' && fileInputRef.current?.click()}
        >
          <input
            ref={fileInputRef}
            type="file"
            accept=".pdf"
            style={{ display: 'none' }}
            onChange={(e) => {
              const file = e.target.files?.[0];
              if (file) handleFile(file);
            }}
          />
          <div className="upload-zone-icon">📄</div>
          {uploading ? (
            <p>Caricamento in corso… <span className="spinner" /></p>
          ) : (
            <>
              <p>Trascina qui il tuo 730 precompilato in formato PDF</p>
              <button
                className="btn btn-primary"
                style={{ width: 'auto', padding: '10px 24px' }}
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  fileInputRef.current?.click();
                }}
              >
                Seleziona file
              </button>
            </>
          )}
        </div>

        <div className="sessions-section">
          <h2>Sessioni precedenti</h2>
          {loading ? (
            <div className="empty-state"><span className="spinner" /></div>
          ) : sessions.length === 0 ? (
            <div className="empty-state">Nessuna sessione precedente. Carica il tuo primo 730!</div>
          ) : (
            <div className="sessions-list">
              {sessions.map((s) => (
                <Link key={s.id} to={`/session/${s.id}`} className="session-card">
                  <div className="session-card-info">
                    <h3>Dichiarazione del {formatDate(s.createdAt)}</h3>
                    <p>{s.currentStepName}</p>
                  </div>
                  <span className={`session-status ${statusClass(s.status)}`}>
                    {statusLabel(s.status)}
                  </span>
                </Link>
              ))}
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
