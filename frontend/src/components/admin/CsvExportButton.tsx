import React, { useState } from 'react';
import { downloadSessionCsv } from '../../api/adminApi';

interface CsvExportButtonProps {
  sessionId: string;
  sessionCode: string;
  adminKey: string;
}

export const CsvExportButton: React.FC<CsvExportButtonProps> = ({
  sessionId,
  sessionCode,
  adminKey,
}) => {
  const [downloading, setDownloading] = useState(false);

  const handleDownload = async () => {
    if (downloading) return;
    setDownloading(true);
    try {
      await downloadSessionCsv(sessionId, sessionCode, adminKey);
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Fehler beim CSV-Download');
    } finally {
      setDownloading(false);
    }
  };

  return (
    <button
      type="button"
      className="btn btn--secondary"
      onClick={handleDownload}
      disabled={downloading}
    >
      {downloading ? 'Exportiere CSV...' : '⬇ Download CSV (BMA-Auswertung)'}
    </button>
  );
};
