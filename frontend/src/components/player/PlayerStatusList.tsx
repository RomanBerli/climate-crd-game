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
  const showContracts = gameMode === 'WITH_CONTRACTS';

  return (
    <section className="player-status-card">
      <h3 className="section-title">Status der Gruppe (aktuelle Runde)</h3>
      <div className="player-status-grid">
        {playersStatus.map((slot) => {
          const isMe = slot.playerNumber === myPlayerNumber;
          return (
            <div
              key={slot.playerNumber}
              className={`player-slot ${slot.isReady ? 'player-slot--ready' : 'player-slot--waiting'} ${
                isMe ? 'player-slot--me' : ''
              }`}
            >
              <div className="player-slot__name">
                Spieler {slot.playerNumber} {isMe && <span className="player-slot__me-tag">(Sie)</span>}
              </div>
              {showContracts && (
                <div style={{ fontSize: '0.75rem', fontWeight: 700, color: slot.inContract ? 'var(--color-blue)' : 'var(--text-muted)' }}>
                  {slot.inContract
                    ? '📜 Im Abkommen'
                    : slot.leftContract
                    ? 'Ausgetreten'
                    : 'Kein Abkommen'}
                </div>
              )}
              <div className="player-slot__state">
                <span className="player-slot__dot" />
                {slot.isReady ? 'Bereit' : 'Überlegt...'}
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
};
