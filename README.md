# Collective-Risk Social Dilemma Experiment

Eine webbasierte Full-Stack-Plattform zur Durchführung des spieltheoretischen Klima-Experiments nach **Milinski et al. (2008)** (*"The collective-risk social dilemma and the maintenance of global climate"*).

Entwickelt im Rahmen einer Berufsmaturitätsarbeit (BMA) mit **Java 21 / Spring Boot 3.4**, **PostgreSQL 16** und **React 18 / TypeScript (Vite)**.

---

## Überblick & Features

In Gruppen von standardmässig **6 Spielern** erhält jede Person ein Startkapital von **4 Fr.** Über **10 Runden** hinweg entscheiden die Spieler gleichzeitig und anonym (unter Wetter-/Klima-Pseudonymen wie *Monsun*, *Passat*, *Taifun*), ob sie in der jeweiligen Runde **0 Fr.**, **0.2 Fr.** oder **0.4 Fr.** in ein gemeinsames Klimakonto einzahlen.

* **Das kollektive Ziel:** Erreicht die Gruppe nach 10 Runden gemeinsam mindestens **12 Fr.** (50 % des gesamten Startkapitals), wird die Klimakatastrophe abgewendet und alle Spieler behalten ihr verbliebenes Restguthaben.
* **Das Risiko:** Wird das Ziel verfehlt, verlieren alle Spieler mit einer Wahrscheinlichkeit von **90 %** ihr gesamtes Restguthaben.
* **Zwei Spielvarianten:**
  * **Variante 1 – Standard (`NORMAL`):** Klassisches Experiment ohne Verträge.
  * **Variante 2 – Mit Klimaabkommen (`WITH_CONTRACTS`):** Vor Runde 1 können Spieler freiwillig einem Klimaabkommen beitreten, das einen Mindestbeitrag von **0.2 Fr.** pro Runde technisch erzwingt. Zwischen zwei Runden können Mitglieder entweder im Abkommen bleiben, gegen eine Gebühr von **0.2 Fr.** an jedes verbleibende Mitglied austreten oder eine Abstimmung zur kostenfreien Abschaffung des Abkommens (einfache Mehrheit $> 50\%$) starten.
* **Informationsdesign wie im Originalexperiment:** Während des Spiels sehen die Spieler **nicht** die kumulierte Gesamtsumme im Klimakonto und im Rückblick jeweils **nur die letzte abgeschlossene Runde**. Für eigene Berechnungen steht ein lokaler Notizblock im Browser zur Verfügung.
* **Manuelle Rundensteuerung durch den Experimentator:** Keine Zeitdruck-Timer oder Zwangs-Entscheidungen – der Admin gibt die nächste Runde im Dashboard frei, sobald alle 6 Spieler ihre Wahl getroffen haben.
* **Integrierter Abschluss-Fragebogen & CSV-Export:** Nach Runde 10 füllen alle Teilnehmenden direkt in der App einen Fragebogen aus (inkl. Drag-&-Drop-Motivationsranking, Vertragsfragen in Variante 2 sowie Demografie & Twint-Nummer) und sehen unmittelbar danach ihr individuelles Endergebnis. Alle Spiel- und Fragebogendaten lassen sich im Admin-Dashboard als Excel-kompatible CSV-Datei exportieren.

---

## Schnellstart (Docker Compose)

Die gesamte Applikation (PostgreSQL, Spring Boot Backend und Nginx/React Frontend) lässt sich mit einem einzigen Befehl starten:

```bash
docker compose up --build
```

Nach dem Start sind folgende Dienste erreichbar:
* **Spieler-Ansicht (Lobby & Spiel):** [http://localhost:3000](http://localhost:3000)
* **Admin-Dashboard (Spielleitung):** [http://localhost:3000/admin](http://localhost:3000/admin) *(Standard-Passwort: `climategame2026`)*
* **Backend API (direkt):** `http://localhost:8080/api`

Um alle Container zu stoppen und die Datenbank zurückzusetzen:
```bash
docker compose down -v
```

---

## Konfiguration

Sämtliche Einstellungen befinden sich zentral in [`backend/src/main/resources/application.yml`](backend/src/main/resources/application.yml) und können bei Bedarf über Umgebungsvariablen in [`docker-compose.yml`](docker-compose.yml) überschrieben werden:

| Eigenschaft in `application.yml` | Umgebungsvariable | Standardwert | Beschreibung |
| :--- | :--- | :--- | :--- |
| `app.admin-secret` | `APP_ADMIN_SECRET` | `climategame2026` | Passwort für das Admin-Dashboard (`/admin`) |
| `app.game.start-amount-chf` | `ENV_START_AMOUNT` | `4` | Startkapital pro Spieler in Franken (`4 Fr.`) |
| `app.game.group-size` | `ENV_GROUP_SIZE` | `6` | Anzahl Spieler pro Session |
| `app.game.total-rounds` | `ENV_TOTAL_ROUNDS` | `10` | Anzahl zu spielender Runden |
| `app.game.risk-level` | `ENV_RISK_LEVEL` | `0.90` | Verlustwahrscheinlichkeit (90 %) bei verfehltem Klimaziel |

---

## Lokale Entwicklung (ohne Docker)

### Voraussetzungen
* **Java 21** & **Maven 3.9+**
* **Node.js 22+** & **npm**
* **PostgreSQL 16** (lokal laufend auf Port `5432`, Datenbank `climategame`, User `postgres`, Passwort `postgres`)

### 1. Backend starten & testen
```bash
cd backend
mvn spring-boot:run
```
Unit-Tests ausführen:
```bash
cd backend
mvn test
```

### 2. Frontend starten & bauen
```bash
cd frontend
npm install
npm run dev
```
Produktions-Build prüfen:
```bash
cd frontend
npm run build
```

---

## Projektstruktur

```text
climate-crd-game/
├── backend/                        # Spring Boot 3.4 (Java 21) REST-Backend
│   ├── src/main/java/ch/bbw/climategame/
│   │   ├── config/                 # SecurityConfig & AdminAuthenticationFilter
│   │   ├── controller/             # LobbyController, GameController, AdminController
│   │   ├── dto/                    # Request/Response Records
│   │   ├── model/                  # JPA Entities (GameSession, Player, RoundInvestment, PlayerSurvey)
│   │   ├── repository/             # Spring Data JPA Repositories
│   │   └── service/                # GameService (Spiellogik, Verträge, CSV-Export, 30s Inaktivitäts-Kick)
│   └── src/main/resources/
│       └── application.yml         # Zentrale Applikations- und Spielkonfiguration
├── frontend/                       # React 18 + TypeScript + Vite Frontend
│   └── src/
│       ├── api/                    # Typisierter Fetch-Client
│       ├── components/             # UI-, Spieler- und Admin-Komponenten
│       ├── context/                # GameContext (State & gezieltes Polling)
│       ├── pages/                  # LobbyPage, GamePage, SurveyPage, EndgamePage, AdminPage
│       ├── styles/                 # Design-System (tokens.css, main.css)
│       └── utils/                  # Franken-Währungsformatierung (currency.ts)
├── docker-compose.yml              # Multi-Container-Orchestrierung (Postgres, Backend, Frontend)
├── spec.md                         # Fachliche System- und Spielspezifikation
└── plan.md                         # Technischer Architektur- und Implementierungsplan
```
