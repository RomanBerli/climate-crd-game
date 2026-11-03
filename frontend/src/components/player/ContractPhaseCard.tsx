import React, { useState } from 'react';
import { ContractJoinChoice, GameStateResponse } from '../../types/game';
import { submitContractJoin } from '../../api/gameApi';

interface ContractPhaseCardProps {
  gameState: GameStateResponse;
  onStateUpdated: (newState: GameStateResponse) => void;
}

export const ContractPhaseCard: React.FC<ContractPhaseCardProps> = ({
  gameState,
  onStateUpdated,
}) => {
  const { contractState, player, groupSize } = gameState;
  const [busy, setBusy] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const minHalf = Math.ceil(groupSize / 2);

  const handleJoinChoice = async (choice: ContractJoinChoice) => {
    if (busy) return;
    setBusy(true);
    setError(null);
    try {
      const updated = await submitContractJoin(choice);
      onStateUpdated(updated);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Fehler bei der Abkommens-Entscheidung.');
    } finally {
      setBusy(false);
    }
  };

  const getDecisionLabel = (choice: ContractJoinChoice | null): string => {
    if (choice === 'YES') return 'Ja';
    if (choice === 'CONDITIONAL') return `Ab ${minHalf} Spielern`;
    if (choice === 'NO') return 'Nein';
    return player.inContract ? 'Ja' : 'Nein';
  };

  return (
    <section className="investment-card">
      <div className="investment-card__header">
        <h2>Abkommen beitreten</h2>
      </div>

      {error && <div className="alert alert--error">{error}</div>}

      {contractState.hasDecidedCurrentContractPhase ? (
        <div className="investment-confirmed">
          <div className="investment-confirmed__icon">✓</div>
          <div>
            <h3>Ihre Entscheidung: {getDecisionLabel(player.contractJoinChoice)}</h3>
            <p>
              Bitte warten Sie, bis alle {groupSize} Spieler über den Beitritt entschieden haben. Danach startet Runde 1 automatisch.
            </p>
          </div>
        </div>
      ) : (
        <div className="investment-grid" style={{ gridTemplateColumns: 'repeat(3, 1fr)' }}>
          <button
            type="button"
            className="invest-btn"
            disabled={busy}
            onClick={() => handleJoinChoice('YES')}
          >
            <span className="invest-btn__amount">Ja</span>
          </button>

          <button
            type="button"
            className="invest-btn"
            disabled={busy}
            onClick={() => handleJoinChoice('CONDITIONAL')}
          >
            <span className="invest-btn__amount">Ab {minHalf} Spielern</span>
          </button>

          <button
            type="button"
            className="invest-btn"
            disabled={busy}
            onClick={() => handleJoinChoice('NO')}
          >
            <span className="invest-btn__amount">Nein</span>
          </button>
        </div>
      )}
    </section>
  );
};
