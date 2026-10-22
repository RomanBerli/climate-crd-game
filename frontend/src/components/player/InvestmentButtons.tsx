import React, { useState } from 'react';
import { formatFr } from '../../utils/currency';

interface InvestmentButtonsProps {
  currentBalanceRappen: number;
  allowedOptions: number[];
  hasSubmitted: boolean;
  submittedAmount: number | null;
  allPlayersReady: boolean;
  onInvest: (amountRappen: number) => Promise<void>;
}

const OPTION_SUBTITLES: Record<number, string> = {
  0: 'Kein Beitrag in dieser Runde',
  20: 'Mittlerer Beitrag (Fairer Anteil)',
  40: 'Maximaler Rundenbeitrag',
};

export const InvestmentButtons: React.FC<InvestmentButtonsProps> = ({
  currentBalanceRappen,
  allowedOptions,
  hasSubmitted,
  submittedAmount,
  allPlayersReady,
  onInvest,
}) => {
  const [submittingAmount, setSubmittingAmount] = useState<number | null>(null);

  const handleSelect = async (amount: number) => {
    if (hasSubmitted || submittingAmount !== null) return;
    setSubmittingAmount(amount);
    try {
      await onInvest(amount);
    } finally {
      setSubmittingAmount(null);
    }
  };

  const standardOptions = [0, 20, 40];

  return (
    <section className="investment-card">
      <div className="investment-card__header">
        <h2>Beitrag für das Klimakonto wählen</h2>
        <p>
          Verfügbares Guthaben: <strong>{formatFr(currentBalanceRappen)}</strong>
        </p>
      </div>

      {hasSubmitted ? (
        <div className="investment-confirmed">
          <div className="investment-confirmed__icon">✓</div>
          <div>
            <h3>Einsatz für diese Runde eingeloggt: {formatFr(submittedAmount ?? 0)}</h3>
            <p>
              {allPlayersReady
                ? 'Alle 6 Spieler haben gewählt. Warten auf Freigabe der nächsten Runde durch den Versuchsleiter...'
                : 'Ihre Auswahl ist gespeichert. Warten auf die verbleibenden Mitspieler...'}
            </p>
          </div>
        </div>
      ) : (
        <div className="investment-grid">
          {standardOptions.map((amount) => {
            const isAllowed = allowedOptions.includes(amount) && currentBalanceRappen >= amount;
            const subtitle = OPTION_SUBTITLES[amount] || 'Beitrag';
            const isLoading = submittingAmount === amount;

            return (
              <button
                key={amount}
                type="button"
                className={`invest-btn invest-btn--${amount}`}
                disabled={!isAllowed || submittingAmount !== null}
                onClick={() => handleSelect(amount)}
              >
                <span className="invest-btn__amount">{formatFr(amount)}</span>
                <span className="invest-btn__desc">{subtitle}</span>
                <span className="invest-btn__action">
                  {isLoading ? 'Wird gespeichert...' : 'Jetzt einzahlen →'}
                </span>
              </button>
            );
          })}
        </div>
      )}
    </section>
  );
};
