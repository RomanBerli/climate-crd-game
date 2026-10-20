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
      <div className="notepad-card__header">
        <h3 className="section-title">Persönlicher Notizblock</h3>
        <span className="badge badge--private">100% Lokal & Privat</span>
      </div>
      <p className="notepad-card__description">
        Hier können Sie über alle 10 Runden hinweg eigene Berechnungen und Notizen festhalten.
        Ihre Eingaben verbleiben ausschliesslich in Ihrem Browser.
      </p>
      <textarea
        className="notepad-card__textarea"
        value={notes}
        onChange={(e) => onChange(e.target.value)}
        placeholder="Eigene Notizen & Rechnungen (z. B. bisherige Rundenbeiträge, Ziel 1200 Rappen)..."
        rows={12}
      />
    </aside>
  );
};
