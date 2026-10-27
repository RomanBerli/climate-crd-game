import React, { useState } from 'react';
import { CsvExportType, downloadSessionCsv } from '../../api/adminApi';

interface CsvExportButtonProps {
  sessionId: string;
  sessionCode: string;
  adminKey: string;
}

const EXPORT_OPTIONS: { type: CsvExportType; label: string; title: string }[] = [
  {
    type: 'round_info',
    label: '⬇ RoundInfo',
    title: 'Spiel- & Vertragsübersicht pro Spieler (inkl. Auszahlung & Abkommen-Status)',
  },
  {
    type: 'invest',
    label: '⬇ Invest',
    title: 'Alle Runden-Investitionen (Runde 1–10) inkl. Vertragsstatus pro Runde',
  },
  {
    type: 'survey',
    label: '⬇ Survey',
    title: 'Fragebogen-Antworten ohne Telefonnummer (anonymisiert)',
  },
  {
    type: 'payout',
    label: '⬇ Auszahlung',
    title: 'Auszahlungsliste mit CHF-Betrag und Twint-Telefonnummer',
  },
  {
    type: 'all',
    label: '⬇ Alles in einem',
    title: 'Kombinierte Gesamttabelle aller Werte (anonymisiert ohne Telefonnummer)',
  },
];

export const CsvExportButton: React.FC<CsvExportButtonProps> = ({
  sessionId,
  sessionCode,
  adminKey,
}) => {
  const [downloadingType, setDownloadingType] = useState<CsvExportType | null>(null);

  const handleDownload = async (type: CsvExportType) => {
    if (downloadingType !== null) return;
    setDownloadingType(type);
    try {
      await downloadSessionCsv(sessionId, sessionCode, type, adminKey);
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Fehler beim CSV-Download');
    } finally {
      setDownloadingType(null);
    }
  };

  return (
    <div
      className="csv-export-group"
      style={{ display: 'flex', flexWrap: 'wrap', gap: '0.4rem', alignItems: 'center' }}
    >
      {EXPORT_OPTIONS.map((opt) => {
        const isCurrent = downloadingType === opt.type;
        return (
          <button
            key={opt.type}
            type="button"
            className="btn btn--secondary"
            style={{ padding: '0.45rem 0.75rem', fontSize: '0.82rem' }}
            onClick={() => handleDownload(opt.type)}
            disabled={downloadingType !== null}
            title={opt.title}
          >
            {isCurrent ? 'Exportiere...' : opt.label}
          </button>
        );
      })}
    </div>
  );
};
