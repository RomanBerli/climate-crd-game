import React from 'react';
import { RoundHistoryDto } from '../../types/game';
import { formatFr } from '../../utils/currency';

interface HistoryTableProps {
  history: RoundHistoryDto[];
  myPlayerNumber: number;
  groupSize: number;
  initialBalanceRappen?: number;
}

export const HistoryTable: React.FC<HistoryTableProps> = ({
  history,
  myPlayerNumber,
  groupSize,
}) => {
  const playerColumns = Array.from({ length: groupSize }, (_, i) => i + 1);
  const lastRoundEntry = history.length > 0 ? history[history.length - 1] : null;

  return (
    <section className="history-card">
      <div className="history-card__header">
        <h3 className="section-title" style={{ marginBottom: 0 }}>
          {lastRoundEntry
            ? `Ergebnis der letzten Runde (Runde ${lastRoundEntry.round})`
            : 'Ergebnis der letzten Runde'}
        </h3>
      </div>

      {!lastRoundEntry ? (
        <div className="history-empty">
          Runde 1 läuft. Sobald Runde 1 abgeschlossen ist, sehen Sie hier die Einzelbeiträge der letzten Runde.
        </div>
      ) : (
        <div className="history-table-wrapper">
          <table className="history-table">
            <thead>
              <tr>
                {playerColumns.map((pNum) => (
                  <th
                    key={pNum}
                    className={pNum === myPlayerNumber ? 'history-table__col--me' : ''}
                  >
                    Spieler {pNum}
                    {pNum === myPlayerNumber ? ' (Sie)' : ''}
                  </th>
                ))}
                <th>Runden-Total</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                {playerColumns.map((pNum) => {
                  const entry = lastRoundEntry.investments.find(
                    (inv) => inv.playerNumber === pNum
                  );
                  const amount = entry ? entry.amountRappen : 0;
                  return (
                    <td
                      key={pNum}
                      className={`history-table__cell ${
                        pNum === myPlayerNumber ? 'history-table__col--me' : ''
                      }`}
                    >
                      <span className="amount-pill amount-pill--20">
                        {formatFr(amount)}
                      </span>
                    </td>
                  );
                })}
                <td className="history-table__round">
                  <strong>+{formatFr(lastRoundEntry.roundTotalRappen)}</strong>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
};
