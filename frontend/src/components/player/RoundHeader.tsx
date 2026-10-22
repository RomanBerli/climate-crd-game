import React from 'react';
import { GameStateResponse } from '../../types/game';
import { formatFr } from '../../utils/currency';

interface RoundHeaderProps {
  gameState: GameStateResponse;
}

export const RoundHeader: React.FC<RoundHeaderProps> = ({ gameState }) => {
  const {
    sessionCode,
    gameMode,
    contractState,
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
          Risiko: {riskPercent}%
        </span>
        {gameMode === 'WITH_CONTRACTS' && (
          <span className={`badge ${player.inContract ? 'badge--accent' : 'badge--neutral'}`}>
            {player.inContract
              ? `📜 Im Abkommen (${contractState.memberCount} Mitgl.)`
              : contractState.contractAbolished
              ? 'Abkommen aufgelöst'
              : player.leftContract
              ? 'Aus Abkommen ausgetreten'
              : 'Ohne Abkommen'}
          </span>
        )}
      </div>

      <div className="round-header__round">
        <span className="round-header__label">Aktuelle Runde</span>
        <strong className="round-header__value">
          {currentRound} <span className="round-header__total">/ {totalRounds}</span>
        </strong>
      </div>

      <div className="round-header__metrics">
        <div className="metric-box">
          <span className="metric-box__label">Gruppen-Klimaziel (10 Runden)</span>
          <strong className="metric-box__value">{formatFr(targetSumRappen)}</strong>
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
