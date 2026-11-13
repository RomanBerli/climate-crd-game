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
  const [selectedChoice, setSelectedChoice] = useState<ContractJoinChoice | null>(null);
  const [busy, setBusy] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const minHalf = Math.ceil(groupSize / 2);
  const minContribution = formatFr(contractState.minInvestmentRappen);

  const options: Array<{ choice: ContractJoinChoice; label: string; desc: string }> = [
    {
      choice: 'YES',
      label: 'Ja',
      desc: 'Ich trete dem Abkommen auf jeden Fall bei.',
    },
    {
      choice: 'CONDITIONAL',
      label: `Ab ${minHalf} Spielern`,
      desc: `Ich trete nur bei, wenn insgesamt mindestens ${minHalf} von ${groupSize} Spielern beitreten wollen.`,
    },
    {
      choice: 'NO',
      label: 'Nein',
      desc: 'Ich trete dem Abkommen nicht bei.',
    },
  ];

  const handleConfirm = async () => {
    if (busy || selectedChoice === null) return;
    setBusy(true);
    setError(null);
    try {
      const updated = await submitContractJoin(selectedChoice);
      onStateUpdated(updated);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Fehler bei der Abkommens-Entscheidung.');
    } finally {
      setBusy(false);
    }
  };

  const getDecisionLabel = (choice: ContractJoinChoice | null): string => {
    const match = options.find((o) => o.choice === choice);
    return match ? match.label : '–';
  };

  return (
    <section className="investment-card">
      <div className="investment-card__header">
        <h2>Abkommen beitreten?</h2>
      </div>

      {error && <div className="alert alert--error">{error}</div>}

      {contractState.hasDecidedCurrentContractPhase ? (
        <div className="investment-confirmed">
          <div className="investment-confirmed__icon">✓</div>
          <div>
            <h3>Deine Entscheidung: {getDecisionLabel(player.contractJoinChoice)}</h3>
            <p>
              Warte, bis alle {groupSize} Spieler entschieden haben. Danach siehst du, wer im Abkommen ist, und Runde 1 beginnt.
            </p>
          </div>
        </div>
      ) : (
        <>
          <ul className="contract-rules">
            <li>
              Wer im Abkommen ist, zahlt in jeder Runde mindestens <strong>{minContribution}</strong> ein. «0 Fr.» ist für Mitglieder gesperrt.
            </li>
            <li>
              Der Beitritt ist nur jetzt möglich. Ein Austritt ist während des Spiels möglich und gilt ab der jeweils nächsten Runde.
            </li>
            <li>
              Alle sehen während des Spiels, wer im Abkommen ist.
            </li>
          </ul>

          <div className="investment-grid" style={{ gridTemplateColumns: 'repeat(3, 1fr)' }}>
            {options.map((opt) => (
              <button
                key={opt.choice}
                type="button"
                className={`invest-btn ${selectedChoice === opt.choice ? 'invest-btn--selected' : ''}`}
                disabled={busy}
                aria-pressed={selectedChoice === opt.choice}
                onClick={() => setSelectedChoice(opt.choice)}
              >
                <span className="invest-btn__amount">{opt.label}</span>
                <span className="invest-btn__desc">{opt.desc}</span>
              </button>
            ))}
          </div>

          <div className="confirm-row">
            <span className="confirm-row__hint">
              {selectedChoice === null
                ? 'Wähle eine Antwort und bestätige sie danach.'
                : `Ausgewählt: «${getDecisionLabel(selectedChoice)}» – nach dem Bestätigen kannst du die Entscheidung nicht mehr ändern.`}
            </span>
            <button
              type="button"
              className="btn btn--primary confirm-row__button"
              disabled={selectedChoice === null || busy}
              onClick={handleConfirm}
            >
              {busy ? 'Wird gespeichert...' : 'Entscheidung bestätigen'}
            </button>
          </div>
        </>
      )}
    </section>
  );
};
