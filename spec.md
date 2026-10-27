# System-Spezifikation: Collective-Risk Social Dilemma Experiment

## 1. Projektübersicht & Zielsetzung
Entwicklung einer webbasierten Applikation zur Durchführung eines spieltheoretischen Klima-Experiments (basierend auf *Milinski et al., 2008: "The collective-risk social dilemma and the maintenance of global climate"*).
Die Plattform ermöglicht es Spielern in Gruppen über mehrere Runden hinweg Entscheidungen über Beiträge in ein gemeinsames Klimakonto zu treffen, unterstützt sowohl das klassische Basisspiel (**Variante 1: Standard**) als auch ein freiwilliges **Klimaabkommen mit bedingtem/unbedingtem Beitritt, Mindestbeitrag und optionalem Austritt ab Runde 2 (Variante 2: Mit Klimaabkommen)** und erhebt im Anschluss einen wissenschaftlichen Fragebogen.

---

## 2. Spielregeln & Domänenlogik (Game Mechanics)

### 2.1 Grundparameter & Konfiguration
Alle Spielparameter werden zentral in [`application.yml`](backend/src/main/resources/application.yml) (bzw. über Umgebungsvariablen) konfiguriert:
* **Startkapital (`StartAmount`):** Standardmässig `4 Fr.` (intern als `400` Rappen gespeichert, um Fliesskommafehler zu vermeiden).
* **Gruppengrösse (`GroupSize`):** Standardmässig `6` Spieler pro Session.
* **Rundenanzahl (`TotalRounds`):** Standardmässig `10` Runden.
* **Klimaziel (`TargetAmount`):** Automatisch berechnet als $50\%$ des gesamten Gruppen-Startkapitals:
  $$\text{TargetAmount} = \text{GroupSize} \times \text{StartAmount} \times 0.5$$
  *(Bei 6 Spielern à 4 Fr. beträgt das Gesamtkapital 24 Fr. und das Klimaziel **12 Fr.**)*
* **Verlustrisiko (`RiskLevel`):** Standardmässig $90\%$ (`0.90`). Wird das Klimaziel nach der letzten Runde verfehlt, verlieren alle Spieler mit einer Wahrscheinlichkeit von $90\%$ ihr gesamtes verbliebenes Restguthaben.
* **Währungsanzeige im UI:** Sämtliche Beträge werden im gesamten Frontend ausschliesslich in **Franken (`Fr.`)** dargestellt (z. B. `0 Fr.`, `0.2 Fr.`, `0.4 Fr.`, `4 Fr.`, `12 Fr.`).

### 2.2 Anonymität, Lobby & Inaktivitäts-Schutz
* **Pseudonyme:** Beim Betreten einer Session erhält jeder Spieler automatisch ein zufälliges, neutrales Klima-/Wetter-Pseudonym (z. B. *Monsun*, *Passat*, *Taifun*, *Golfstrom*, *Jetstream*, *Polarwirbel*). Klarnamen werden im Spiel nicht angezeigt.
* **Aktives Verlassen:** Verlässt ein Spieler den Warteraum über den Button `"Raum verlassen"` (`POST /api/lobby/leave`), wird sein Spieler-Datensatz sofort aus der Datenbank gelöscht und der Platz in der Lobby wieder freigegeben.
* **Automatischer Inaktivitäts-Kick (30s):** Spieler in der `LOBBY`, die länger als **30 Sekunden** kein Lebenszeichen (Polling) senden (z. B. durch Schliessen des Browser-Tabs), werden automatisch aus der Session entfernt, damit keine Plätze durch inaktive Verbindungen blockiert bleiben.
* **Session-Persistenz:** Über den `sessionToken` im `localStorage` können aktive Spieler bei einem Seiten-Reload nahtlos in ihre laufende Sitzung zurückkehren.

