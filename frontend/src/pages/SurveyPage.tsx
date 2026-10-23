import React, { useState } from 'react';
import { GameStateResponse } from '../types/game';
import { submitSurvey } from '../api/gameApi';

interface SurveyPageProps {
  gameState: GameStateResponse;
  onStateUpdated: (newState: GameStateResponse) => void;
}

const UNFAIRNESS_OPTIONS = [
  'Ich habe mehr eingezahlt, um es auszugleichen.',
  'Ich habe gleich weitergemacht.',
  'Ich habe weniger eingezahlt, weil ich das unfair fand.',
  'Ich habe es nicht bemerkt oder es gab keine Beiträge unter dem fairen Beitrag.',
];

const INITIAL_MOTIVATION_ITEMS = [
  'möglichst viel eigenes Geld behalten',
  'den Verlust des eigenen Geldes vermeiden',
  'dass die Gruppe das Ziel erreicht',
  'fair zu sein bzw. meinen Anteil zu leisten',
  'den Preis zu gewinnen',
];

const CONTRACT_JOINED_OPTIONS = [
  'Ich wollte sicherstellen, dass die Gruppe das Ziel erreicht.',
  'Ich habe erwartet, dass die anderen auch beitreten und ihren Teil leisten.',
  'Der feste Mindestbeitrag gab mir Sicherheit und Planbarkeit.',
  'Ich finde es fair, dass alle gleich viel beitragen.',
  'Ich wollte den anderen zeigen, dass ich kooperiere.',
  'Die Austrittsgebühr sollte mich selbst zum Durchhalten verpflichten.',
  'OTHER',
];

const CONTRACT_NOT_JOINED_OPTIONS = [
  'Ich wollte flexibel bleiben und selbst entscheiden, wie viel ich einzahle.',
  'Ich wollte weniger als den Mindestbeitrag einzahlen.',
  'Ich habe nicht darauf vertraut, dass die anderen sich daran halten.',
  'Ich dachte, die Gruppe erreicht das Ziel auch ohne mich bzw. ohne Abkommen.',
  'Die Austrittsgebühr war mir zu riskant.',
  'Ich wollte auch ohne Abkommen meinen fairen Teil leisten.',
  'Ich habe das Abkommen nicht richtig verstanden.',
  'OTHER',
];

const CONTRACT_LEFT_OPTIONS = [
  'Andere haben weniger beigetragen als ich. Das fand ich unfair.',
  'Ich dachte, das Ziel wird sowieso nicht mehr erreicht.',
  'Ich dachte, das Ziel wird auch ohne meinen Beitrag erreicht.',
  'Ich wollte mehr eigenes Geld behalten. Das war mir die Austrittsgebühr wert.',
  'Andere sind ausgetreten, deshalb bin ich auch gegangen.',
  'OTHER',
];

const CONTRACT_STAYED_OPTIONS = [
  'Ich wollte das Ziel der Gruppe erreichen.',
  'Ich hatte es zugesagt und wollte mich daran halten.',
  'Ein Austritt hätte sich wegen der Austrittsgebühr nicht gelohnt.',
  'Die anderen haben sich auch daran gehalten.',
  'Ich habe nicht über einen Austritt nachgedacht.',
  'OTHER',
];

