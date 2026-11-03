import React, { useState } from 'react';
import { GameStateResponse } from '../types/game';
import { joinLobby, leaveLobby } from '../api/gameApi';

interface LobbyPageProps {
  gameState: GameStateResponse | null;
  onJoined: () => Promise<void>;
  onLeft: () => void;
  onNavigateAdmin?: () => void;
}

export const LobbyPage: React.FC<LobbyPageProps> = ({
  gameState,
  onJoined,
  onLeft,
}) => {
  const [roomCode, setRoomCode] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleJoin = async (e: React.FormEvent) => {
    e.preventDefault();
    const cleaned = roomCode.trim().toUpperCase();
    if (cleaned.length !== 4) {
      setError('Bitte geben Sie einen 4-stelligen Spielcode ein (z. B. X7K9).');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await joinLobby(cleaned);
      await onJoined();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Beitritt fehlgeschlagen.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleLeaveLobby = async () => {
    onLeft();
    await leaveLobby();
  };

  // Falls der Spieler bereits im Warteraum (LOBBY) eines Raums eingeloggt ist
  if (gameState && gameState.status === 'LOBBY') {
    return (
      <main className="page-container page-container--centered">
        <div className="lobby-waiting-card">
          <h1>Warteraum – Sie sind Spieler {gameState.player.playerNumber}</h1>
          <p className="lobby-waiting-card__subtitle">
            Sobald alle {gameState.groupSize} Spieler beigetreten sind, startet die Spielleitung das Spiel.
          </p>

          <div className="lobby-waiting-card__footer">
            <span className="pulse-indicator">Warte auf Spielstart durch die Spielleitung...</span>
            <button type="button" className="btn btn--ghost" onClick={handleLeaveLobby}>
              Raum verlassen
            </button>
          </div>
        </div>
      </main>
    );
  }

  return (
    <main className="page-container page-container--centered">
      <div className="lobby-login-card">
        <div className="lobby-login-card__header">
          <h1>Klimaspiel</h1>
          <p>
            Geben Sie den 4-stelligen Spielcode ein, den Sie von der Spielleitung erhalten haben.
          </p>
        </div>

        <form onSubmit={handleJoin} className="lobby-form">
          <label htmlFor="roomCodeInput" className="lobby-form__label">
            4-stelliger Spielcode
          </label>
          <input
            id="roomCodeInput"
            type="text"
            maxLength={4}
            value={roomCode}
            onChange={(e) => setRoomCode(e.target.value.toUpperCase())}
            placeholder="z.B. X7K9"
            className="lobby-form__input"
            autoFocus
            autoComplete="off"
          />

          {error && <div className="alert alert--error">{error}</div>}

          <button type="submit" className="btn btn--primary btn--large" disabled={submitting}>
            {submitting ? 'Verbinde mit Lobby...' : 'Der Gruppe beitreten →'}
          </button>
        </form>
      </div>
    </main>
  );
};
