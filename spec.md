# Spezifikation: Replikation "Collective-Risk Social Dilemma" (Milinski-Experiment)

## Experiment on basis of:
The collective-risk social dilemma and the prevention
of simulated dangerous climate change
**Link to paper**: https://www.pnas.org/doi/10.1073/pnas.0709546105


## 1. Spielmechanik und Kernparameter

### 1.1 Kernvariablen und Finanz-Logik
Das System verwendet relative Ableitungen basierend auf einem zentralen Startbetrag. 

**Umgebungsvariablen (Environment Variables):**
* `ENV_START_AMOUNT`: Startkapital pro Spieler in CHF (Integer/Float, Default: 4)
* `ENV_GROUP_SIZE`: Anzahl der Spieler pro Gruppe (Integer, Default: 6)
* `ENV_TOTAL_ROUNDS`: Anzahl der Spielrunden (Integer, Default: 10)

**Abgeleitete System-Variablen (berechnet beim Session-Start):**
* `TARGET_SUM` (Kollektives Ziel): `(ENV_START_AMOUNT / 2) * ENV_GROUP_SIZE`
  *(Logik: Die Gruppe muss gemeinsam genau die Hälfte ihres Gesamt-Startkapitals opfern, um das Ziel zu erreichen).*
* `INVEST_OPTION_0`: `0` (Kostenlos / Trittbrettfahrer)
* `INVEST_OPTION_1`: `ENV_START_AMOUNT * 0.05` (Fairer Anteil)
* `INVEST_OPTION_2`: `ENV_START_AMOUNT * 0.10` (Altruistischer Anteil)

### 1.2 State Management & Timer-Logik
Um das Experiment bei Verbindungsabbrüchen oder Inaktivität einzelner Spieler am Laufen zu halten, implementiert das System einen zweistufigen Runden-Timer:
* **Reguläre Phase:** 60 Sekunden. Die Spieler werden aufgefordert, ihre Investition für die aktuelle Runde zu tätigen.
* **Overtime-Phase:** 30 Sekunden. Läuft die reguläre Phase ab, startet ein visuell hervorgehobener Countdown.
* **Fallback (Auto-Bet):** Tätigt ein Spieler nach insgesamt 90 Sekunden keine Eingabe, loggt das System serverseitig automatisch den mittleren Investitionswert ein (entspricht `INVEST_OPTION_1` / dem fairen Anteil). Der Status des Spielers wird auf "ready" gesetzt und die Gruppe rückt in die nächste Runde vor.

### 1.3 Währung, State-Tracking und Payout
* **Währung (Backend):** Alle Berechnungen im Backend basieren auf der Basiswährung (CHF).
* **Abrechnungs-Logik:** Das System trackt das individuelle Restguthaben über alle 10 Runden. 
    * *Szenario A (Ziel erreicht):* Guthaben = Startkapital - getätigte Investitionen.
    * *Szenario B (Ziel verfehlt, Risiko tritt NICHT ein):* Guthaben = Startkapital - getätigte Investitionen.
    * *Szenario C (Ziel verfehlt, Risiko tritt ein):* Guthaben = 0.
* **Endbildschirm (Payout):** Nach Abschluss des Experiments wird dem Spieler sein individueller Endbetrag angezeigt. Als Identifikator dient lediglich die zugewiesene Spielernummer (z. B. "Spieler 3") sowie der Session-Code.

### 1.4 UI-Restriktionen & Kognitive Last
Um die psychologische Dynamik des Original-Experiments zu replizieren, gelten strikte UI-Vorgaben:
* **Kein Fortschrittsbalken:** Die aktuell kumulierte Gesamtsumme des "Klima-Accounts" wird den Spielern im Interface **niemals** angezeigt.
* **Transparenz der Mitspieler:** Nach jeder Runde werden die Einzelinvestitionen aller 6 Spieler der vergangenen Runde in einer statischen Reihenfolge angezeigt, damit spezifische Verhaltensmuster identifizierbar bleiben.
* **Digitaler Notizblock:** Jeder Spieler erhält im UI ein dauerhaftes Freitext-Feld (`<textarea>`), das über alle 10 Runden hinweg sichtbar bleibt. Hier können die Spieler eigene Berechnungen anstellen.

## 2. Architektur & Datenhaltung
Aufgrund des kontrollierten Settings (Versuchsleiter physisch anwesend) wird die Architektur bewusst schlank gehalten.

**2.1 Frontend & Lokaler State (Spielfeld)**
* Das Notizfeld der Spieler ist rein lokal. Es wird im Browser-State gehalten und **nicht** an den Server übermittelt.
* Auf dem Endbildschirm wird lediglich die zugewiesene Spielernummer und der auszuzahlende Endbetrag groß und deutlich angezeigt.

