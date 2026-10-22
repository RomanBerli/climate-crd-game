import React from 'react';
import { GameStateResponse } from '../../types/game';
import { formatFr } from '../../utils/currency';

interface RoundHeaderProps {
  gameState: GameStateResponse;
}

export const RoundHeader: React.FC<RoundHeaderProps> = ({ gameState }) => {
  const {
    sessionCode,
    currentRound,
    totalRounds,
    riskPercent,
    targetSumRappen,
    totalPotRappen,
    player,
  } = gameState;

  return (
    <header className="round-header">
      <div className="round-header__identity">
        <span className="badge badge--accent">Spieler {player.playerNumber}</span>
        <span className="badge badge--neutral">Raum {sessionCode}</span>
        <span className="badge badge--risk" title="Wahrscheinlichkeit des Totalverlusts bei Verfehlen des Klimaziels">
          Risiko: {riskPercent}%
        </span>
      </div>

      <div className="round-header__round">
        <span className="round-header__label">Aktuelle Runde</span>
        <strong className="round-header__value">
          {currentRound} <span className="round-header__total">/ {totalRounds}</span>
        </strong>
      </div>

      <div className="round-header__metrics">
        <div className="metric-box">
          <span className="metric-box__label">Klimafonds Total (nach Runde {Math.max(0, currentRound - 1)})</span>
          <strong className="metric-box__value">
            {formatFr(totalPotRappen)} / {formatFr(targetSumRappen)}
          </strong>
        </div>
        <div className="metric-box metric-box--highlight">
          <span className="metric-box__label">Ihr aktuelles Restguthaben</span>
          <strong className="metric-box__value metric-box__value--balance">
            {formatFr(player.currentBalanceRappen)}
          </strong>
        </div>
      </div>
    </header>
  );
};
