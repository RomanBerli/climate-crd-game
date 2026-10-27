import React, { useEffect, useState } from 'react';
import { formatFr } from '../../utils/currency';

interface InvestmentButtonsProps {
  initialBalanceRappen: number;
  currentBalanceRappen: number;
  allowedOptions: number[];
  hasSubmitted: boolean;
  submittedAmount: number | null;
  allPlayersReady: boolean;
  inContract?: boolean;
  canLeaveContract?: boolean;
  minInvestmentRappen?: number;
  lastContractEvent?: string | null;
  onInvest: (amountRappen: number) => Promise<void>;
  onLeaveContract?: () => Promise<void>;
}

export const InvestmentButtons: React.FC<InvestmentButtonsProps> = ({
  initialBalanceRappen,
  currentBalanceRappen,
  allowedOptions,
  hasSubmitted,
  submittedAmount,
  allPlayersReady,
  inContract = false,
  canLeaveContract = false,
  minInvestmentRappen = 0,
  lastContractEvent = null,
  onInvest,
  onLeaveContract,
}) => {
  const [submittingAmount, setSubmittingAmount] = useState<number | null>(null);
  const [leavingContract, setLeavingContract] = useState<boolean>(false);
  const [visibleContractEvent, setVisibleContractEvent] = useState<string | null>(lastContractEvent);

  useEffect(() => {
    if (!lastContractEvent) {
      setVisibleContractEvent(null);
      return;
    }
    setVisibleContractEvent(lastContractEvent);
    const timer = window.setTimeout(() => {
      setVisibleContractEvent(null);
    }, 10000);
    return () => window.clearTimeout(timer);
  }, [lastContractEvent]);

  const handleSelect = async (amount: number) => {
    if (hasSubmitted || submittingAmount !== null || leavingContract) return;
    setSubmittingAmount(amount);
    try {
      await onInvest(amount);
    } finally {
      setSubmittingAmount(null);
    }
  };

  const handleLeave = async () => {
    if (!onLeaveContract || leavingContract || submittingAmount !== null) return;
    setLeavingContract(true);
    try {
      await onLeaveContract();
    } finally {
      setLeavingContract(false);
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

      {(visibleContractEvent || (inContract && !hasSubmitted)) && (
        <div className="contract-mini-notices">
          {visibleContractEvent && (
            <div className="contract-mini-box">
              <strong>Abkommen:</strong> {visibleContractEvent}
            </div>
          )}
          {inContract && !hasSubmitted && (
            <div className="contract-mini-box">
              <strong>📜 Im Abkommen:</strong> Mindestbeitrag {formatFr(minContractAmount)}
            </div>
          )}
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
              ? `Gesperrt (Min. ${formatFr(minContractAmount)})`
              : getOptionSubtitle(amount);
            const isLoading = submittingAmount === amount;

            return (
              <button
                key={amount}
                type="button"
                className={`invest-btn invest-btn--${amount}`}
                disabled={!isAllowed || submittingAmount !== null || leavingContract}
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

      {inContract && canLeaveContract && onLeaveContract && (
        <div
          style={{
            marginTop: '0.75rem',
            display: 'flex',
            justifyContent: 'flex-end',
          }}
        >
          <button
            type="button"
            disabled={leavingContract || submittingAmount !== null}
            onClick={handleLeave}
            style={{
              background: 'none',
              border: 'none',
              padding: '0.2rem 0.35rem',
              fontSize: '0.78rem',
              color: 'var(--text-secondary)',
              textDecoration: 'underline',
              cursor: 'pointer',
              opacity: 0.65,
            }}
          >
            {leavingContract ? 'Wird verarbeitet...' : 'Aus dem Abkommen austreten'}
          </button>
        </div>
      )}
    </section>
  );
};
