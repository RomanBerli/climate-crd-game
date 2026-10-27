import React, { useState } from 'react';
import { GameStateResponse } from '../types/game';
import { submitContractLeave, submitInvestment } from '../api/gameApi';
import { useLocalNotepad } from '../hooks/useLocalNotepad';
import { RoundHeader } from '../components/player/RoundHeader';
import { ContractPhaseCard } from '../components/player/ContractPhaseCard';
import { InvestmentButtons } from '../components/player/InvestmentButtons';
import { PlayerStatusList } from '../components/player/PlayerStatusList';
import { HistoryTable } from '../components/player/HistoryTable';
import { LocalNotepad } from '../components/player/LocalNotepad';

interface GamePageProps {
  gameState: GameStateResponse;
  onStateUpdated: (newState: GameStateResponse) => void;
}

export const GamePage: React.FC<GamePageProps> = ({
  gameState,
  onStateUpdated,
}) => {
  const [investError, setInvestError] = useState<string | null>(null);
  const { notes, setNotes } = useLocalNotepad(
    gameState.sessionId,
    gameState.player.playerNumber
  );

  const handleInvest = async (amountRappen: number) => {
    setInvestError(null);
    try {
      const updated = await submitInvestment(amountRappen);
      onStateUpdated(updated);
    } catch (err) {
      setInvestError(err instanceof Error ? err.message : 'Fehler beim Speichern des Beitrags.');
    }
  };

  const handleLeaveContract = async () => {
    setInvestError(null);
    try {
      const updated = await submitContractLeave();
      onStateUpdated(updated);
    } catch (err) {
      setInvestError(err instanceof Error ? err.message : 'Fehler beim Verlassen des Abkommens.');
    }
  };

  const allPlayersReady = gameState.playersStatus.every((slot) => slot.isReady);

  return (
    <div className="game-layout">
      <RoundHeader gameState={gameState} />

      <div className="game-main-grid">
        <div className="game-main-grid__left">
          {investError && <div className="alert alert--error">{investError}</div>}

          {gameState.roundPhase !== 'INVESTMENT' ? (
            <ContractPhaseCard
              gameState={gameState}
              onStateUpdated={onStateUpdated}
            />
          ) : (
            <InvestmentButtons
              initialBalanceRappen={gameState.player.initialBalanceRappen}
              currentBalanceRappen={gameState.player.currentBalanceRappen}
              allowedOptions={gameState.player.allowedInvestmentOptions}
              hasSubmitted={gameState.player.hasSubmittedCurrentRound}
              submittedAmount={gameState.player.currentRoundInvestmentRappen}
              allPlayersReady={allPlayersReady}
              inContract={gameState.player.inContract}
              canLeaveContract={gameState.contractState?.canLeaveContractNow ?? false}
              minInvestmentRappen={gameState.contractState?.minInvestmentRappen ?? 0}
              lastContractEvent={gameState.contractState?.lastContractEvent ?? null}
              onInvest={handleInvest}
              onLeaveContract={handleLeaveContract}
            />
          )}

          <HistoryTable
            history={gameState.history}
            myPlayerNumber={gameState.player.playerNumber}
            groupSize={gameState.groupSize}
            initialBalanceRappen={gameState.player.initialBalanceRappen}
          />

          <PlayerStatusList
            playersStatus={gameState.playersStatus}
            myPlayerNumber={gameState.player.playerNumber}
            gameMode={gameState.gameMode}
          />
        </div>

        <div className="game-main-grid__right">
          <LocalNotepad notes={notes} onChange={setNotes} />
        </div>
      </div>
    </div>
  );
};
