import React, { useState } from 'react';

interface CreateSessionModalProps {
  onCreateSession: (riskLevel: number) => Promise<void>;
}

const TREATMENTS = [
  {
    value: 0.9,
    label: '90% Risiko-Treatment (High Risk)',
    description: '90% Verlustwahrscheinlichkeit bei Verfehlen des 1200-Rappen-Ziels (Milinski et al.)',
  },
  {
    value: 0.5,
    label: '50% Risiko-Treatment (Medium Risk)',
    description: '50% Verlustwahrscheinlichkeit bei Verfehlen des 1200-Rappen-Ziels',
  },
  {
    value: 0.1,
    label: '10% Risiko-Treatment (Low Risk)',
    description: '10% Verlustwahrscheinlichkeit bei Verfehlen des 1200-Rappen-Ziels',
  },
];

export const CreateSessionModal: React.FC<CreateSessionModalProps> = ({ onCreateSession }) => {
  const [selectedRisk, setSelectedRisk] = useState<number>(0.9);
  const [creating, setCreating] = useState<boolean>(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (creating) return;
    setCreating(true);
    try {
      await onCreateSession(selectedRisk);
    } finally {
      setCreating(false);
    }
  };

  return (
    <form className="admin-create-card" onSubmit={handleSubmit}>
      <div className="admin-create-card__header">
        <h2>Neue Experiment-Session anlegen</h2>
        <p>Wählen Sie das gewünschte Risiko-Treatment für die 6er-Gruppe (Startkapital: 400 Rappen, Ziel: 1200 Rappen).</p>
      </div>

      <div className="treatment-options">
        {TREATMENTS.map((t) => (
          <label
            key={t.value}
            className={`treatment-option ${
              selectedRisk === t.value ? 'treatment-option--selected' : ''
            }`}
          >
            <input
              type="radio"
              name="riskLevel"
              value={t.value}
              checked={selectedRisk === t.value}
              onChange={() => setSelectedRisk(t.value)}
            />
            <div>
              <strong>{t.label}</strong>
              <span>{t.description}</span>
            </div>
          </label>
        ))}
      </div>

      <button type="submit" className="btn btn--primary" disabled={creating}>
        {creating ? 'Raum wird erstellt...' : '+ Neuen Gruppenraum (6 Spieler) generieren'}
      </button>
    </form>
  );
};
