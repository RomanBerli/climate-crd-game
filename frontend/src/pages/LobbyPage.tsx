import React, { useState } from 'react';
import { GameStateResponse } from '../types/game';
import { joinLobby, leaveLobby } from '../api/gameApi';

interface LobbyPageProps {
  gameState: GameStateResponse | null;
  onJoined: () => Promise<void>;
  onNavigateAdmin: () => void;
}

export const LobbyPage: React.FC<LobbyPageProps> = ({
  gameState,
  onJoined,
  onNavigateAdmin,
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
    await leaveLobby();
    await onJoined();
  };

  // Falls der Spieler bereits im Warteraum (LOBBY) eines Raums eingeloggt ist
  if (gameState && gameState.status === 'LOBBY') {
    return (
      <main className="page-container page-container--centered">
        <div className="lobby-waiting-card">
          <div className="lobby-waiting-card__badges">
            <span className="badge badge--accent">Spieler {gameState.player.playerNumber}</span>
            <span className="badge badge--neutral">Raumcode: {gameState.sessionCode}</span>
            <span className="badge badge--risk">Risiko-Treatment: {gameState.riskPercent}%</span>
          </div>

          <h1>Warteraum – Sie sind Spieler {gameState.player.playerNumber}</h1>
          <p className="lobby-waiting-card__subtitle">
            Sobald alle {gameState.groupSize} Probanden beigetreten sind, startet der Versuchsleiter das Experiment.
          </p>

          <div className="lobby-rules-summary">
            <h3>Experiment-Regeln im Überblick</h3>
            <ul>
              <li>
                <strong>Ihr Startkapital:</strong> {gameState.player.initialBalanceRappen} Rappen
              </li>
              <li>
                <strong>Dauer:</strong> {gameState.totalRounds} Runden (pro Runde wählbar: 0, 20 oder 40 Rappen)
              </li>
              <li>
                <strong>Gemeinsames Klimaziel der {gameState.groupSize}er-Gruppe:</strong>{' '}
                {gameState.targetSumRappen} Rappen nach {gameState.totalRounds} Runden
              </li>
              <li>
                <strong>Risiko bei Verfehlen des Ziels:</strong> Mit{' '}
                <strong>{gameState.riskPercent}% Wahrscheinlichkeit</strong> verlieren alle Spieler ihr gesamtes Restguthaben (0 Rappen).
              </li>
            </ul>
          </div>

          <div className="lobby-slots-section">
            <div className="lobby-slots-section__header">
              <span>Verbundene Spieler</span>
              <strong>
                {gameState.connectedPlayersCount} / {gameState.groupSize} bereit
              </strong>
            </div>
            <div className="player-status-grid">
              {gameState.playersStatus.map((slot) => {
                const isMe = slot.playerNumber === gameState.player.playerNumber;
                return (
                  <div
                    key={slot.playerNumber}
                    className={`player-slot ${
                      slot.isConnected ? 'player-slot--ready' : 'player-slot--waiting'
                    } ${isMe ? 'player-slot--me' : ''}`}
                  >
                    <div className="player-slot__name">
                      Spieler {slot.playerNumber} {isMe && <span className="player-slot__me-tag">(Sie)</span>}
                    </div>
                    <div className="player-slot__state">
                      <span className="player-slot__dot" />
                      {slot.isConnected ? 'Verbunden' : 'Freier Platz'}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          <div className="lobby-waiting-card__footer">
            <span className="pulse-indicator">Warte auf Spielstart durch Versuchsleiter...</span>
            <button type="button" className="btn btn--ghost" onClick={handleLeaveLobby}>
              Anderen Raumcode eingeben
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
          <span className="badge badge--accent">Klima-Labor • Milinski-Experiment</span>
          <h1>Collective-Risk Social Dilemma</h1>
          <p>
            Geben Sie den 4-stelligen Spielcode ein, den Sie von Ihrem Versuchsleiter erhalten haben.
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

        <div className="lobby-login-card__admin-link">
          <button type="button" className="btn btn--ghost" onClick={onNavigateAdmin}>
            Versuchsleiter / Admin-Dashboard öffnen
          </button>
        </div>
      </div>
    </main>
  );
};
