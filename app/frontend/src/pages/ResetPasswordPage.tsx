import { useState, FormEvent } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { resetPassword } from '../api/auth';

export default function ResetPasswordPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') ?? '';

  const [newPassword, setNewPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (newPassword !== confirm) {
      setError('Le password non coincidono.');
      return;
    }
    if (newPassword.length < 8) {
      setError('La password deve essere di almeno 8 caratteri.');
      return;
    }
    setLoading(true);
    try {
      await resetPassword(token, newPassword);
      navigate('/login');
    } catch {
      setError('Link non valido o scaduto. Richiedi un nuovo link.');
    } finally {
      setLoading(false);
    }
  };

  if (!token) {
    return (
      <div className="auth-layout">
        <div className="auth-card">
          <div className="auth-logo">730 Facile</div>
          <div className="alert alert-error">Token mancante. Richiedi un nuovo link di recupero.</div>
          <div className="auth-links"><Link to="/forgot-password">Recupera password</Link></div>
        </div>
      </div>
    );
  }

  return (
    <div className="auth-layout">
      <div className="auth-card">
        <div className="auth-logo">730 Facile</div>
        <div className="auth-subtitle">Verifica il tuo modello precompilato</div>
        <h1 className="auth-title">Nuova password</h1>

        {error && <div className="alert alert-error">{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label" htmlFor="newPassword">Nuova password</label>
            <input
              id="newPassword"
              type="password"
              className="form-input"
              placeholder="Min. 8 caratteri"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              required
              autoComplete="new-password"
            />
          </div>
          <div className="form-group">
            <label className="form-label" htmlFor="confirm">Conferma password</label>
            <input
              id="confirm"
              type="password"
              className="form-input"
              placeholder="Ripeti la password"
              value={confirm}
              onChange={(e) => setConfirm(e.target.value)}
              required
              autoComplete="new-password"
            />
          </div>
          <button type="submit" className="btn btn-primary" disabled={loading} style={{ marginTop: '8px' }}>
            {loading ? <span className="spinner" /> : 'Aggiorna password'}
          </button>
        </form>
      </div>
    </div>
  );
}
