import React from 'react';
import { RoundHistoryDto } from '../../types/game';
import { formatFr } from '../../utils/currency';

interface HistoryTableProps {
  history: RoundHistoryDto[];
  myPlayerNumber: number;
  groupSize: number;
  initialBalanceRappen?: number;
}

/**
 * Statische Transparenz-Anzeige:
 * Zeigt ausschliesslich die Einzelbeiträge der unmittelbar letzten Runde (Spieler 1 bis Spieler 6)
 * sowie das Runden-Total in Franken (ohne kumuliertes Fonds-Total, damit die Spieler selbst Notizen machen).
 */
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
        <h3 className="section-title">
          {lastRoundEntry
            ? `Ergebnis der letzten Runde (Runde ${lastRoundEntry.round})`
            : 'Ergebnis der letzten Runde'}
        </h3>
        <span className="history-card__hint">
          Es wird nur die jeweils letzte Runde angezeigt – nutzen Sie das Notizfeld für eigene Berechnungen
        </span>
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
