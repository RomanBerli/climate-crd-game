import React, { useState } from 'react';
import { formatFr } from '../../utils/currency';

interface InvestmentButtonsProps {
  initialBalanceRappen: number;
  currentBalanceRappen: number;
  allowedOptions: number[];
  hasSubmitted: boolean;
  submittedAmount: number | null;
  allPlayersReady: boolean;
  isLastRound: boolean;
  onInvest: (amountRappen: number) => Promise<void>;
}

/**
 * Zweistufige Eingabe: Betrag auswählen, dann ausdrücklich bestätigen.
 * So landet kein Vertipper unwiderruflich in den Experimentdaten. Die Auswahl ist zu Beginn
 * jeder Runde leer (die Komponente wird pro Runde neu aufgebaut), es gibt also keine Vorauswahl.
 */
export const InvestmentButtons: React.FC<InvestmentButtonsProps> = ({
  initialBalanceRappen,
  currentBalanceRappen,
  allowedOptions,
  hasSubmitted,
  submittedAmount,
  allPlayersReady,
  isLastRound,
  onInvest,
}) => {
  const [selectedAmount, setSelectedAmount] = useState<number | null>(null);
  const [submitting, setSubmitting] = useState<boolean>(false);

  const handleConfirm = async () => {
    if (hasSubmitted || submitting || selectedAmount === null) return;
    setSubmitting(true);
    try {
      await onInvest(selectedAmount);
    } finally {
      setSubmitting(false);
    }
  };

  const opt0 = 0;
  const opt1 = Math.round(initialBalanceRappen * 0.05);
  const opt2 = Math.round(initialBalanceRappen * 0.10);
  const dynamicOptions = [opt0, opt1, opt2];

  return (
    <section className="investment-card">
      <div className="investment-card__header" style={{ alignItems: 'center' }}>
        <h2>Beitrag</h2>
        <div className="metric-box metric-box--highlight">
          <span className="metric-box__label">Eigenes Restguthaben</span>
          <strong className="metric-box__value metric-box__value--balance">
            {formatFr(currentBalanceRappen)}
          </strong>
        </div>
      </div>

      {hasSubmitted ? (
        <div className="investment-confirmed">
          <div className="investment-confirmed__icon">✓</div>
          <div>
            <h3>Dein Beitrag für diese Runde: {formatFr(submittedAmount ?? 0)}</h3>
            <p>
              {allPlayersReady
                ? isLastRound
                  ? 'Alle haben entschieden. Unten siehst du die Beiträge dieser Runde. Die Spielleitung schliesst das Spiel ab.'
                  : 'Alle haben entschieden. Unten siehst du die Beiträge dieser Runde. Die Spielleitung startet die nächste Runde.'
                : 'Warte auf die anderen Spieler...'}
            </p>
          </div>
        </div>
      ) : (
        <>
          <div className="investment-grid">
            {dynamicOptions.map((amount) => {
              const isAllowed = allowedOptions.includes(amount) && currentBalanceRappen >= amount;
              const isSelected = selectedAmount === amount;

              return (
                <button
                  key={amount}
                  type="button"
                  className={`invest-btn invest-btn--${amount} ${isSelected ? 'invest-btn--selected' : ''}`}
                  disabled={!isAllowed || submitting}
                  aria-pressed={isSelected}
                  onClick={() => setSelectedAmount(amount)}
                >
                  <span className="invest-btn__amount">{formatFr(amount)}</span>
                </button>
              );
            })}
          </div>

          <div className="confirm-row">
            <span className="confirm-row__hint">
              {selectedAmount === null
                ? 'Wähle einen Betrag und bestätige ihn danach.'
                : `Ausgewählt: ${formatFr(selectedAmount)} – nach dem Bestätigen kannst du den Beitrag nicht mehr ändern.`}
            </span>
            <button
              type="button"
              className="btn btn--primary confirm-row__button"
              disabled={selectedAmount === null || submitting}
              onClick={handleConfirm}
            >
              {submitting ? 'Wird gespeichert...' : 'Beitrag bestätigen'}
            </button>
          </div>
        </>
      )}
    </section>
  );
};
