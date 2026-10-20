import React from 'react';
import { RoundPhase } from '../../types/game';

interface StageTimerProps {
  roundPhase: RoundPhase;
  remainingSeconds: number;
  hasSubmitted: boolean;
}

export const StageTimer: React.FC<StageTimerProps> = ({
  roundPhase,
  remainingSeconds,
  hasSubmitted,
}) => {
  const isOvertime = roundPhase === 'OVERTIME';
  const maxSeconds = isOvertime ? 30 : 60;
  const clampedSeconds = Math.max(0, Math.min(maxSeconds, remainingSeconds));
  const percentage = Math.round((clampedSeconds / maxSeconds) * 100);

  return (
    <section
      className={`stage-timer ${
        isOvertime ? 'stage-timer--overtime' : 'stage-timer--regular'
      }`}
    >
      <div className="stage-timer__top">
        <div className="stage-timer__phase-info">
          <span
            className={`stage-timer__badge ${
              isOvertime ? 'stage-timer__badge--overtime' : 'stage-timer__badge--regular'
            }`}
          >
            {isOvertime ? 'OVERTIME-PHASE (30s)' : 'REGULÄRE PHASE (60s)'}
          </span>
          <span className="stage-timer__subtitle">
            {hasSubmitted
              ? 'Ihre Auswahl wurde gespeichert. Warten auf verbleibende Mitspieler...'
              : isOvertime
              ? 'Overtime: Wähle jetzt deinen Beitrag, sonst erfolgt ein Auto-Einsatz von 20 Rappen!'
              : 'Bitte wählen Sie Ihren Beitrag für diese Runde.'}
          </span>
        </div>

        <div className="stage-timer__clock" aria-live="polite">
          <span className="stage-timer__seconds">{remainingSeconds}</span>
          <span className="stage-timer__unit">Sek.</span>
        </div>
      </div>

      <div className="stage-timer__bar-track">
        <div
          className="stage-timer__bar-fill"
          style={{ width: `${percentage}%` }}
        />
      </div>
    </section>
  );
};
