import React from 'react';
import { GameStateResponse } from '../../types/game';
import { formatFr } from '../../utils/currency';

interface RoundHeaderProps {
  gameState: GameStateResponse;
}

export const RoundHeader: React.FC<RoundHeaderProps> = ({ gameState }) => {
  const {
    currentRound,
    totalRounds,
    targetSumRappen,
    player,
  } = gameState;

  return (
    <header className="round-header">
      <div className="round-header__identity">
        <span className="badge badge--accent">Spieler {player.playerNumber}</span>
      </div>

      <div className="round-header__round">
        <span className="round-header__label">Runde</span>
        <strong className="round-header__value">
          {currentRound} <span className="round-header__total">/ {totalRounds}</span>
        </strong>
      </div>

      <div className="round-header__metrics">
        <div className="metric-box">
          <span className="metric-box__label">Gruppenziel</span>
          <strong className="metric-box__value">{formatFr(targetSumRappen)}</strong>
        </div>
      </div>
    </header>
  );
};
