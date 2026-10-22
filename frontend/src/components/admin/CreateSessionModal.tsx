import React, { useState } from 'react';
import { GameMode } from '../../types/game';

interface CreateSessionModalProps {
  onCreateSession: (gameMode: GameMode) => Promise<void>;
}

const GAME_MODES: Array<{
  value: GameMode;
  label: string;
  description: string;
  disabled: boolean;
  badge?: string;
}> = [
  {
    value: 'NORMAL',
    label: 'Variante 1: Normal',
    description:
      'Klassisches Milinski Collective-Risk Experiment (ohne Verträge).',
    disabled: false,
  },
  {
    value: 'WITH_CONTRACTS',
    label: 'Variante 2: Mit Verträgen (Klimaabkommen)',
    description:
      'Freiwilliger Beitritt zum Klimaabkommen vor Runde 1 (fester Mindestbeitrag pro Runde, Austritt gegen Gebühr an jedes verbleibende Mitglied oder kostenlose Auflösung per Mehrheitsabstimmung).',
    disabled: false,
  },
];

export const CreateSessionModal: React.FC<CreateSessionModalProps> = ({ onCreateSession }) => {
  const [selectedMode, setSelectedMode] = useState<GameMode>('NORMAL');
  const [creating, setCreating] = useState<boolean>(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (creating) return;
    setCreating(true);
    try {
      await onCreateSession(selectedMode);
    } finally {
      setCreating(false);
    }
  };

  return (
    <form className="admin-create-card" onSubmit={handleSubmit}>
      <div className="admin-create-card__header">
        <h2>Neue Experiment-Session anlegen</h2>
        <p>
          Wählen Sie den Spielmodus für die Gruppe (Risiko-Prozentsatz wird zentral über{' '}
          <code>ENV_RISK_LEVEL</code>, Default 90%, gesteuert).
        </p>
      </div>

      <div className="treatment-options treatment-options--two-cols">
        {GAME_MODES.map((m) => (
          <label
            key={m.value}
            className={`treatment-option ${
              selectedMode === m.value ? 'treatment-option--selected' : ''
            } ${m.disabled ? 'treatment-option--disabled' : ''}`}
          >
            <input
              type="radio"
              name="gameMode"
              value={m.value}
              disabled={m.disabled}
              checked={selectedMode === m.value}
              onChange={() => !m.disabled && setSelectedMode(m.value)}
            />
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
                <strong>{m.label}</strong>
                {m.badge && <span className="badge badge--neutral">{m.badge}</span>}
              </div>
              <span>{m.description}</span>
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
