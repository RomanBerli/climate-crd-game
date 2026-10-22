import React, { useState } from 'react';
import { AdminSessionResponse } from '../../types/game';
import { formatFr } from '../../utils/currency';
import { CsvExportButton } from './CsvExportButton';

interface LiveSessionCardProps {
  session: AdminSessionResponse;
  adminKey: string;
  onStart: (sessionId: string) => Promise<void>;
  onAdvanceRound: (sessionId: string) => Promise<void>;
}

const PHASE_LABELS: Record<string, string> = {
  CONTRACT_JOIN: 'Abkommen-Beitritt (vor Runde 1)',
  CONTRACT_REVIEW: 'Zwischenrunde (Abkommen)',
  CONTRACT_VOTE: 'Abstimmung (Abkommen)',
  INVESTMENT: 'Einzahlungsrunde',
};

export const LiveSessionCard: React.FC<LiveSessionCardProps> = ({
  session,
  adminKey,
  onStart,
  onAdvanceRound,
}) => {
  const [busy, setBusy] = useState<boolean>(false);

  const canStart =
    session.status === 'LOBBY' && session.connectedPlayersCount >= session.groupSize;

  const isLastRound = session.currentRound >= session.totalRounds;

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
              <span className="badge badge--accent">
                Modus: {session.gameMode === 'WITH_CONTRACTS' ? 'Mit Verträgen' : 'Normal'}
              </span>
              {session.gameMode === 'WITH_CONTRACTS' && session.status === 'RUNNING' && (
                <span className="badge badge--neutral">
                  {PHASE_LABELS[session.roundPhase] || session.roundPhase} ({session.contractMemberCount} im Abkommen)
                </span>
              )}
              <span className="badge badge--risk">
                Risiko: {session.riskPercent}%
              </span>
              {session.status !== 'LOBBY' && (
                <span className="badge badge--neutral">
                  Runde {session.currentRound} / {session.totalRounds}
                </span>
              )}
            </div>
            <p className="admin-session-card__meta">
              Spieler verbunden: <strong>{session.connectedPlayersCount} / {session.groupSize}</strong>
              {session.status === 'RUNNING' && (
                <>
                  {' '}
                  • Bereit in Runde {session.currentRound}:{' '}
                  <strong>
                    {session.readyPlayersCount} / {session.groupSize} Spieler
                  </strong>
                </>
              )}
              {session.status === 'FINISHED' && (
                <>
                  {' '}
                  • Fragebogen ausgefüllt:{' '}
                  <strong>
                    {session.readyPlayersCount} / {session.groupSize} Spieler
                  </strong>
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
              className="btn btn--primary"
              disabled={!session.canAdvanceRound || busy}
              onClick={() => runAction(() => onAdvanceRound(session.id))}
              title={
                session.canAdvanceRound
                  ? isLastRound
                    ? 'Runde 10 abschliessen und Experiment beenden'
                    : `Runde ${session.currentRound + 1} starten`
                  : session.roundPhase !== 'INVESTMENT'
                  ? `Spieler befinden sich noch in der Abkommen-Phase (${PHASE_LABELS[session.roundPhase]})`
                  : `Warten bis alle ${session.groupSize} Spieler gewählt haben (${session.readyPlayersCount}/${session.groupSize})`
              }
            >
              {session.roundPhase !== 'INVESTMENT'
                ? `⏳ ${PHASE_LABELS[session.roundPhase]} (${session.readyPlayersCount}/${session.groupSize})`
                : isLastRound
                ? `🏁 Experiment abschliessen (${session.readyPlayersCount}/${session.groupSize} bereit)`
                : `⏭ Nächste Runde (${session.readyPlayersCount}/${session.groupSize} bereit)`}
            </button>
          )}

          <CsvExportButton
            sessionId={session.id}
            sessionCode={session.code}
            adminKey={adminKey}
          />
        </div>
      </div>

      {session.status === 'FINISHED' && (
        <div className="admin-pot-monitor">
          <div className="admin-pot-monitor__labels">
            <span>
              <strong>Endstand Klimakonto:</strong> {formatFr(session.totalPotRappen)} /{' '}
              {formatFr(session.targetSumRappen)}
            </span>
            <span className="admin-pot-monitor__outcome">
              {session.isGoalReached
                ? '✓ Klimaziel erreicht'
                : session.riskOccurred
                ? `✗ Ziel verfehlt & ${session.riskPercent}% Risiko eingetreten (Totalverlust)`
                : '⚠ Ziel verfehlt, aber Glück beim Risiko-Wurf (Guthaben erhalten)'}
            </span>
          </div>
        </div>
      )}

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
                  ? '✓ In Lobby'
                  : session.status === 'FINISHED'
                  ? p.isReadyCurrentRound
                    ? '✓ Fragebogen fertig'
                    : '📝 Fragebogen offen'
                  : p.isReadyCurrentRound
                  ? '✓ Hat gewählt'
                  : '⏳ Überlegt...'}
              </span>
            </div>
            {p.isConnected && session.gameMode === 'WITH_CONTRACTS' && session.status !== 'LOBBY' && (
              <div className="admin-player-box__details">
                <span>
                  {p.inContract
                    ? '📜 Im Abkommen'
                    : p.leftContract
                    ? 'Ausgetreten'
                    : 'Kein Abkommen'}
                </span>
              </div>
            )}
            {p.isConnected && session.status === 'FINISHED' && (
              <div className="admin-player-box__details">
                <span className="admin-player-box__payout">
                  Auszahlung: <strong>{formatFr(p.finalPayoutRappen ?? 0)}</strong>
                </span>
              </div>
            )}
          </div>
        ))}
      </div>
    </article>
  );
};
