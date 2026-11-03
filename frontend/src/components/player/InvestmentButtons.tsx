import React, { useState } from 'react';
import { formatFr } from '../../utils/currency';

interface InvestmentButtonsProps {
  initialBalanceRappen: number;
  currentBalanceRappen: number;
  allowedOptions: number[];
  hasSubmitted: boolean;
  submittedAmount: number | null;
  allPlayersReady: boolean;
  onInvest: (amountRappen: number) => Promise<void>;
}

export const InvestmentButtons: React.FC<InvestmentButtonsProps> = ({
  initialBalanceRappen,
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
            <h3>Einsatz für diese Runde eingeloggt: {formatFr(submittedAmount ?? 0)}</h3>
            <p>
              {allPlayersReady
                ? 'Alle Spieler haben gewählt...'
                : 'Warten auf die verbleibenden Mitspieler...'}
            </p>
          </div>
        </div>
      ) : (
        <div className="investment-grid">
          {dynamicOptions.map((amount) => {
            const isAllowed = allowedOptions.includes(amount) && currentBalanceRappen >= amount;

            return (
              <button
                key={amount}
                type="button"
                className={`invest-btn invest-btn--${amount}`}
                disabled={!isAllowed || submittingAmount !== null}
                onClick={() => handleSelect(amount)}
              >
                <span className="invest-btn__amount">{formatFr(amount)}</span>
              </button>
            );
          })}
        </div>
      )}
    </section>
  );
};
