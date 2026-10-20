import React from 'react';
import { RoundHistoryDto } from '../../types/game';

interface HistoryTableProps {
  history: RoundHistoryDto[];
  myPlayerNumber: number;
  groupSize: number;
}

/**
 * Statische Transparenz-Matrix gemäss Spec 1.4 & Plan 3.3:
 * Spieler 1 steht immer ganz links, Spieler 6 immer ganz rechts.
 * Es wird KEINE Gesamtsumme des Klimatopfes berechnet oder angezeigt!
 */
export const HistoryTable: React.FC<HistoryTableProps> = ({
  history,
  myPlayerNumber,
  groupSize,
}) => {
  const playerColumns = Array.from({ length: groupSize }, (_, i) => i + 1);

  return (
    <section className="history-card">
      <div className="history-card__header">
        <h3 className="section-title">Runden-Historie (Einzelbeiträge in Rappen)</h3>
        <span className="history-card__hint">
          Statische Spaltenordnung (Spieler 1 bis Spieler {groupSize})
        </span>
      </div>

      {history.length === 0 ? (
        <div className="history-empty">
          Noch keine Runde abgeschlossen. Nach Abschluss von Runde 1 erscheinen hier die Einzelinvestitionen aller 6 Spieler.
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
              {history.map((row) => (
                <tr key={row.round}>
                  <td className="history-table__round">Runde {row.round}</td>
                  {playerColumns.map((pNum) => {
                    const entry = row.investments.find((inv) => inv.playerNumber === pNum);
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
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
};
