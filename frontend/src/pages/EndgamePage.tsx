import React from 'react';
import { GameStateResponse } from '../types/game';
import { leaveLobby } from '../api/gameApi';

interface EndgamePageProps {
  gameState: GameStateResponse;
  onResetSession: () => Promise<void>;
}

/**
 * Statischer Milinski-Endbildschirm gemäss Spec 1.3, 2.1 & 6:
 * Zeigt Spielernummer, Session-Code, unmissverständlichen Ergebnis-Textblock
 * und den auszuzahlenden Endbetrag in Rappen gross und deutlich an.
 */
export const EndgamePage: React.FC<EndgamePageProps> = ({
  gameState,
  onResetSession,
}) => {
  const { player, sessionCode, endgame } = gameState;
  const payoutRappen = endgame ? endgame.finalPayoutRappen : player.finalPayoutRappen ?? 0;

  const isSuccess = endgame?.isGoalReached === true;
  const isLuckySurvival = endgame?.isGoalReached === false && endgame?.riskOccurred === false;

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
            <span className="endgame-id-box__label">Risiko-Treatment</span>
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
          <strong className="endgame-payout-display__amount">{payoutRappen} Rappen</strong>
          <div className="endgame-payout-display__breakdown">
            <span>Startkapital: {player.initialBalanceRappen} Rappen</span>
            <span>•</span>
            <span>Ihre getätigten Einzahlungen: {player.ownTotalInvestedRappen} Rappen</span>
            <span>•</span>
            <span>Restguthaben vor Risiko-Prüfung: {player.currentBalanceRappen} Rappen</span>
          </div>
        </div>

        <div className="endgame-card__footer">
          <button type="button" className="btn btn--ghost" onClick={handleNewSession}>
            Sitzung nach Auszahlung schliessen (Neuer Raum)
          </button>
        </div>
      </section>
    </main>
  );
};
