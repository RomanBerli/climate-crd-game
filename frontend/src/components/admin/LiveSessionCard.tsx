import React, { useState } from 'react';
import { AdminSessionResponse } from '../../types/game';
import { CsvExportButton } from './CsvExportButton';

interface LiveSessionCardProps {
  session: AdminSessionResponse;
  adminKey: string;
  onStart: (sessionId: string) => Promise<void>;
  onPause: (sessionId: string) => Promise<void>;
  onResume: (sessionId: string) => Promise<void>;
}

export const LiveSessionCard: React.FC<LiveSessionCardProps> = ({
  session,
  adminKey,
  onStart,
  onPause,
  onResume,
}) => {
  const [busy, setBusy] = useState<boolean>(false);

  const canStart =
    session.status === 'LOBBY' && session.connectedPlayersCount >= session.groupSize;

  const potPercent = Math.min(
    100,
    Math.round((session.totalPotRappen / session.targetSumRappen) * 100)
  );

  const runAction = async (fn: () => Promise<void>) => {
    if (busy) return;
    setBusy(true);
    try {
      await fn();
    } finally {
      setBusy(false);
    }
  };

  return (
    <article className={`admin-session-card admin-session-card--${session.status.toLowerCase()}`}>
      <div className="admin-session-card__top">
        <div className="admin-session-card__title-group">
          <div className="room-code-display">
            <span className="room-code-display__label">Raumcode</span>
            <strong className="room-code-display__code">{session.code}</strong>
          </div>
          <div>
            <div className="admin-session-card__badges">
              <span className={`badge badge--status-${session.status.toLowerCase()}`}>
                {session.status}
              </span>
              <span className="badge badge--risk">
                Treatment: {session.riskPercent}% Risiko
              </span>
              {session.status !== 'LOBBY' && (
                <span className="badge badge--neutral">
                  Runde {session.currentRound} / {session.totalRounds} ({session.roundPhase})
                </span>
              )}
            </div>
            <p className="admin-session-card__meta">
              Spieler verbunden: <strong>{session.connectedPlayersCount} / {session.groupSize}</strong>
              {(session.status === 'RUNNING' || session.status === 'PAUSED') && (
                <>
                  {' '}
                  • Restzeit Runde: <strong>{session.remainingSeconds}s</strong>
                </>
              )}
            </p>
          </div>
        </div>

        <div className="admin-session-card__actions">
          {session.status === 'LOBBY' && (
            <button
              type="button"
              className="btn btn--primary"
              disabled={!canStart || busy}
              onClick={() => runAction(() => onStart(session.id))}
              title={
                canStart
                  ? 'Experiment jetzt starten'
                  : `Warten auf ${session.groupSize} Spieler (${session.connectedPlayersCount}/${session.groupSize} verbunden)`
              }
            >
              ▶ Spiel starten ({session.connectedPlayersCount}/{session.groupSize})
            </button>
          )}

          {session.status === 'RUNNING' && (
            <button
              type="button"
              className="btn btn--warning"
              disabled={busy}
              onClick={() => runAction(() => onPause(session.id))}
            >
              ⏸ Pause (Freeze)
            </button>
          )}

          {session.status === 'PAUSED' && (
            <button
              type="button"
              className="btn btn--primary"
              disabled={busy}
              onClick={() => runAction(() => onResume(session.id))}
            >
              ▶ Spiel fortsetzen
            </button>
          )}

          <CsvExportButton
            sessionId={session.id}
            sessionCode={session.code}
            adminKey={adminKey}
          />
        </div>
      </div>

      {/* Nur für den Versuchsleiter sichtbar: Kollektives Klimakonto */}
      <div className="admin-pot-monitor">
        <div className="admin-pot-monitor__labels">
          <span>
            <strong>Klimakonto (Nur für Admin sichtbar):</strong> {session.totalPotRappen} /{' '}
            {session.targetSumRappen} Rappen ({potPercent}%)
          </span>
          {session.status === 'FINISHED' && (
            <span className="admin-pot-monitor__outcome">
              {session.isGoalReached
                ? '✓ Klimaziel erreicht'
                : session.riskOccurred
                ? `✗ Ziel verfehlt & ${session.riskPercent}% Risiko eingetreten (Totalverlust)`
                : '⚠ Ziel verfehlt, aber Glück beim Risiko-Wurf (Guthaben erhalten)'}
            </span>
          )}
        </div>
        <div className="admin-pot-monitor__bar">
          <div
            className="admin-pot-monitor__fill"
            style={{ width: `${potPercent}%` }}
          />
        </div>
      </div>

      <div className="admin-players-grid">
        {session.players.map((p) => (
          <div
            key={p.playerNumber}
            className={`admin-player-box ${
              p.isConnected ? 'admin-player-box--connected' : 'admin-player-box--empty'
            }`}
          >
            <div className="admin-player-box__header">
              <strong>Spieler {p.playerNumber}</strong>
              <span
                className={`status-indicator ${
                  !p.isConnected
                    ? 'status-indicator--offline'
                    : p.isReadyCurrentRound
                    ? 'status-indicator--ready'
                    : 'status-indicator--thinking'
                }`}
              >
                {!p.isConnected
                  ? 'Frei'
                  : session.status === 'LOBBY'
                  ? 'In Lobby'
                  : p.isReadyCurrentRound
                  ? `Bereit (${p.currentRoundInvestmentRappen ?? 0} Rp.)`
                  : 'Wartet...'}
              </span>
            </div>
            {p.isConnected && (
              <div className="admin-player-box__details">
                <span>Restguthaben: <strong>{p.currentBalanceRappen} Rp.</strong></span>
                <span>Eingezahlt: {p.totalInvestedRappen} Rp.</span>
                {session.status === 'FINISHED' && (
                  <span className="admin-player-box__payout">
                    Auszahlung: <strong>{p.finalPayoutRappen ?? 0} Rappen</strong>
                  </span>
                )}
              </div>
            )}
          </div>
        ))}
      </div>
    </article>
  );
};
