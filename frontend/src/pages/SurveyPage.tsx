import React, { useState } from 'react';
import { GameStateResponse } from '../types/game';
import { submitSurvey } from '../api/gameApi';

interface SurveyPageProps {
  gameState: GameStateResponse;
  onStateUpdated: (newState: GameStateResponse) => void;
}

const STRATEGY_OPTIONS = [
  'Ich habe möglichst wenig eingezahlt, um viel Geld zu behalten, obwohl die Gruppe dadurch das Ziel verfehlen konnte.',
  'Ich habe in jeder Runde ungefähr den fairen Beitrag von 30 Rappen eingezahlt.',
  'Ich habe mehr als den fairen Beitrag eingezahlt, damit die Gruppe das Ziel sicher erreicht.',
  'Ich habe mich nach den Beiträgen der anderen gerichtet.',
  'Ich hatte keine feste Strategie.',
];

const UNFAIRNESS_OPTIONS = [
  'Ich habe mehr eingezahlt, um es auszugleichen.',
  'Ich habe mich nicht beeinflussen lassen und gleich weitergemacht.',
  'Ich habe weniger eingezahlt, weil ich das unfair fand.',
  'Ich habe weniger eingezahlt, weil ich nicht mehr an das Ziel geglaubt habe.',
  'Ich habe nicht auf die Beiträge der anderen geachtet.',
  'Es gab keine Beiträge unter 30 Rappen.',
];

const CONTRACT_JOINED_OPTIONS = [
  'Ich wollte die Chance erhöhen, dass die Gruppe das Ziel erreicht.',
  'Ich wollte verhindern, dass ich mein eigenes Geld verliere.',
  'Ich habe erwartet, dass die meisten anderen auch beitreten.',
  'Ich wollte ein Vorbild sein, damit die anderen auch fair spielen.',
  'Ich finde es fair, wenn alle gleich viel beitragen.',
  'Ich wollte mich auf einen festen Beitrag festlegen, statt jede Runde neu zu entscheiden.',
  'OTHER',
];

const CONTRACT_NOT_JOINED_OPTIONS = [
  'Ich wollte möglichst wenig einzahlen, um viel Geld zu behalten.',
  'Ich wollte meine Beiträge den Beiträgen der anderen anpassen können.',
  'Ich habe nicht erwartet, dass genug andere beitreten.',
  'Ich dachte, die Gruppe erreicht das Ziel auch ohne Abkommen.',
  'Ich wollte meinen fairen Beitrag freiwillig leisten, ohne mich zu verpflichten.',
  'Ich habe das Abkommen nicht richtig verstanden.',
  'OTHER',
];

const CONTRACT_LEFT_OPTIONS = [
  'Ich fand es unfair, dass andere weniger als 30 Rappen eingezahlt haben.',
  'Ich habe nicht mehr geglaubt, dass die Gruppe das Ziel erreicht.',
  'Ich dachte, die Gruppe erreicht das Ziel auch ohne meinen Beitrag.',
  'Ich wollte mehr eigenes Geld behalten.',
  'Ich wollte meine Beiträge den Beiträgen der anderen anpassen können.',
  'Ich bin ausgetreten, weil andere ausgetreten sind.',
  'OTHER',
];

const GENDER_OPTIONS = ['männlich', 'weiblich', 'keine Angabe'];

