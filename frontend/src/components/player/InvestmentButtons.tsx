import React, { useState } from 'react';
import { formatFr } from '../../utils/currency';

interface InvestmentButtonsProps {
  initialBalanceRappen: number;
  currentBalanceRappen: number;
  allowedOptions: number[];
  hasSubmitted: boolean;
  submittedAmount: number | null;
  allPlayersReady: boolean;
  inContract?: boolean;
  minInvestmentRappen?: number;
  lastContractEvent?: string | null;
  onInvest: (amountRappen: number) => Promise<void>;
}

export const InvestmentButtons: React.FC<InvestmentButtonsProps> = ({
  initialBalanceRappen,
  currentBalanceRappen,
  allowedOptions,
  hasSubmitted,
  submittedAmount,
  allPlayersReady,
  inContract = false,
  minInvestmentRappen = 0,
  lastContractEvent = null,
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

  const minContractAmount = minInvestmentRappen > 0 ? minInvestmentRappen : opt1;

  const getOptionSubtitle = (amount: number): string => {
    if (amount === opt0) return 'Kein Beitrag in dieser Runde';
    if (amount === opt1) return 'Mittlerer Beitrag (Fairer Anteil)';
    return 'Maximaler Rundenbeitrag';
  };

  return (
    <section className="investment-card">
      <div className="investment-card__header">
        <h2>Beitrag für das Klimakonto wählen</h2>
        <p>
          Verfügbares Guthaben: <strong>{formatFr(currentBalanceRappen)}</strong>
        </p>
      </div>

      {lastContractEvent && (
        <div className="lobby-rules-summary" style={{ marginBottom: '1rem', padding: '0.85rem 1rem' }}>
          <strong>Abkommen-Info:</strong> {lastContractEvent}
        </div>
      )}

      {inContract && !hasSubmitted && (
        <div className="lobby-rules-summary" style={{ marginBottom: '1rem', padding: '0.85rem 1rem' }}>
          <strong>📜 Sie sind Mitglied im Abkommen:</strong> Ihr Mindestbeitrag beträgt{' '}
          <strong>{formatFr(minContractAmount)}</strong> in dieser Runde.
        </div>
      )}

      {hasSubmitted ? (
        <div className="investment-confirmed">
          <div className="investment-confirmed__icon">✓</div>
          <div>
            <h3>Einsatz für diese Runde eingeloggt: {formatFr(submittedAmount ?? 0)}</h3>
            <p>
              {allPlayersReady
                ? 'Alle Spieler haben gewählt. Warten auf Freigabe der nächsten Runde durch den Versuchsleiter...'
                : 'Ihre Auswahl ist gespeichert. Warten auf die verbleibenden Mitspieler...'}
            </p>
          </div>
        </div>
      ) : (
        <div className="investment-grid">
          {dynamicOptions.map((amount) => {
            const isBlockedByContract = inContract && amount < minContractAmount && !allowedOptions.includes(amount);
            const isAllowed = allowedOptions.includes(amount) && currentBalanceRappen >= amount;
            const subtitle = isBlockedByContract
              ? `Gesperrt durch Klimaabkommen (Min. ${formatFr(minContractAmount)})`
              : getOptionSubtitle(amount);
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
                  {isBlockedByContract
                    ? `Min. ${formatFr(minContractAmount)} Pflicht`
                    : isLoading
                    ? 'Wird gespeichert...'
                    : 'Jetzt einzahlen →'}
                </span>
              </button>
            );
          })}
        </div>
      )}
    </section>
  );
};