### 2.3 Rundenablauf & Manuelle Steuerung (Variante 1 & 2)
* **Kein Rundentimer / Kein Auto-Bet:** Es gibt keinen Countdown und keine automatische Zwangswahl. Alle Spieler treffen ihre Entscheidung in ihrem eigenen Tempo.
* **Beitragsoptionen pro Runde:** Jeder Spieler wählt in der Investitionsphase (`INVESTMENT`) eine von drei diskreten Optionen bezogen auf das Startkapital:
  * **0%** (`0 Fr.`)
  * **5%** (`0.2 Fr.` – der faire Durchschnittsbeitrag)
  * **10%** (`0.4 Fr.`)
* **Manuelle Rundenfreigabe durch den Admin:** Sobald alle Spieler der Session ihre Entscheidung für die aktuelle Runde eingereicht haben (`allPlayersReadyForNextRound`), schaltet der Experimentator im Admin-Dashboard manuell in die nächste Runde weiter (`POST /api/admin/sessions/{id}/next-round`) bzw. schliesst nach Runde 10 das Experiment ab.

### 2.4 Informations-Sichtbarkeit & Lokaler Notizblock
* **Kein Gesamttopf während des Spiels:** Während das Spiel läuft (`RUNNING`), wird den Spielern die kumulierte Gesamtsumme im Klimakonto **nicht** angezeigt.
* **Nur letzte abgeschlossene Runde sichtbar:** Ab Runde 2 sehen die Spieler in der Rückblick-Tabelle ausschliesslich die Einzelbeiträge jedes Gruppenmitglieds sowie die Rundensumme der **unmittelbar vorangegangenen Runde** (nicht die gesamte Historie aller früheren Runden).
* **Lokaler Notizblock:** Um den Überblick über die eigenen Berechnungen und bisherige Zahlungen zu behalten, steht jedem Spieler auf der rechten Seite ein privater Notizblock zur Verfügung, der ausschliesslich lokal im Browser (`localStorage`) gespeichert wird.

---

## 3. Variante 2: Klimaabkommen (`WITH_CONTRACTS`)

Beim Erstellen einer Session im Admin-Dashboard kann zwischen **Variante 1: Normal** (`NORMAL`) und **Variante 2: Mit Verträgen (Klimaabkommen)** (`WITH_CONTRACTS`) gewählt werden. In Variante 2 gelten alle Regeln aus Variante 1 sowie zusätzlich folgende Abkommensmechanismen:

### 3.1 Freiwilliger Beitritt vor Runde 1 (`CONTRACT_JOIN`)
* Vor der 1. Investitionsrunde entscheidet jeder der 6 Spieler über den Beitritt zum Klimaabkommen zwischen **drei Optionen**:
  1. **«Ja» (`YES`):** Unbedingter Beitritt – der Spieler wird unabhängig von der Wahl der anderen immer Mitglied.
  2. **«Nur wenn min. die Hälfte dabei» (`CONDITIONAL`):** Bedingter Beitritt – der Spieler wird nur dann Mitglied, wenn insgesamt mindestens die Hälfte der Gruppe ($\ge 3$ von 6 Spielern mit `YES` oder `CONDITIONAL`) bereit ist beizutreten. Sind es weniger als 3 potenzielle Mitglieder, bleibt er Nichtmitglied.
  3. **«Nein» (`NO`):** Kein Beitritt – der Spieler spielt als Nichtmitglied.
* Ein späterer Beitritt in darauffolgenden Runden ist **nicht** möglich.
* Für alle Spieler ist jederzeit transparent ersichtlich, welche Gruppenmitglieder im Abkommen sind (`📜 Im Abkommen`) und wer ausgetreten ist (`Ausgetreten`).

### 3.2 Technisch erzwungener Mindestbeitrag (`INVESTMENT`)
* **Nichtmitglieder:** Wählen pro Runde frei zwischen **0%** (`0 Fr.`), **5%** (z. B. `0.20 Fr.` / `0.25 Fr.`) und **10%** (z. B. `0.40 Fr.` / `0.50 Fr.`).
* **Mitglieder:** Das Klimaabkommen verpflichtet jedes aktive Mitglied, pro Runde **mindestens 5%** des Startkapitals in das Klimakonto einzuzahlen. Die Option `0 Fr.` ist für aktive Abkommensmitglieder sowohl im Frontend gesperrt als auch im Backend technisch blockiert; wählbar sind somit nur die 5%- oder 10%-Option.

