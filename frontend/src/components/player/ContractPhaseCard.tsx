import React, { useState } from 'react';
import { GameStateResponse } from '../../types/game';
import {
  submitContractJoin,
  submitContractReview,
  submitContractVote,
} from '../../api/gameApi';
import { formatFr } from '../../utils/currency';

interface ContractPhaseCardProps {
  gameState: GameStateResponse;
  onStateUpdated: (newState: GameStateResponse) => void;
}

export const ContractPhaseCard: React.FC<ContractPhaseCardProps> = ({
  gameState,
  onStateUpdated,
}) => {
  const { roundPhase, contractState, player, currentRound, groupSize } = gameState;
  const [busy, setBusy] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const runAction = async (fn: () => Promise<GameStateResponse>) => {
    if (busy) return;
    setBusy(true);
    setError(null);
    try {
      const updated = await fn();
      onStateUpdated(updated);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Fehler bei der Abkommens-Aktion.');
    } finally {
      setBusy(false);
    }
  };

  if (roundPhase === 'CONTRACT_JOIN') {
    return (
      <section className="investment-card">
        <div className="investment-card__header">
          <h2>Klimaabkommen vor Runde 1 (Freiwilliger Beitritt)</h2>
          <span className="badge badge--accent">Vor Runde 1</span>
        </div>

        <div className="lobby-rules-summary" style={{ marginBottom: '1.25rem' }}>
          <h3>Regeln des Klimaabkommens</h3>
          <ul>
            <li>
              <strong>Mindestbeitrag:</strong> Jedes Mitglied verpflichtet sich, in jeder Runde mindestens{' '}
              <strong>{formatFr(contractState.minInvestmentRappen)}</strong> einzuzahlen. Dies wird im Spiel technisch erzwungen.
            </li>
            <li>
              <strong>Austritt zwischen zwei Runden:</strong> Ein Austritt kostet eine Gebühr von{' '}
              <strong>{formatFr(contractState.exitFeePerMemberRappen)} an jedes verbleibende Mitglied</strong>.
            </li>
            <li>
              <strong>Abstimmung zur Abschaffung:</strong> Zwischen zwei Runden kann ein Mitglied eine Abstimmung starten. Stimmt eine einfache Mehrheit der Mitglieder dafür, wird das Abkommen kostenlos aufgelöst.
            </li>
          </ul>
        </div>

        {error && <div className="alert alert--error">{error}</div>}

        {contractState.hasDecidedCurrentContractPhase ? (
          <div className="investment-confirmed">
            <div className="investment-confirmed__icon">✓</div>
            <div>
              <h3>
                Ihre Entscheidung:{' '}
                {player.inContract ? 'Dem Abkommen beigetreten' : 'Nicht beigetreten'}
              </h3>
              <p>
                Bitte warten Sie, bis alle {groupSize} Spieler über den Beitritt entschieden haben. Danach startet Runde 1 automatisch.
              </p>
            </div>
          </div>
        ) : (
          <div className="investment-grid" style={{ gridTemplateColumns: 'repeat(2, 1fr)' }}>
            <button
              type="button"
              className="invest-btn"
              disabled={busy}
              onClick={() => runAction(() => submitContractJoin(true))}
            >
              <span className="invest-btn__amount">Beitreten</span>
              <span className="invest-btn__desc">
                Verbindlicher Mindestbeitrag von {formatFr(contractState.minInvestmentRappen)} pro Runde
              </span>
              <span className="invest-btn__action">Dem Abkommen beitreten →</span>
            </button>

            <button
              type="button"
              className="invest-btn"
              disabled={busy}
              onClick={() => runAction(() => submitContractJoin(false))}
            >
              <span className="invest-btn__amount">Nicht beitreten</span>
              <span className="invest-btn__desc">
                Keine Bindung (auch 0 Fr. pro Runde wählbar, kein späterer Beitritt möglich)
              </span>
              <span className="invest-btn__action">Ohne Abkommen spielen →</span>
            </button>
          </div>
        )}
      </section>
    );
  }

  if (roundPhase === 'CONTRACT_VOTE') {
    return (
      <section className="investment-card">
        <div className="investment-card__header">
          <h2>Abstimmung zur Abschaffung des Abkommens</h2>
          <span className="badge badge--risk">
            Gestartet von Spieler {contractState.voteInitiatorPlayerNumber}
          </span>
        </div>

        <p style={{ marginBottom: '1rem', color: 'var(--text-secondary)' }}>
          Spieler {contractState.voteInitiatorPlayerNumber} hat vor Runde {currentRound} eine Abstimmung zur kostenlosen Auflösung des Klimaabkommens gestartet. Stimmt eine einfache Mehrheit der {contractState.memberCount} Mitglieder für die Abschaffung, wird das Abkommen ohne Gebühren aufgelöst.
        </p>

        {error && <div className="alert alert--error">{error}</div>}

        {!player.inContract ? (
          <div className="investment-confirmed">
            <div className="investment-confirmed__icon">⏳</div>
            <div>
              <h3>Sie sind kein Mitglied des Abkommens</h3>
              <p>
                Die aktuellen Mitglieder ({contractState.memberCount} Spieler) stimmen gerade über die Abschaffung des Abkommens ab. Bitte warten...
              </p>
            </div>
          </div>
        ) : contractState.hasDecidedCurrentContractPhase ? (
          <div className="investment-confirmed">
            <div className="investment-confirmed__icon">✓</div>
            <div>
              <h3>Ihre Stimme wurde gezählt</h3>
              <p>Warten auf die Stimmabgabe der übrigen Abkommens-Mitglieder...</p>
            </div>
          </div>
        ) : (
          <div className="investment-grid" style={{ gridTemplateColumns: 'repeat(2, 1fr)' }}>
            <button
              type="button"
              className="invest-btn"
              disabled={busy}
              onClick={() => runAction(() => submitContractVote(true))}
            >
              <span className="invest-btn__amount">Für Abschaffung</span>
              <span className="invest-btn__desc">
                Abkommen kostenlos auflösen (Mindestbeitrag entfällt für alle)
              </span>
              <span className="invest-btn__action">Für Abschaffung stimmen →</span>
            </button>

            <button
              type="button"
              className="invest-btn"
              disabled={busy}
              onClick={() => runAction(() => submitContractVote(false))}
            >
              <span className="invest-btn__amount">Gegen Abschaffung</span>
              <span className="invest-btn__desc">
                Abkommen und Mindestbeitrag ({formatFr(contractState.minInvestmentRappen)}) beibehalten
              </span>
              <span className="invest-btn__action">Abkommen beibehalten →</span>
            </button>
          </div>
        )}
      </section>
    );
  }

  // CONTRACT_REVIEW (Zwischen zwei Runden)
  const remainingOtherMembers = Math.max(0, contractState.memberCount - 1);

  return (
    <section className="investment-card">
      <div className="investment-card__header">
        <h2>Zwischenphase vor Runde {currentRound}: Klimaabkommen</h2>
        <span className="badge badge--accent">
          {contractState.memberCount} aktive Mitglieder
        </span>
      </div>

      {contractState.lastContractEvent && (
        <div className="lobby-rules-summary" style={{ marginBottom: '1rem' }}>
          <strong>Status-Meldung:</strong> {contractState.lastContractEvent}
        </div>
      )}

      {error && <div className="alert alert--error">{error}</div>}

      {!player.inContract ? (
        <div className="investment-confirmed">
          <div className="investment-confirmed__icon">⏳</div>
          <div>
            <h3>Zwischenphase der Abkommens-Mitglieder</h3>
            <p>
              Sie sind aktuell nicht im Abkommen. Bitte warten Sie kurz, bis die {contractState.memberCount} Mitglieder des Abkommens ihre Entscheidung vor Runde {currentRound} getroffen haben.
            </p>
          </div>
        </div>
      ) : contractState.hasDecidedCurrentContractPhase ? (
        <div className="investment-confirmed">
          <div className="investment-confirmed__icon">✓</div>
          <div>
            <h3>Entscheidung gespeichert</h3>
            <p>
              Warten auf die übrigen Mitglieder des Abkommens, bevor Runde {currentRound} startet...
            </p>
          </div>
        </div>
      ) : (
        <div className="investment-grid">
          <button
            type="button"
            className="invest-btn"
            disabled={busy}
            onClick={() => runAction(() => submitContractReview('STAY'))}
          >
            <span className="invest-btn__amount">Bleiben</span>
            <span className="invest-btn__desc">
              Weiterhin im Abkommen bleiben (Mindestbeitrag {formatFr(contractState.minInvestmentRappen)})
            </span>
            <span className="invest-btn__action">Im Abkommen bleiben →</span>
          </button>

          <button
            type="button"
            className="invest-btn"
            disabled={busy || contractState.voteAlreadyHeldThisRound}
            onClick={() => runAction(() => submitContractReview('START_VOTE'))}
          >
            <span className="invest-btn__amount">Abstimmung</span>
            <span className="invest-btn__desc">
              {contractState.voteAlreadyHeldThisRound
                ? 'Abstimmung fand in dieser Zwischenrunde bereits statt'
                : 'Abstimmung zur kostenlosen Abschaffung des Abkommens starten'}
            </span>
            <span className="invest-btn__action">
              {contractState.voteAlreadyHeldThisRound ? 'Bereits abgestimmt' : 'Abstimmung starten →'}
            </span>
          </button>

          <button
            type="button"
            className="invest-btn"
            disabled={busy || !contractState.canAffordExit}
            onClick={() => runAction(() => submitContractReview('LEAVE'))}
          >
            <span className="invest-btn__amount">
              Austreten ({formatFr(contractState.totalExitFeeForPlayerRappen)})
            </span>
            <span className="invest-btn__desc">
              {remainingOtherMembers > 0
                ? `Gebühr: ${formatFr(contractState.exitFeePerMemberRappen)} an jedes der ${remainingOtherMembers} verbleibenden Mitglieder`
                : 'Sie sind das letzte Mitglied (0 Fr. Gebühr)'}
            </span>
            <span className="invest-btn__action">
              {contractState.canAffordExit
                ? 'Kostenpflichtig austreten →'
                : 'Guthaben reicht nicht für Gebühr'}
            </span>
          </button>
        </div>
      )}
    </section>
  );
};
