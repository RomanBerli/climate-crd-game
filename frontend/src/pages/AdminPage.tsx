import React, { useEffect, useState } from 'react';
import {
  advanceAdminRound,
  createAdminSession,
  fetchAdminSessions,
  startAdminSession,
} from '../api/adminApi';
import { getStoredAdminSecret, setStoredAdminSecret } from '../api/client';
import { useAdminPolling } from '../hooks/useAdminPolling';
import { CreateSessionModal } from '../components/admin/CreateSessionModal';
import { LiveSessionCard } from '../components/admin/LiveSessionCard';

interface AdminPageProps {
  onBackToPlayerView: () => void;
}

export const AdminPage: React.FC<AdminPageProps> = ({ onBackToPlayerView }) => {
  const [adminKey, setAdminKey] = useState<string>(() => getStoredAdminSecret());
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(() => getStoredAdminSecret().length > 0);
  const [loginInput, setLoginInput] = useState<string>('');
  const [loginLoading, setLoginLoading] = useState<boolean>(false);
  const [loginError, setLoginError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const {
    sessions,
    loading,
    error,
    unauthorized,
    refreshSessions,
  } = useAdminPolling(adminKey, isAuthenticated);

  useEffect(() => {
    if (unauthorized) {
      setStoredAdminSecret(null);
      setAdminKey('');
      setIsAuthenticated(false);
      setLoginError('Sitzung abgelaufen oder ungültiges Admin-Secret. Bitte erneut anmelden.');
    }
  }, [unauthorized]);

  const handleLoginSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const candidate = loginInput.trim();
    if (!candidate) {
      setLoginError('Bitte das Admin-Secret eingeben.');
      return;
    }

    setLoginLoading(true);
    setLoginError(null);
    try {
      await fetchAdminSessions(candidate);
      setStoredAdminSecret(candidate);
      setAdminKey(candidate);
      setIsAuthenticated(true);
      setLoginInput('');
    } catch (err) {
      setStoredAdminSecret(null);
      setIsAuthenticated(false);
      setLoginError(
        err instanceof Error ? err.message : 'Authentifizierung fehlgeschlagen.'
      );
    } finally {
      setLoginLoading(false);
    }
  };

  const handleLogout = () => {
    setStoredAdminSecret(null);
    setAdminKey('');
    setIsAuthenticated(false);
    setLoginInput('');
  };

  const handleCreateSession = async (riskLevel: number) => {
    setActionError(null);
    try {
      await createAdminSession(riskLevel, adminKey);
      await refreshSessions();
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Fehler beim Erstellen der Session.');
    }
  };

  const handleStart = async (sessionId: string) => {
    setActionError(null);
    try {
      await startAdminSession(sessionId, adminKey);
      await refreshSessions();
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Fehler beim Spielstart.');
    }
  };

  const handleAdvanceRound = async (sessionId: string) => {
    setActionError(null);
    try {
      await advanceAdminRound(sessionId, adminKey);
      await refreshSessions();
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Fehler beim Weiterschalten der Runde.');
    }
  };

  if (!isAuthenticated) {
    return (
      <main className="page-container page-container--centered">
        <div className="lobby-login-card">
          <div className="lobby-login-card__header">
            <span className="badge badge--accent">Versuchsleiter-Authentifizierung</span>
            <h1>Admin-Dashboard Login</h1>
            <p>
              Der Zugriff auf das Versuchsleiter-Cockpit ist geschützt. Bitte geben Sie das konfigurierte{' '}
              <code>APP_ADMIN_SECRET</code> ein.
            </p>
          </div>

          <form onSubmit={handleLoginSubmit} className="lobby-form">
            <label htmlFor="adminSecretLoginInput" className="lobby-form__label">
              Admin-Secret
            </label>
            <input
              id="adminSecretLoginInput"
              type="password"
              value={loginInput}
              onChange={(e) => setLoginInput(e.target.value)}
              placeholder="Admin-Passwort eingeben..."
              className="lobby-form__input"
              style={{ fontSize: '1.25rem', letterSpacing: 'normal' }}
              autoFocus
            />

            {loginError && <div className="alert alert--error">{loginError}</div>}

            <button
              type="submit"
              className="btn btn--primary btn--large"
              disabled={loginLoading}
            >
              {loginLoading ? 'Prüfe Berechtigung...' : 'Anmelden →'}
            </button>
          </form>

          <div className="lobby-login-card__admin-link">
            <button type="button" className="btn btn--ghost" onClick={onBackToPlayerView}>
              ← Zurück zur Spieler-Ansicht
            </button>
          </div>
        </div>
      </main>
    );
  }

  return (
    <main className="page-container">
      <header className="admin-header">
        <div>
          <span className="badge badge--accent">Versuchsleiter-Cockpit</span>
          <h1>Milinski Collective-Risk Game – Admin-Dashboard</h1>
        </div>
        <div className="admin-header__controls">
          <button type="button" className="btn btn--ghost" onClick={handleLogout}>
            Abmelden
          </button>
          <button type="button" className="btn btn--secondary" onClick={onBackToPlayerView}>
            ← Zur Spieler-Ansicht
          </button>
        </div>
      </header>

      {(error || actionError) && (
        <div className="alert alert--error">{actionError || error}</div>
      )}

      <CreateSessionModal onCreateSession={handleCreateSession} />

      <section className="admin-sessions-list">
        <div className="admin-sessions-list__header">
          <h2>Aktive & Abgeschlossene Experiment-Sessions ({sessions.length})</h2>
          {loading && <span>Lade...</span>}
        </div>

        {sessions.length === 0 ? (
          <div className="history-empty">
            Noch keine Sessions vorhanden. Erstellen Sie oben einen neuen 6er-Gruppenraum.
          </div>
        ) : (
          sessions.map((session) => (
            <LiveSessionCard
              key={session.id}
              session={session}
              adminKey={adminKey}
              onStart={handleStart}
              onAdvanceRound={handleAdvanceRound}
            />
          ))
        )}
      </section>
    </main>
  );
};
