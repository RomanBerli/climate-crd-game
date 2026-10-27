import React, { useState } from 'react';
import { ContractJoinChoice, GameStateResponse } from '../../types/game';
import { submitContractJoin } from '../../api/gameApi';
import { formatFr } from '../../utils/currency';

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
    if (choice === 'YES') return 'Ja – Dem Abkommen beitreten';
    if (choice === 'CONDITIONAL') {
      return `Nur wenn min. die Hälfte (≥ ${minHalf} Spieler) dabei ist`;
    }
    if (choice === 'NO') return 'Nein – Nicht beitreten';
    return player.inContract ? 'Dem Abkommen beitreten' : 'Nicht beigetreten';
  };

  return (
    <section className="investment-card">
      <div className="investment-card__header">
        <h2>Freiwilliges Klimaabkommen (vor Runde 1)</h2>
        <span className="badge badge--accent">Vor Runde 1</span>
      </div>

      <div className="lobby-rules-summary" style={{ marginBottom: '1.25rem' }}>
        <h3>Regeln des Klimaabkommens</h3>
        <ul>
          <li>
            <strong>Mindestbeitrag für Mitglieder:</strong> Jedes Mitglied verpflichtet sich, pro Runde mindestens{' '}
            <strong>{formatFr(contractState.minInvestmentRappen)}</strong> einzuzahlen (0 Fr. ist für Mitglieder gesperrt).
          </li>
          <li>
            <strong>Bedingter Beitritt:</strong> Wählen Sie{' '}
            <em>«Nur wenn min. die Hälfte dabei»</em>, werden Sie nur dann Mitglied, wenn insgesamt mindestens{' '}
            <strong>{minHalf} von {groupSize} Spielern</strong> bereit sind beizutreten.
          </li>
          <li>
            <strong>Austritt:</strong> Nach der ersten Runde (ab Runde 2) können Mitglieder das Abkommen optional wieder verlassen.
          </li>
        </ul>
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
            <span className="invest-btn__desc">
              Verbindlich beitreten (Mindestbeitrag {formatFr(contractState.minInvestmentRappen)} pro Runde)
            </span>
            <span className="invest-btn__action">Dem Abkommen beitreten →</span>
          </button>

          <button
            type="button"
            className="invest-btn"
            disabled={busy}
            onClick={() => handleJoinChoice('CONDITIONAL')}
          >
            <span className="invest-btn__amount">Ab {minHalf} Spielern</span>
            <span className="invest-btn__desc">
              Nur beitreten, wenn min. die Hälfte (≥ {minHalf} von {groupSize} Spielern) dabei ist
            </span>
            <span className="invest-btn__action">Bedingt beitreten →</span>
          </button>

          <button
            type="button"
            className="invest-btn"
            disabled={busy}
            onClick={() => handleJoinChoice('NO')}
          >
            <span className="invest-btn__amount">Nein</span>
            <span className="invest-btn__desc">
              Nicht beitreten (keine Bindung, auch 0 Fr. pro Runde wählbar)
            </span>
            <span className="invest-btn__action">Ohne Abkommen spielen →</span>
          </button>
        </div>
      )}
    </section>
  );
};
