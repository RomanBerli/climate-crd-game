import React, { useState } from 'react';
import { GameMode, PlayerSlotStatusDto } from '../../types/game';

interface PlayerStatusListProps {
  playersStatus: PlayerSlotStatusDto[];
  myPlayerNumber: number;
  gameMode?: GameMode;
  inContract?: boolean;
  canLeaveContract?: boolean;
  currentRound?: number;
  totalRounds?: number;
  onLeaveContract?: () => Promise<void>;
}

export const PlayerStatusList: React.FC<PlayerStatusListProps> = ({
  playersStatus,
  myPlayerNumber,
  gameMode = 'NORMAL',
  inContract = false,
  canLeaveContract = false,
  currentRound = 1,
  totalRounds = 10,
  onLeaveContract,
}) => {
  const [leavingContract, setLeavingContract] = useState<boolean>(false);

  if (gameMode !== 'WITH_CONTRACTS') {
    return null;
  }

  const handleLeave = async () => {
    if (!onLeaveContract || leavingContract || !canLeaveContract) return;
    setLeavingContract(true);
    try {
      await onLeaveContract();
    } finally {
      setLeavingContract(false);
    }
  };

  const showLeaveButton = inContract && currentRound < totalRounds && Boolean(onLeaveContract);

  return (
    <section className="player-status-card">
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '0.75rem',
          marginBottom: '0.85rem',
        }}
      >
        <h3 className="section-title" style={{ marginBottom: 0 }}>
          Abkommen
        </h3>

        {showLeaveButton && (
          <button
            type="button"
            className="btn btn--ghost"
            disabled={!canLeaveContract || leavingContract}
            onClick={handleLeave}
            style={{ padding: '0.4rem 0.85rem', fontSize: '0.85rem' }}
          >
            {leavingContract
              ? 'Wird gespeichert...'
              : canLeaveContract
              ? 'In der nächsten Runde Abkommen verlassen'
              : 'Austritt für nächste Runde vorgemerkt'}
          </button>
        )}
      </div>

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
                  ? 'Im Abkommen'
                  : slot.leftContract
                  ? 'Ausgetreten'
                  : 'Nicht dabei'}
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
};
