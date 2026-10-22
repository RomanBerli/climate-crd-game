import React from 'react';

interface LocalNotepadProps {
  notes: string;
  onChange: (value: string) => void;
}

/**
 * Dauerhafter digitaler Notizblock (Spec 1.4 & 2.1).
 * Rein lokal im Browser-State gehalten, niemals an den Server übermittelt.
 */
export const LocalNotepad: React.FC<LocalNotepadProps> = ({ notes, onChange }) => {
  return (
    <aside className="notepad-card">
      <div className="notepad-card__header" style={{ marginBottom: '0.75rem' }}>
        <h3 className="section-title">Persönlicher Notizblock</h3>
      </div>
      <textarea
        className="notepad-card__textarea"
        value={notes}
        onChange={(e) => onChange(e.target.value)}
        placeholder="Eigene Notizen & Rechnungen (z. B. bisherige Rundenbeiträge, Ziel 12 Fr.)..."
        rows={12}
      />
    </aside>
  );
};
