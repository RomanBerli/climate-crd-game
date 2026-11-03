import React from 'react';

interface LocalNotepadProps {
  notes: string;
  onChange: (value: string) => void;
}

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
        placeholder="Eigene Notizen & Rechnungen..."
        rows={12}
      />
    </aside>
  );
};
