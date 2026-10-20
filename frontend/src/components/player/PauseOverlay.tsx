import React from 'react';

interface PauseOverlayProps {
  visible: boolean;
}

/**
 * Blockierendes Fullscreen-Overlay bei Admin-Pause (Spec 4.1, Plan 3.6).
 */
export const PauseOverlay: React.FC<PauseOverlayProps> = ({ visible }) => {
  if (!visible) return null;

  return (
    <div className="pause-overlay" role="dialog" aria-modal="true" aria-label="Spiel pausiert">
      <div className="pause-overlay__box">
        <div className="pause-overlay__icon">⏸</div>
        <h2>Spiel pausiert durch Admin</h2>
        <p>
          Der Versuchsleiter hat das Experiment vorübergehend angehalten.
          Der Runden-Timer ist exakt eingefroren und läuft nahtlos weiter, sobald die Pause aufgehoben wird.
        </p>
      </div>
    </div>
  );
};
