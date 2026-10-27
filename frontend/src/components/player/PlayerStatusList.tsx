import React from 'react';
import { GameMode, PlayerSlotStatusDto } from '../../types/game';

interface PlayerStatusListProps {
  playersStatus: PlayerSlotStatusDto[];
  myPlayerNumber: number;
  gameMode?: GameMode;
}

export const PlayerStatusList: React.FC<PlayerStatusListProps> = ({
  playersStatus,
  myPlayerNumber,
  gameMode = 'NORMAL',
}) => {
  // Nur im Vertragsmodus anzeigen, wer im Abkommen ist (keine Bereit-/Überlegt-Anzeige)
  if (gameMode !== 'WITH_CONTRACTS') {
    return null;
  }

  return (
    <section className="player-status-card">
      <h3 className="section-title">Abkommen-Status der Gruppe</h3>
      <div className="player-status-grid">
        {playersStatus.map((slot) => {
          const isMe = slot.playerNumber === myPlayerNumber;
          return (
            <div
              key={slot.playerNumber}
              className={`player-slot ${slot.inContract ? 'player-slot--ready' : ''} ${
                isMe ? 'player-slot--me' : ''
              }`}
            >
              <div className="player-slot__name">
                Spieler {slot.playerNumber} {isMe && <span className="player-slot__me-tag">(Sie)</span>}
              </div>
              <div
                style={{
                  fontSize: '0.78rem',
                  fontWeight: 700,
                  color: slot.inContract ? 'var(--color-blue)' : 'var(--text-muted)',
                }}
              >
                {slot.inContract
                  ? '📜 Im Abkommen'
                  : slot.leftContract
                  ? 'Ausgetreten'
                  : 'Kein Abkommen'}
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
};