export const SurveyPage: React.FC<SurveyPageProps> = ({
  gameState,
  onStateUpdated,
}) => {
  const { player, sessionCode, gameMode } = gameState;
  const isContractsMode = gameMode === 'WITH_CONTRACTS';

  const [expectedProb, setExpectedProb] = useState<number>(50);
  const [reactionToUnfairness, setReactionToUnfairness] = useState<string>('');
  const [motivationRanking, setMotivationRanking] = useState<string[]>(INITIAL_MOTIVATION_ITEMS);
  const [draggedIdx, setDraggedIdx] = useState<number | null>(null);

  // Abkommen-Fragen (nur im Modus WITH_CONTRACTS sichtbar)
  const [contractJoinedChoice, setContractJoinedChoice] = useState<string>('');
  const [contractJoinedOther, setContractJoinedOther] = useState<string>('');

  const [contractNotJoinedChoice, setContractNotJoinedChoice] = useState<string>('');
  const [contractNotJoinedOther, setContractNotJoinedOther] = useState<string>('');

  const [contractLeftChoice, setContractLeftChoice] = useState<string>('');
  const [contractLeftOther, setContractLeftOther] = useState<string>('');

  const [contractStayedChoice, setContractStayedChoice] = useState<string>('');
  const [contractStayedOther, setContractStayedOther] = useState<string>('');

  const [age, setAge] = useState<string>('');
  const [gender, setGender] = useState<string>('');
  const [twintPhone, setTwintPhone] = useState<string>('');

  const [submitting, setSubmitting] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const moveItem = (index: number, direction: -1 | 1) => {
    const targetIndex = index + direction;
    if (targetIndex < 0 || targetIndex >= motivationRanking.length) return;
    const copy = [...motivationRanking];
    const [removed] = copy.splice(index, 1);
    copy.splice(targetIndex, 0, removed);
    setMotivationRanking(copy);
  };

  const handleDragStart = (idx: number) => {
    setDraggedIdx(idx);
  };

  const handleDragOver = (e: React.DragEvent, idx: number) => {
    e.preventDefault();
    if (draggedIdx === null || draggedIdx === idx) return;
    const copy = [...motivationRanking];
    const [draggedItem] = copy.splice(draggedIdx, 1);
    copy.splice(idx, 0, draggedItem);
    setMotivationRanking(copy);
    setDraggedIdx(idx);
  };

  const resolveOptionValue = (choice: string, otherText: string): string | null => {
    if (!choice) return null;
    if (choice === 'OTHER') {
      return `Anderer Grund: ${otherText.trim()}`;
    }
    return choice;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!reactionToUnfairness) {
      setError('Bitte wähle eine Antwort bei Frage 2 aus.');
      return;
    }

    const parsedAge = parseInt(age, 10);
    if (Number.isNaN(parsedAge) || parsedAge < 5 || parsedAge > 120) {
      setError('Bitte gib ein gültiges Alter ein.');
      return;
    }

    if (!gender) {
      setError('Bitte wähle dein Geschlecht aus.');
      return;
    }

    if (!twintPhone.trim()) {
      setError('Bitte gib deine Telefonnummer für die Twint-Auszahlung an.');
      return;
    }

    setSubmitting(true);
    try {
      const updated = await submitSurvey({
        expectedProbabilityPct: expectedProb,
        reactionToUnfairness,
        motivationRanking,
        contractJoinedReason: isContractsMode
          ? resolveOptionValue(contractJoinedChoice, contractJoinedOther)
          : null,
        contractNotJoinedReason: isContractsMode
          ? resolveOptionValue(contractNotJoinedChoice, contractNotJoinedOther)
          : null,
        contractLeftReason: isContractsMode
          ? resolveOptionValue(contractLeftChoice, contractLeftOther)
          : null,
        contractStayedReason: isContractsMode
          ? resolveOptionValue(contractStayedChoice, contractStayedOther)
          : null,
        age: parsedAge,
        gender,
        twintPhone: twintPhone.trim(),
      });
      onStateUpdated(updated);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Fehler beim Absenden des Fragebogens.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="page-container">
      <div className="survey-card">
        <header className="survey-card__header">
          <div className="round-header__identity">
            <span className="badge badge--accent">Spieler {player.playerNumber}</span>
            <span className="badge badge--neutral">Raum {sessionCode}</span>
            <span className="badge badge--private">Abschluss-Fragebogen</span>
          </div>
          <h1>Fragebogen zum Spielverlauf</h1>
          <p>
            Alle 10 Runden sind gespielt! Bitte beantworte kurz die folgenden Fragen, bevor das Endresultat und deine Auszahlung angezeigt werden.
          </p>
        </header>

        <form onSubmit={handleSubmit} className="survey-form">
          {/* Frage 1 */}
          <div className="survey-question">
            <label className="survey-question__title" htmlFor="expectedProbInput">
              1. Wie wahrscheinlich hast du es zu Beginn des Spiels gefunden, dass eure Gruppe die Zielsumme erreicht? (0–100 %)
            </label>
            <div className="survey-probability-row">
              <input
                type="range"
                min={0}
                max={100}
                step={1}
                value={expectedProb}
                onChange={(e) => setExpectedProb(Number(e.target.value))}
                className="survey-slider"
              />
              <div className="survey-prob-number">
                <input
                  id="expectedProbInput"
                  type="number"
                  min={0}
                  max={100}
                  value={expectedProb}
                  onChange={(e) =>
                    setExpectedProb(Math.max(0, Math.min(100, Number(e.target.value) || 0)))
                  }
                />
                <span>%</span>
              </div>
            </div>
          </div>

          {/* Frage 2 */}
          <div className="survey-question">
            <span className="survey-question__title">
              2. Hast du bemerkt, dass andere weniger als den fairen Beitrag eingezahlt haben? Falls ja, wie hast du reagiert?
            </span>
            <div className="survey-radio-list">
              {UNFAIRNESS_OPTIONS.map((option) => (
                <label
                  key={option}
                  className={`survey-radio-item ${
                    reactionToUnfairness === option ? 'survey-radio-item--selected' : ''
                  }`}
                >
                  <input
                    type="radio"
                    name="reactionToUnfairness"
                    value={option}
                    checked={reactionToUnfairness === option}
                    onChange={() => setReactionToUnfairness(option)}
                  />
                  <span>{option}</span>
                </label>
              ))}
            </div>
          </div>

          {/* Frage 3: Sortierbare Liste */}
          <div className="survey-question">
            <span className="survey-question__title">
              3. Was war dir beim Spielen am wichtigsten?
            </span>
            <p className="survey-question__hint">
              Sortiere die Punkte mit den Pfeilen (oder per Drag & Drop): <strong>Oben (Platz 1) = am wichtigsten</strong>, <strong>Unten (Platz 5) = am unwichtigsten</strong>.
            </p>
            <div className="survey-ranking-list">
              {motivationRanking.map((item, idx) => (
                <div
                  key={item}
                  className="survey-ranking-item"
                  draggable
                  onDragStart={() => handleDragStart(idx)}
                  onDragOver={(e) => handleDragOver(e, idx)}
                  onDragEnd={() => setDraggedIdx(null)}
                >
                  <span className="survey-ranking-item__rank">{idx + 1}.</span>
                  <span className="survey-ranking-item__text">{item}</span>
                  <div className="survey-ranking-item__controls">
                    <button
                      type="button"
                      className="btn btn--secondary survey-rank-btn"
                      disabled={idx === 0}
                      onClick={() => moveItem(idx, -1)}
                      title="Nach oben"
                    >
                      ↑
                    </button>
                    <button
                      type="button"
                      className="btn btn--secondary survey-rank-btn"
                      disabled={idx === motivationRanking.length - 1}
                      onClick={() => moveItem(idx, 1)}
                      title="Nach unten"
                    >
                      ↓
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Abkommen-Fragen: Nur im Modus 'WITH_CONTRACTS' */}
          {isContractsMode && (
            <>
              <div className="survey-question">
                <span className="survey-question__title">
                  Falls dem Abkommen beigetreten: Was war der wichtigste Grund, warum du dem Abkommen beigetreten bist?
                </span>
                <div className="survey-radio-list">
                  {CONTRACT_JOINED_OPTIONS.map((option) => (
                    <label
                      key={option}
                      className={`survey-radio-item ${
                        contractJoinedChoice === option ? 'survey-radio-item--selected' : ''
                      }`}
                    >
                      <input
                        type="radio"
                        name="contractJoined"
                        value={option}
                        checked={contractJoinedChoice === option}
                        onChange={() => setContractJoinedChoice(option)}
                      />
                      <span>{option === 'OTHER' ? 'Anderer Grund:' : option}</span>
                    </label>
                  ))}
                  {contractJoinedChoice === 'OTHER' && (
                    <input
                      type="text"
                      className="survey-text-input"
                      placeholder="Bitte Grund angeben..."
                      value={contractJoinedOther}
                      onChange={(e) => setContractJoinedOther(e.target.value)}
                    />
                  )}
                </div>
              </div>

              <div className="survey-question">
                <span className="survey-question__title">
                  Falls nicht beigetreten: Was war der wichtigste Grund, warum du dem Abkommen nicht beigetreten bist?
                </span>
                <div className="survey-radio-list">
                  {CONTRACT_NOT_JOINED_OPTIONS.map((option) => (
                    <label
                      key={option}
                      className={`survey-radio-item ${
                        contractNotJoinedChoice === option ? 'survey-radio-item--selected' : ''
                      }`}
                    >
                      <input
                        type="radio"
                        name="contractNotJoined"
                        value={option}
                        checked={contractNotJoinedChoice === option}
                        onChange={() => setContractNotJoinedChoice(option)}
                      />
                      <span>{option === 'OTHER' ? 'Anderer Grund:' : option}</span>
                    </label>
                  ))}
                  {contractNotJoinedChoice === 'OTHER' && (
                    <input
                      type="text"
                      className="survey-text-input"
                      placeholder="Bitte Grund angeben..."
                      value={contractNotJoinedOther}
                      onChange={(e) => setContractNotJoinedOther(e.target.value)}
                    />
                  )}
                </div>
              </div>

              <div className="survey-question">
                <span className="survey-question__title">
                  Falls ausgetreten vom Abkommen: Was war der wichtigste Grund für deinen Austritt?
                </span>
                <div className="survey-radio-list">
                  {CONTRACT_LEFT_OPTIONS.map((option) => (
                    <label
                      key={option}
                      className={`survey-radio-item ${
                        contractLeftChoice === option ? 'survey-radio-item--selected' : ''
                      }`}
                    >
                      <input
                        type="radio"
                        name="contractLeft"
                        value={option}
                        checked={contractLeftChoice === option}
                        onChange={() => setContractLeftChoice(option)}
                      />
                      <span>{option === 'OTHER' ? 'Anderer Grund:' : option}</span>
                    </label>
                  ))}
                  {contractLeftChoice === 'OTHER' && (
                    <input
                      type="text"
                      className="survey-text-input"
                      placeholder="Bitte Grund angeben..."
                      value={contractLeftOther}
                      onChange={(e) => setContractLeftOther(e.target.value)}
                    />
                  )}
                </div>
              </div>

              <div className="survey-question">
                <span className="survey-question__title">
                  Falls bis zum Ende im Abkommen geblieben: Was war der wichtigste Grund, warum du bis zum Schluss im Abkommen geblieben bist?
                </span>
                <div className="survey-radio-list">
                  {CONTRACT_STAYED_OPTIONS.map((option) => (
                    <label
                      key={option}
                      className={`survey-radio-item ${
                        contractStayedChoice === option ? 'survey-radio-item--selected' : ''
                      }`}
                    >
                      <input
                        type="radio"
                        name="contractStayed"
                        value={option}
                        checked={contractStayedChoice === option}
                        onChange={() => setContractStayedChoice(option)}
                      />
                      <span>{option === 'OTHER' ? 'Anderer Grund:' : option}</span>
                    </label>
                  ))}
                  {contractStayedChoice === 'OTHER' && (
                    <input
                      type="text"
                      className="survey-text-input"
                      placeholder="Bitte Grund angeben..."
                      value={contractStayedOther}
                      onChange={(e) => setContractStayedOther(e.target.value)}
                    />
                  )}
                </div>
              </div>
            </>
          )}

          {/* Demografie & Twint */}
          <div className="survey-demographics-grid">
            <div className="survey-question">
              <label className="survey-question__title" htmlFor="surveyAgeInput">
                4. Wie alt bist du?
              </label>
              <input
                id="surveyAgeInput"
                type="number"
                min={5}
                max={120}
                placeholder="z.B. 18"
                value={age}
                onChange={(e) => setAge(e.target.value)}
                className="survey-text-input"
                required
              />
            </div>

            <div className="survey-question">
              <span className="survey-question__title">5. Geschlecht</span>
              <div className="survey-radio-inline">
                {['Mann', 'Frau', 'Keine Angabe'].map((g) => (
                  <label
                    key={g}
                    className={`survey-radio-item ${
                      gender === g ? 'survey-radio-item--selected' : ''
                    }`}
                  >
                    <input
                      type="radio"
                      name="gender"
                      value={g}
                      checked={gender === g}
                      onChange={() => setGender(g)}
                    />
                    <span>{g}</span>
                  </label>
                ))}
              </div>
            </div>
          </div>

          <div className="survey-question">
            <label className="survey-question__title" htmlFor="surveyTwintInput">
              6. Auf welche Telefonnummer sollte dein Betrag ausgezahlt werden über Twint?
            </label>
            <input
              id="surveyTwintInput"
              type="tel"
              placeholder="z.B. 079 123 45 67"
              value={twintPhone}
              onChange={(e) => setTwintPhone(e.target.value)}
              className="survey-text-input"
              required
            />
          </div>

          {error && <div className="alert alert--error">{error}</div>}

          <button
            type="submit"
            className="btn btn--primary btn--large"
            disabled={submitting}
          >
            {submitting ? 'Fragebogen wird gespeichert...' : 'Fragebogen absenden →'}
          </button>
        </form>
      </div>
    </main>
  );
};
