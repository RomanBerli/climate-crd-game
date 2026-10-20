import React from 'react';
import { PlayerSlotStatusDto } from '../../types/game';

interface PlayerStatusListProps {
  playersStatus: PlayerSlotStatusDto[];
  myPlayerNumber: number;
}

export const PlayerStatusList: React.FC<PlayerStatusListProps> = ({
  playersStatus,
  myPlayerNumber,
}) => {
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
