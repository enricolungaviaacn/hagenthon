import { useState, FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { register } from '../api/auth';

export default function RegisterPage() {
  const navigate = useNavigate();
  const [nome, setNome] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (password.length < 8) {
      setError('La password deve essere di almeno 8 caratteri.');
      return;
    }
    setLoading(true);
    try {
      await register({ nome, email, password });
      navigate('/login');
    } catch {
      setError("Registrazione fallita. L'email potrebbe essere già in uso.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-layout">
      <div className="auth-card">
        <div className="auth-logo">730 Facile</div>
        <div className="auth-subtitle">Verifica il tuo modello precompilato</div>
        <h1 className="auth-title">Crea account</h1>

        {error && <div className="alert alert-error">{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label" htmlFor="nome">Nome completo</label>
            <input
              id="nome"
              type="text"
              className="form-input"
              placeholder="Mario Rossi"
              value={nome}
              onChange={(e) => setNome(e.target.value)}
              required
              autoComplete="name"
            />
          </div>
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
          <div className="form-group">
            <label className="form-label" htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              className="form-input"
              placeholder="Min. 8 caratteri"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              autoComplete="new-password"
            />
          </div>
          <button type="submit" className="btn btn-primary" disabled={loading} style={{ marginTop: '8px' }}>
            {loading ? <span className="spinner" /> : 'Crea account'}
          </button>
        </form>

        <div className="auth-links">
          Hai già un account?{' '}
          <Link to="/login">Accedi</Link>
        </div>
      </div>
    </div>
  );
}
