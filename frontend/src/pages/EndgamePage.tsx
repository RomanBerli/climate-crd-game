import React from 'react';
import { GameStateResponse } from '../types/game';
import { leaveLobby } from '../api/gameApi';
import { formatFr } from '../utils/currency';

interface EndgamePageProps {
  gameState: GameStateResponse;
  onResetSession: () => Promise<void>;
}

/**
 * Statischer Milinski-Endbildschirm:
 * Zeigt Spielernummer, Session-Code, unmissverständlichen Ergebnis-Textblock,
 * den auszuzahlenden Endbetrag in Franken sowie die vollständige Tabelle aller Rundenbeiträge aller Spieler.
 */
export const EndgamePage: React.FC<EndgamePageProps> = ({
  gameState,
  onResetSession,
}) => {
  const { player, sessionCode, endgame, history, groupSize } = gameState;
  const payoutRappen = endgame ? endgame.finalPayoutRappen : player.finalPayoutRappen ?? 0;

  const isSuccess = endgame?.isGoalReached === true;
  const isLuckySurvival = endgame?.isGoalReached === false && endgame?.riskOccurred === false;
  const playerColumns = Array.from({ length: groupSize }, (_, i) => i + 1);

  const playerTotals = playerColumns.map((pNum) =>
    history.reduce((sum, roundRow) => {
      const inv = roundRow.investments.find((entry) => entry.playerNumber === pNum);
      return sum + (inv ? inv.amountRappen : 0);
    }, 0)
  );

  const handleNewSession = async () => {
    await leaveLobby();
    await onResetSession();
  };

  return (
    <main className="page-container">
      <section className="endgame-card">
        <div className="endgame-card__identifiers">
          <div className="endgame-id-box">
            <span className="endgame-id-box__label">Ihre Identifikation</span>
            <strong className="endgame-id-box__value">Spieler {player.playerNumber}</strong>
          </div>
          <div className="endgame-id-box">
            <span className="endgame-id-box__label">Session-Code</span>
            <strong className="endgame-id-box__value">{sessionCode}</strong>
          </div>
          <div className="endgame-id-box">
            <span className="endgame-id-box__label">Risiko-Wahrscheinlichkeit</span>
            <strong className="endgame-id-box__value">{gameState.riskPercent}% Risiko</strong>
          </div>
        </div>

        <div
          className={`endgame-result-banner ${
            isSuccess
              ? 'endgame-result-banner--success'
              : isLuckySurvival
              ? 'endgame-result-banner--luck'
              : 'endgame-result-banner--loss'
          }`}
        >
          <h1>Experiment abgeschlossen (10 / 10 Runden)</h1>
          <p className="endgame-result-banner__message">
            {endgame?.message ||
              'Das Experiment ist beendet. Bitte zeigen Sie diesen Bildschirm dem Versuchsleiter zur Auszahlung.'}
          </p>
        </div>

        <div className="endgame-payout-display">
          <span className="endgame-payout-display__label">
            Ihr finaler Auszahlungsbetrag (Spieler {player.playerNumber} • Raum {sessionCode})
          </span>
          <strong className="endgame-payout-display__amount">{formatFr(payoutRappen)}</strong>
          <div className="endgame-payout-display__breakdown">
            <span>
              Klimafonds Total: {formatFr(gameState.totalPotRappen)} / {formatFr(gameState.targetSumRappen)}
            </span>
            <span>•</span>
            <span>Startkapital: {formatFr(player.initialBalanceRappen)}</span>
            <span>•</span>
            <span>Ihre getätigten Einzahlungen: {formatFr(player.ownTotalInvestedRappen)}</span>
            <span>•</span>
            <span>Restguthaben vor Risiko-Prüfung: {formatFr(player.currentBalanceRappen)}</span>
          </div>
        </div>

        {history.length > 0 && (
          <div className="history-card" style={{ boxShadow: 'none' }}>
            <div className="history-card__header">
              <h3 className="section-title">Alle Einzahlungen aller Spieler (Runde 1 – {history.length})</h3>
            </div>
            <div className="history-table-wrapper">
              <table className="history-table">
                <thead>
                  <tr>
                    <th>Runde</th>
                    {playerColumns.map((pNum) => (
                      <th
                        key={pNum}
                        className={pNum === player.playerNumber ? 'history-table__col--me' : ''}
                      >
                        Spieler {pNum}
                        {pNum === player.playerNumber ? ' (Sie)' : ''}
                      </th>
                    ))}
                    <th>Runden-Total</th>
                  </tr>
                </thead>
                <tbody>
                  {history.map((roundEntry) => (
                    <tr key={roundEntry.round}>
                      <td className="history-table__round">Runde {roundEntry.round}</td>
                      {playerColumns.map((pNum) => {
                        const entry = roundEntry.investments.find(
                          (inv) => inv.playerNumber === pNum
                        );
                        const amount = entry ? entry.amountRappen : 0;
                        return (
                          <td
                            key={pNum}
                            className={`history-table__cell ${
                              pNum === player.playerNumber ? 'history-table__col--me' : ''
                            }`}
                          >
                            <span className="amount-pill amount-pill--20">
                              {formatFr(amount)}
                            </span>
                          </td>
                        );
                      })}
                      <td className="history-table__round">
                        <strong>+{formatFr(roundEntry.roundTotalRappen)}</strong>
                      </td>
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr style={{ borderTop: '2px solid var(--border-strong)', background: 'var(--bg-card-elevated)' }}>
                    <td className="history-table__round">Gesamt</td>
                    {playerColumns.map((pNum, idx) => (
                      <td
                        key={pNum}
                        className={`history-table__cell ${
                          pNum === player.playerNumber ? 'history-table__col--me' : ''
                        }`}
                      >
                        <strong>{formatFr(playerTotals[idx])}</strong>
                      </td>
                    ))}
                    <td className="history-table__round">
                      <strong>{formatFr(gameState.totalPotRappen)}</strong>
                    </td>
                  </tr>
                </tfoot>
              </table>
            </div>
          </div>
        )}

        <div className="endgame-card__footer">
          <button type="button" className="btn btn--ghost" onClick={handleNewSession}>
            Sitzung nach Auszahlung schliessen (Neuer Raum)
          </button>
        </div>
      </section>
    </main>
  );
};
