import React, { useState } from 'react';
import {
  advanceAdminRound,
  createAdminSession,
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
  const [actionError, setActionError] = useState<string | null>(null);
  const { sessions, loading, error, refreshSessions } = useAdminPolling(adminKey, true);

  const handleKeyChange = (val: string) => {
    setAdminKey(val);
    setStoredAdminSecret(val);
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

  return (
    <main className="page-container">
      <header className="admin-header">
        <div>
          <span className="badge badge--accent">Versuchsleiter-Cockpit</span>
          <h1>Milinski Collective-Risk Game – Admin-Dashboard</h1>
        </div>
        <div className="admin-header__controls">
          <div className="admin-key-box">
            <label htmlFor="adminKeyInput">Admin-Schlüssel (X-Admin-Key):</label>
            <input
              id="adminKeyInput"
              type="password"
              value={adminKey}
              onChange={(e) => handleKeyChange(e.target.value)}
              placeholder="admin_geheim_123"
            />
          </div>
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
