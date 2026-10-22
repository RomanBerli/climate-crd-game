import React from 'react';
import { RoundHistoryDto } from '../../types/game';
import { formatFr } from '../../utils/currency';

interface HistoryTableProps {
  history: RoundHistoryDto[];
  myPlayerNumber: number;
  groupSize: number;
  targetSumRappen?: number;
}

/**
 * Statische Transparenz-Anzeige:
 * Zeigt die Einzelbeiträge der unmittelbar letzten Runde (Spieler 1 bis Spieler 6)
 * sowie das Runden-Total und den gesamten Stand des Klimafonds nach dieser Runde in Franken.
 */
export const HistoryTable: React.FC<HistoryTableProps> = ({
  history,
  myPlayerNumber,
  groupSize,
  targetSumRappen,
}) => {
  const playerColumns = Array.from({ length: groupSize }, (_, i) => i + 1);
  const lastRoundEntry = history.length > 0 ? history[history.length - 1] : null;

  return (
    <section className="history-card">
      <div className="history-card__header">
        <h3 className="section-title">
          {lastRoundEntry
            ? `Ergebnis der letzten Runde (Runde ${lastRoundEntry.round})`
            : 'Ergebnis der letzten Runde'}
        </h3>
        {lastRoundEntry ? (
          <span className="badge badge--accent">
            Klimafonds Total: {formatFr(lastRoundEntry.cumulativePotRappen)}
            {targetSumRappen ? ` / ${formatFr(targetSumRappen)}` : ''}
          </span>
        ) : (
          <span className="history-card__hint">
            Klimafonds Startstand: 0 Fr.{targetSumRappen ? ` / ${formatFr(targetSumRappen)}` : ''}
          </span>
        )}
      </div>

      {!lastRoundEntry ? (
        <div className="history-empty">
          Runde 1 läuft. Sobald Runde 1 abgeschlossen ist, sehen Sie hier die Einzelbeiträge der letzten Runde sowie den neuen Gesamtstand des Klimafonds.
        </div>
      ) : (
        <div className="history-table-wrapper">
          <table className="history-table">
            <thead>
              <tr>
                <th>Runde</th>
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
                <th>Fonds Total</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td className="history-table__round">Runde {lastRoundEntry.round}</td>
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
                      <span className={`amount-pill amount-pill--${amount}`}>
                        {formatFr(amount)}
                      </span>
                    </td>
                  );
                })}
                <td className="history-table__round">
                  <strong>+{formatFr(lastRoundEntry.roundTotalRappen)}</strong>
                </td>
                <td className="history-table__round">
                  <strong style={{ color: '#34d399' }}>
                    {formatFr(lastRoundEntry.cumulativePotRappen)}
                  </strong>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
};
