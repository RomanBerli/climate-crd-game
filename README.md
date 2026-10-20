# Collective-Risk Social Dilemma (Milinski-Experiment)

Webbasierte Replikation des **Milinski Collective-Risk Social Dilemma** Experiments (*Milinski et al., PNAS 2008*) für 6er-Gruppen über 10 Runden, gemäss [spec.md](spec.md) und [plan.md](plan.md).

---

## Schnellstart (Docker Compose)

Die gesamte Anwendung (`postgres`, `backend`, `frontend`) lässt sich mit einem einzigen Befehl starten:

```bash
docker-compose up --build
```

* **Spieler-Ansicht (Lobby & Spiel):** [http://localhost:3000](http://localhost:3000)
* **Versuchsleiter / Admin-Cockpit:** [http://localhost:3000/admin](http://localhost:3000/admin)
  * Standard-Admin-Schlüssel (`X-Admin-Key`): `admin_geheim_123`
* **Backend REST-API:** `http://localhost:8080/api/...`

---

## Kernfunktionen & Wissenschaftliche Spezifikation

1. **Ganzzahlige Rappen-Darstellung (Spec 1.1 & 3.3):**
   * Startkapital: `400 Rappen` (4 CHF) pro Spieler.
   * Runden-Optionen: `0 Rappen`, `20 Rappen` (Fairer Anteil) oder `40 Rappen`.
   * Kollektives Gruppenziel nach 10 Runden: `1200 Rappen` (bei 6 Spielern).
2. **Strikte UI-Restriktionen (Spec 1.4):**
   * **Kein Fortschrittsbalken / kein Gesamttopf für Spieler:** Die aktuelle Gesamtsumme des Klimakontos wird im Spieler-UI niemals angezeigt.
   * **Statische Transparenz-Matrix:** Nach jeder Runde werden die Einzelbeiträge aller 6 Spieler in statischer Reihenfolge (`Spieler 1` bis `Spieler 6`) tabellarisch dargestellt.
   * **Lokaler Digitaler Notizblock:** Dauerhaftes `<textarea>`-Notizfeld pro Spieler, das rein lokal im Browser (`localStorage`) gehalten und niemals an den Server übertragen wird.
3. **Zweistufiger Runden-Timer & Auto-Bet (Spec 1.2):**
   * **60s Reguläre Phase** -> **30s Overtime-Phase** (visuell hervorgehoben).
   * Nach 90s Inaktivität bucht der Server automatisch den mittleren Beitrag (`20 Rappen`, `is_auto_bet = true`) ein.
4. **Admin-Steuerung, Freeze-Pause & BMA-CSV-Export (Spec 3 & 4):**
   * Raum-Erstellung mit Risiko-Treatment (`90%`, `50%` oder `10%`).
   * Manueller Spielstart bei `6/6` verbundenen Spielern.
   * Millisekundengenaues Einfrieren (`Pause` / `Resume`) mit blockierendem Fullscreen-Overlay bei allen Spielern.
   * One-Click CSV-Export (`/api/admin/sessions/{id}/export.csv`) aller Runden- und Auszahlungsdaten.
