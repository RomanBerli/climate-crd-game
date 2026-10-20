import React from 'react';
import { GameStateResponse } from '../../types/game';

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
    player,
  } = gameState;

  return (
    <header className="round-header">
      <div className="round-header__identity">
        <span className="badge badge--accent">Spieler {player.playerNumber}</span>
        <span className="badge badge--neutral">Raum {sessionCode}</span>
        <span className="badge badge--risk" title="Wahrscheinlichkeit des Totalverlusts bei Verfehlen des Klimaziels">
          Risiko-Treatment: {riskPercent}%
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
          <span className="metric-box__label">Gruppenziel (nach {totalRounds} Runden)</span>
          <strong className="metric-box__value">{targetSumRappen} Rappen</strong>
        </div>
        <div className="metric-box metric-box--highlight">
          <span className="metric-box__label">Ihr aktuelles Restguthaben</span>
          <strong className="metric-box__value metric-box__value--balance">
            {player.currentBalanceRappen} Rappen
          </strong>
        </div>
      </div>
    </header>
  );
};