**2.2 Backend & Datenbank-Schema (Minimal)**
Das Backend muss nur die Spielmechanik validieren und den Runden-State synchronisieren. Zu speichernde Daten pro Session:
* `session_id`: Eindeutige ID für die 6er-Gruppe (Spielcode).
* `current_round`: Integer (1-10).
* `risk_level`: Das aktive Treatment (z. B. 90%, 50% oder 10%).
* `players`: Array von 6 Objekten (Spieler 1 bis 6).
* `investments`: Log der getätigten Einsätze pro Spieler und Runde.

## 3. Matchmaking & Admin-Dashboard
Um einen kontrollierten Start und reibungslosen Ablauf zu garantieren, wird ein dedizierter Admin-Bereich implementiert.

**3.1 Lobby-System & Zugang**
* **Raum-Erstellung:** Der Admin erstellt im Backend eine neue Session. Das System generiert einen eindeutigen, kurzen Spielcode (z. B. "X7K9").
* **Spieler-Login:** Die 6 Probanden rufen die Startseite der Web-App auf, geben den Spielcode ein und landen in einem Warteraum (Lobby). Das System weist ihnen automatisch eine Spielernummer (1 bis 6) zu.

**3.2 Admin-Steuerung (Session Management)**
* **Spielstart:** Sobald der Admin sieht, dass alle 6 Slots gefüllt sind, löst er den Spielstart manuell per Button aus.
* **Pausen-Funktion:** Der Admin verfügt über einen "Pause"-Button. Bei technischen Schwierigkeiten friert dieser Button das Spiel serverseitig ein.

**3.3 Währungsdarstellung (UI)**
* Im Frontend werden alle Beträge für die Spieler in **Rappen** dargestellt, um eine intuitive, ganzzahlige Darstellung zu gewährleisten (z. B. 400 Rappen Startkapital; Spenden-Optionen 0, 20 oder 40 Rappen).

## 4. Edge Cases, Reconnects & Datenexport

**4.1 Pausen-Verhalten (Freeze-State)**
* Löst der Admin die Pause aus, legt sich bei allen aktiven Spielern sofort ein blockierendes Fullscreen-Overlay ("Spiel pausiert durch Admin") über das Interface.
* Der serverseitige Runden-Timer wird exakt an der aktuellen Millisekunde eingefroren.
* Bei Aufhebung der Pause durch den Admin verschwindet das Overlay und der Timer läuft nahtlos weiter.

**4.2 Reconnects & Session-Recovery**
* Das System speichert die Session-Zugehörigkeit (Spielcode) und die zugewiesene Spielernummer (1 bis 6) persistent in einem lokalen Browser-Cookie.
* Schließt ein Spieler den Tab oder verliert die Verbindung, ruft er die URL erneut auf. Das System liest das Cookie aus, umgeht die Startseite und lädt direkt den korrekten UI-State der aktuellen Runde für diesen spezifischen Slot neu.

**4.3 Datenexport (BMA-Auswertung)**
* Das Admin-Dashboard bietet pro abgeschlossener Session einen "Download CSV"-Button.
* Die exportierte Datei enthält eine tabellarische Aufschlüsselung aller 10 Runden: Spielernummer, getätigtes Investment pro Runde, das gespielte Risiko-Treatment, Angabe ob das Ziel erreicht wurde sowie das finale Auszahlungsguthaben.

## 5. Netzwerk & Infrastruktur
Um in restriktiven Schulnetzwerken fehlerfrei zu funktionieren und den Datenschutz zu garantieren:

* **5.1 Kommunikation (Polling):** Das System verwendet robustes HTTP-Polling (oder Long-Polling), um den Timer und den Runden-State zwischen Server und Clients zu synchronisieren.
* **5.2 Deployment (Docker Compose):** Die gesamte Anwendung (Frontend, Backend, Minimal-Datenbank) wird containerisiert und lässt sich auf einem lokalen Rechner/Server mit einem einzigen Befehl (`docker-compose up`) starten.
* **5.3 Datenschutz (Zero-Tracking):** Es dürfen keinerlei externe Analyse-Tools (Analytics, Sentry etc.) oder externe CDNs eingebunden werden. Sämtliche Daten verbleiben lokal.

## 6. Das Milinski-Endgame (Resolution)
Nach Ablauf der 10. Runde berechnet der Server das Resultat und zeigt einen statischen Endbildschirm an.

* **Szenario A (Ziel erreicht):** Text bestätigt den Erfolg. Das persönliche Restguthaben wird zur Auszahlung angezeigt.
* **Szenario B (Ziel verfehlt - Risiko-Wurf):** Der Server berechnet basierend auf dem eingestellten Treatment ein Zufallsergebnis.
  * *Risiko tritt NICHT ein:* Spieler behalten ihr Restguthaben. Text: "Ziel verfehlt. Sie hatten Glück: Ihr Restguthaben bleibt erhalten."
  * *Risiko tritt ein:* Das Restguthaben wird serverseitig auf 0 gesetzt. Text: "Ziel verfehlt. Ihr Geld ging durch das [X]% Risiko verloren. Auszahlung: 0 Rappen."
  * *Darstellung:* Es werden keine komplexen Animationen verwendet, das Ergebnis wird als unmissverständlicher Textblock kommuniziert.