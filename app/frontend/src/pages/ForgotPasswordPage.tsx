import { useState, FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { forgotPassword } from '../api/auth';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [sent, setSent] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await forgotPassword(email);
      setSent(true);
    } catch {
      setError('Errore durante la richiesta. Riprova più tardi.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-layout">
      <div className="auth-card">
        <div className="auth-logo">730 Facile</div>
        <div className="auth-subtitle">Verifica il tuo modello precompilato</div>
        <h1 className="auth-title">Recupera password</h1>

        {sent ? (
          <div className="alert alert-success">
            Se l'email è registrata, riceverai le istruzioni per reimpostare la password. Controlla la tua casella.
          </div>
        ) : (
          <>
            {error && <div className="alert alert-error">{error}</div>}
            <p style={{ fontSize: '14px', color: 'var(--text-secondary)', marginBottom: '24px' }}>
              Inserisci la tua email. Ti invieremo un link per reimpostare la password.
            </p>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label className="form-label" htmlFor="email">Email</label>
                <input
                  id="email"
                  type="email"
                  className="form-input"
                  placeholder="nome@esempio.it"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  autoComplete="email"
                />
              </div>
              <button type="submit" className="btn btn-primary" disabled={loading}>
                {loading ? <span className="spinner" /> : 'Invia link di recupero'}
              </button>
            </form>
          </>
        )}

        <div className="auth-links" style={{ marginTop: '20px' }}>
          <Link to="/login">← Torna al login</Link>
        </div>
      </div>
    </div>
  );
}