export const SurveyPage: React.FC<SurveyPageProps> = ({
  gameState,
  onStateUpdated,
}) => {
  const { player, gameMode } = gameState;
  const isContractsMode = gameMode === 'WITH_CONTRACTS';

  // 3a für alle, die beim Start beigetreten sind (bedingt und unbedingt), 3b für alle anderen.
  // Wer später selbst ausgetreten ist, beantwortet zusätzlich 3c.
  // Wer als bedingtes Mitglied automatisch ausgeschieden ist (leftContract === false), bekommt 3c nicht.
  const showJoinedQuestion =
    isContractsMode &&
    (player.contractJoinChoice === 'YES' ||
      player.contractJoinChoice === 'CONDITIONAL' ||
      player.everJoinedContract);
  const showNotJoinedQuestion = isContractsMode && !showJoinedQuestion;
  const showLeftQuestion = isContractsMode && player.leftContract;

  const [strategyChoice, setStrategyChoice] = useState<string>('');
  const [reactionToUnfairness, setReactionToUnfairness] = useState<string>('');

  // Abkommen-Fragen (3a, 3b, 3c)
  const [contractJoinedChoice, setContractJoinedChoice] = useState<string>('');
  const [contractJoinedOther, setContractJoinedOther] = useState<string>('');

  const [contractNotJoinedChoice, setContractNotJoinedChoice] = useState<string>('');
  const [contractNotJoinedOther, setContractNotJoinedOther] = useState<string>('');

  const [contractLeftChoice, setContractLeftChoice] = useState<string>('');
  const [contractLeftOther, setContractLeftOther] = useState<string>('');

  // Angaben zur Person & Auszahlung
  const [age, setAge] = useState<string>('');
  const [gender, setGender] = useState<string>('');
  const [payoutMethod, setPayoutMethod] = useState<'TWINT' | 'CASH' | ''>('');
  const [twintPhone, setTwintPhone] = useState<string>('');

  const [submitting, setSubmitting] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

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

    if (!strategyChoice) {
      setError('Bitte wähle eine Antwort bei Frage 1 (Strategie) aus.');
      return;
    }

    if (!reactionToUnfairness) {
      setError('Bitte wähle eine Antwort bei Frage 2 (Reaktion auf tiefe Beiträge) aus.');
      return;
    }

    if (showJoinedQuestion && !contractJoinedChoice) {
      setError('Bitte wähle einen Grund für deinen Beitritt zum Abkommen aus.');
      return;
    }

    if (showNotJoinedQuestion && !contractNotJoinedChoice) {
      setError('Bitte wähle einen Grund aus, warum du dem Abkommen nicht beigetreten bist.');
      return;
    }

    if (showLeftQuestion && !contractLeftChoice) {
      setError('Bitte wähle einen Grund für deinen Austritt aus dem Abkommen aus.');
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

    if (!payoutMethod) {
      setError('Bitte wähle aus, wie du deinen Betrag erhalten möchtest.');
      return;
    }

    if (payoutMethod === 'TWINT' && !twintPhone.trim()) {
      setError('Bitte gib deine Telefonnummer für die Twint-Auszahlung an.');
      return;
    }

    const finalPayoutInfo =
      payoutMethod === 'TWINT'
        ? twintPhone.trim()
        : 'Bar (Abholung bei Spielleitung)';

    setSubmitting(true);
    try {
      const updated = await submitSurvey({
        expectedProbabilityPct: null,
        reactionToUnfairness,
        motivationRanking: [strategyChoice],
        contractJoinedReason: showJoinedQuestion
          ? resolveOptionValue(contractJoinedChoice, contractJoinedOther)
          : null,
        contractNotJoinedReason: showNotJoinedQuestion
          ? resolveOptionValue(contractNotJoinedChoice, contractNotJoinedOther)
          : null,
        contractLeftReason: showLeftQuestion
          ? resolveOptionValue(contractLeftChoice, contractLeftOther)
          : null,
        contractStayedReason: null,
        lowRiskInvestRappen: null,
        age: parsedAge,
        gender,
        twintPhone: finalPayoutInfo,
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
          <h1>Fragebogen zum Spiel</h1>
          <p>
            Alle 10 Runden sind gespielt! Bitte beantworte kurz die folgenden Fragen, bevor das Endresultat und deine Auszahlung angezeigt werden.
          </p>
        </header>

        <form onSubmit={handleSubmit} className="survey-form">
          {/* Frage 1: Strategie */}
          <div className="survey-question">
            <span className="survey-question__title">
              Welche Strategie hast du beim Spielen hauptsächlich verfolgt?
            </span>
            <div className="survey-radio-list">
              {STRATEGY_OPTIONS.map((option) => (
                <label
                  key={option}
                  className={`survey-radio-item ${strategyChoice === option ? 'survey-radio-item--selected' : ''
                    }`}
                >
                  <input
                    type="radio"
                    name="strategyChoice"
                    value={option}
                    checked={strategyChoice === option}
                    onChange={() => setStrategyChoice(option)}
                  />
                  <span>{option}</span>
                </label>
              ))}
            </div>
          </div>

          {/* Frage 2: Reaktion auf tiefe Beiträge */}
          <div className="survey-question">
            <span className="survey-question__title">
              Haben andere in einer Runde weniger als 30 Rappen eingezahlt? Wie hast du darauf reagiert?
            </span>
            <div className="survey-radio-list">
              {UNFAIRNESS_OPTIONS.map((option) => (
                <label
                  key={option}
                  className={`survey-radio-item ${reactionToUnfairness === option ? 'survey-radio-item--selected' : ''
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

          {/* Frage 3a: Beigetreten */}
          {showJoinedQuestion && (
            <div className="survey-question">
              <span className="survey-question__title">
                Was war der wichtigste Grund für deinen Beitritt zum Abkommen?
              </span>
              <div className="survey-radio-list">
                {CONTRACT_JOINED_OPTIONS.map((option) => (
                  <label
                    key={option}
                    className={`survey-radio-item ${contractJoinedChoice === option ? 'survey-radio-item--selected' : ''
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
          )}

          {/* Frage 3b: Nicht beigetreten */}
          {showNotJoinedQuestion && (
            <div className="survey-question">
              <span className="survey-question__title">
                Was war der wichtigste Grund, warum du nicht beigetreten bist?
              </span>
              <div className="survey-radio-list">
                {CONTRACT_NOT_JOINED_OPTIONS.map((option) => (
                  <label
                    key={option}
                    className={`survey-radio-item ${contractNotJoinedChoice === option ? 'survey-radio-item--selected' : ''
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
          )}

          {/* Frage 3c: Selbst ausgetreten */}
          {showLeftQuestion && (
            <div className="survey-question">
              <span className="survey-question__title">
                Was war der wichtigste Grund für deinen Austritt?
              </span>
              <div className="survey-radio-list">
                {CONTRACT_LEFT_OPTIONS.map((option) => (
                  <label
                    key={option}
                    className={`survey-radio-item ${contractLeftChoice === option ? 'survey-radio-item--selected' : ''
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
          )}

          {/* Angaben zur Person */}
          <div className="survey-demographics-grid">
            <div className="survey-question">
              <label className="survey-question__title" htmlFor="surveyAgeInput">
                Wie alt bist du?
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
              <span className="survey-question__title">
                Welchem Geschlecht fühlst du dich zugehörig?
              </span>
              <div className="survey-radio-list">
                {GENDER_OPTIONS.map((g) => (
                  <label
                    key={g}
                    className={`survey-radio-item ${gender === g ? 'survey-radio-item--selected' : ''
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

          {/* Auszahlung */}
          <div className="survey-question">
            <span className="survey-question__title">
              Wie möchtest du deinen Betrag erhalten?
            </span>
            <div className="survey-radio-list">
              <label
                className={`survey-radio-item ${payoutMethod === 'TWINT' ? 'survey-radio-item--selected' : ''
                  }`}
              >
                <input
                  type="radio"
                  name="payoutMethod"
                  value="TWINT"
                  checked={payoutMethod === 'TWINT'}
                  onChange={() => setPayoutMethod('TWINT')}
                />
                <span>Per Twint</span>
              </label>

              {payoutMethod === 'TWINT' && (
                <div style={{ marginTop: '0.35rem' }}>
                  <label
                    htmlFor="surveyTwintInput"
                    style={{ display: 'block', fontWeight: 600, marginBottom: '0.35rem', fontSize: '0.9rem' }}
                  >
                    Telefonnummer
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
              )}

              <label
                className={`survey-radio-item ${payoutMethod === 'CASH' ? 'survey-radio-item--selected' : ''
                  }`}
              >
                <input
                  type="radio"
                  name="payoutMethod"
                  value="CASH"
                  checked={payoutMethod === 'CASH'}
                  onChange={() => setPayoutMethod('CASH')}
                />
                <span>Bar. Ich komme nach dem Spiel zur Spielleitung nach vorne.</span>
              </label>
            </div>
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