### 3.3 Optionaler Austritt ab Runde 2 (`POST /api/game/contract/leave`)
* In **Runde 1** ist ein Austritt noch nicht möglich, sodass jedes Mitglied mindestens in der ersten Runde seinen Pflichtbeitrag leistet.
* **Ab Runde 2** (nach der ersten Runde) können aktive Mitglieder das Abkommen jederzeit kostenlos verlassen – sowohl **vor** als auch **nach** dem Investieren in der jeweiligen Runde.
* Im Frontend ist diese Aktion bewusst dezent als untergeordnete Option (*«Aus dem Abkommen austreten»*) unterhalb der Beitragsauswahl platziert.
* Nach einem Austritt wechselt der Spieler dauerhaft in den Status **Nichtmitglied** (`inContract = false`, `leftContract = true`) und kann (sofern er in der aktuellen Runde noch nicht eingezahlt hat) ab sofort wieder `0 Fr.` wählen.

---

## 4. Spielende, Fragebogen & Auswertung (`FINISHED`)

### 4.1 Risiko-Wurf & Ergebnisberechnung
Nach Abschluss der 10. Runde beendet der Admin das Spiel:
* **Klimaziel erreicht ($\text{TotalPot} \ge \text{TargetAmount}$):** Der Klimawandel wurde erfolgreich abgewendet. Alle Spieler behalten ihr individuelles Restguthaben (`currentBalance`).
* **Klimaziel verfehlt ($\text{TotalPot} < \text{TargetAmount}$):** Das System zieht eine Zufallszahl (`0.0` bis `1.0`).
  * Mit **90% Wahrscheinlichkeit** tritt die Klimakatastrophe ein (`survivedRisk = false`) und alle Spieler verlieren ihr gesamtes Restguthaben (`Auszahlung = 0 Fr.`).
  * Mit **10% Wahrscheinlichkeit** haben die Spieler Glück (`survivedRisk = true`) und behalten ihr Restguthaben trotz verfehltem Ziel.

### 4.2 Wissenschaftlicher Abschluss-Fragebogen (`SurveyPage`)
Sobald das Spiel den Status `FINISHED` erreicht, wird jedem Spieler **vor** der Ergebnisanzeige automatisch ein Fragebogen angezeigt (ohne weiteres Hintergrund-Polling):
1. **Erwartete Wahrscheinlichkeit (`0%–100%`):** *"Mit welcher Wahrscheinlichkeit (0% - 100%) hast du erwartet, dass deine Gruppe das Ziel erreicht?"*
2. **Reaktion auf Ungleichheit (Single-Choice):** Verhalten, wenn andere weniger beigetragen haben (Trotzdem viel gezahlt / Beitrag reduziert / Geärgert, aber gleich weitergemacht / Nicht darauf geachtet).
3. **Motivations-Ranking (Drag & Drop):** Sortierung von 4 Faktoren nach Wichtigkeit (Eigener Gewinn, Schutz vor Klimaverlust, Fairness innerhalb der Gruppe, Verhalten der anderen Spieler).
4. **Zusatzfragen zum Klimaabkommen (nur in Variante 2 `WITH_CONTRACTS`, abhängig vom tatsächlichen Verhalten des Spielers):**
   * *Was war der wichtigste Grund, warum du dem Abkommen beigetreten bist?* (falls beigetreten)
   * *Was war der wichtigste Grund, warum du dem Abkommen nicht beigetreten bist?* (falls nicht beigetreten)
   * *Was war der wichtigste Grund für deinen Austritt aus dem Abkommen?* (falls ausgetreten)
   * *Was war der wichtigste Grund, warum du im Abkommen geblieben bist?* (falls bis zum Schluss geblieben)
5. **Demografie & Auszahlung:**
   * **Alter** (Pflichtfeld)
   * **Geschlecht:** `Mann` oder `Frau` (Pflichtfeld)
   * **Handynummer für Twint:** Optional für die Gewinnauszahlung.

