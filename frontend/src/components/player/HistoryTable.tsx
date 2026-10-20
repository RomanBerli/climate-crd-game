import React from 'react';
import { RoundHistoryDto } from '../../types/game';

interface HistoryTableProps {
  history: RoundHistoryDto[];
  myPlayerNumber: number;
  groupSize: number;
}

/**
 * Statische Transparenz-Anzeige gemäss Spec 1.4:
 * Es wird ausschliesslich die unmittelbar letzte Runde angezeigt (Spieler 1 bis Spieler 6).
 * Für ältere Runden müssen sich die Spieler selbst Notizen im Notizblock machen.
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
            ? `Rückblick: Einzelbeiträge der letzten Runde (Runde ${lastRoundEntry.round})`
            : 'Rückblick: Letzte Runde'}
        </h3>
        <span className="history-card__hint">
          Nur die jeweils letzte Runde ist sichtbar – nutzen Sie Ihren Notizblock!
        </span>
      </div>

      {!lastRoundEntry ? (
        <div className="history-empty">
          Runde 1 läuft. Ab Runde 2 sehen Sie hier jeweils nur die Einzelinvestitionen der unmittelbar vorherigen Runde. Bitte notieren Sie sich wichtige Werte selbst im Notizblock!
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
                        {amount} Rp.
                      </span>
                    </td>
                  );
                })}
              </tr>
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
};
