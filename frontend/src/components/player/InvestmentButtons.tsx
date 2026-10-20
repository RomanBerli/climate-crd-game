import React, { useState } from 'react';

interface InvestmentButtonsProps {
  currentBalanceRappen: number;
  allowedOptions: number[];
  hasSubmitted: boolean;
  submittedAmount: number | null;
  isPaused: boolean;
  onInvest: (amountRappen: number) => Promise<void>;
}

const OPTION_METADATA: Record<number, { title: string; subtitle: string }> = {
  0: {
    title: '0 Rappen',
    subtitle: 'Kein Beitrag in dieser Runde',
  },
  20: {
    title: '20 Rappen',
    subtitle: 'Mittlerer Beitrag (Fairer Anteil)',
  },
  40: {
    title: '40 Rappen',
    subtitle: 'Maximaler Rundenbeitrag',
  },
};

export const InvestmentButtons: React.FC<InvestmentButtonsProps> = ({
  currentBalanceRappen,
  allowedOptions,
  hasSubmitted,
  submittedAmount,
  isPaused,
  onInvest,
}) => {
  const [submittingAmount, setSubmittingAmount] = useState<number | null>(null);

  const handleSelect = async (amount: number) => {
    if (hasSubmitted || isPaused || submittingAmount !== null) return;
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
          Verfügbares Guthaben: <strong>{currentBalanceRappen} Rappen</strong>
        </p>
      </div>

      {hasSubmitted ? (
        <div className="investment-confirmed">
          <div className="investment-confirmed__icon">✓</div>
          <div>
            <h3>Einsatz für diese Runde eingeloggt: {submittedAmount} Rappen</h3>
            <p>Sobald alle 6 Spieler gewählt haben, startet automatisch die nächste Runde.</p>
          </div>
        </div>
      ) : (
        <div className="investment-grid">
          {standardOptions.map((amount) => {
            const isAllowed = allowedOptions.includes(amount) && currentBalanceRappen >= amount;
            const meta = OPTION_METADATA[amount] || {
              title: `${amount} Rappen`,
              subtitle: 'Beitrag',
            };
            const isLoading = submittingAmount === amount;

            return (
              <button
                key={amount}
                type="button"
                className={`invest-btn invest-btn--${amount}`}
                disabled={!isAllowed || isPaused || submittingAmount !== null}
                onClick={() => handleSelect(amount)}
              >
                <span className="invest-btn__amount">{meta.title}</span>
                <span className="invest-btn__desc">{meta.subtitle}</span>
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