### 4.3 Sofortige Ergebnisanzeige (`EndgamePage`)
Unmittelbar nach dem Absenden des Fragebogens (`POST /api/game/survey`) sieht der Spieler automatisch seinen individuellen Ergebnis-Screen mit:
* Gesamtsumme im Klimakonto vs. benötigtes Klimaziel (`12 Fr.`),
* Ausgang des Risiko-Entscheids (Ziel erreicht / Katastrophe eingetreten / Glück gehabt),
* Persönlicher Endauszahlung in `Fr.`,
* Vollständiger Runden-Historie aller 10 Runden.

---

## 5. Benutzeroberfläche & Design-System

### 5.1 Farbpalette & Geometrie
Das UI folgt einem klaren, kantigen ("boxy", minimale Rundungen von `2px`–`4px`) Editorial-Design auf Basis von vier Hauptfarben:
* `#F5EBDD` – **Warm Cream** (Seitenhintergrund)
* `#F2765E` – **Terracotta / Coral** (Primäre Aktionsfarbe, Akzente, leicht roter Hintergrund `#FCE8E4` für die aktuelle Aktionskarte)
* `#315B8C` – **Steel Blue** (Sekundärfarbe, Badges, Fokus-Ringe, Tabellen-Hervorhebungen)
* `#413333` – **Dark Espresso** (Typografie, Rahmenlinien, Header)

### 5.2 Aufbau der Spieler-Ansicht (`GamePage`)
* **Header:** Raumname, eigenes Pseudonym, aktuelle Runde (`X / 10`), Klimaziel (`12 Fr.`) und persönliches Restguthaben in `Fr.`.
* **Linke Hauptspalte (von oben nach unten):**
  1. **Aktuelle Aktions-Card (leicht roter Hintergrund `#FCE8E4`):** Je nach Phase entweder Beitrittsentscheidung zum Klimaabkommen (`CONTRACT_JOIN` vor Runde 1) oder Beitrag für das Klimakonto (`INVESTMENT`, inkl. dezentem Austritts-Link ab Runde 2 für aktive Mitglieder). Eine kompakte Hinweisbox zeigt den aktuellen eigenen Abkommensstatus sowie kurzzeitige Abkommens-Meldungen.
  2. **Rückblick letzte Runde (`HistoryTable`):** Zeigt ab Runde 2 die Einzelbeiträge und die Rundensumme der jeweils letzten abgeschlossenen Runde.
  3. **Status der Gruppe (`PlayerStatusList`):** Unterste Card mit den 6 anonymisierten Spielern, ihrem Abkommens-Badge (`📜 Im Abkommen` / `Ausgetreten`) und ihrem aktuellen Entscheidungsstatus (`Entschieden` / `Überlegt...`).
* **Rechte Seitenspalte:** Privater, lokal gespeicherter **Notizblock** (`LocalNotepad`) für eigene Berechnungen.

### 5.3 Admin-Dashboard & CSV-Export (`AdminPage`)
* **Passwortschutz:** Zugriff über das in `application.yml` definierte Admin-Passwort (`app.admin-secret`).
* **Session-Erstellung:** Wahl des Raumnamens und des Spielmodus (`NORMAL` oder `WITH_CONTRACTS`).
* **Live-Steuerung:** Übersicht über alle Räume, verbundene Spieler, deren Bereitschaftsstatus in der aktuellen Runde (`✓ Hat gewählt` / `⏳ Überlegt...`), Button zum Starten des Spiels sowie Button `"Nächste Runde freigeben"` bzw. `"Experiment abschliessen"`.
* **Vollständiger CSV-Export (`export.csv`):** Exportiert pro Session eine Excel-kompatible CSV-Datei (UTF-8 mit BOM) mit allen Spielerdaten, Rundenbeiträgen, Abkommensstatus (`contract_join_choice`, `ever_joined_contract`, `left_contract`, `in_contract_at_end`, `left_contract_at_round`), Endergebnis sowie sämtlichen Antworten aus dem Abschluss-Fragebogen.
