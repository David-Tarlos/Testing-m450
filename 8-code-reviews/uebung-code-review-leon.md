# Übung Code-Review – Lösung Leon

Recipe Planner (React + Spring Boot) aus Kapitel 7, Projekt unter
`recipe-planner-fronend-and-backend/`. David hat dieselben Features unabhängig
umgesetzt (eigene Datei `uebung-code-review.md`). Diese Datei beschreibt nur
meine Lösung.

> Zur Unabhängigkeit: Davids Code und Doku habe ich vor meiner Umsetzung nicht
> gelesen. Beim Prüfen des Repo-Zustands habe ich aber in `git log` die
> Betreffzeilen seiner Commits gesehen, darunter den Pfad seines
> PUT-Endpoints.

## Pull Requests

| PR | Branch | Ziel-Branch | Inhalt |
|----|--------|-------------|--------|
| 1 | `feature/add-recipe-leon` | `main` | Aufgabe 1: Rezept anlegen (Frontend) + diese Doku |
| 2 | `feature/edit-recipe-backend-leon` | `main` | Aufgabe 2: PUT-Endpoint (Backend) |
| 3 | `feature/edit-recipe-frontend-leon` | `feature/add-recipe-leon` | Aufgabe 2: Rezept bearbeiten (Frontend) |

Die Branch-Namen haben das Suffix `-leon`, weil Davids Branches mit den
vorgegebenen Namen schon im gemeinsamen Repo liegen.

## Aufgabe 1 – Rezept hinzufügen (Frontend)

Ausgangslage: `AddRecipe.js` sah fertig aus, war aber eine Attrappe. Die
Felder hatten keinen State, es gab kein `onSubmit`, und der Submit-Button tat
nichts.

Umgesetzt:
- Alle Felder (Rezept und Zutaten) sind controlled und hängen am State.
- Das Formular sendet per `POST /api/recipes` und springt danach auf die
  Übersicht.
- Bei einem Fehler erscheint eine Meldung, die Eingaben bleiben erhalten.
  Während des Speicherns ist der Submit-Button gesperrt.
- Pflichtfelder: Rezeptname, Zutatname, Menge (ganze Zahl ≥ 1).
- Die Backend-URL steht neu einmal zentral in `src/api.js`.

### Bewusst so entschieden

| Entscheidung | Alternative | Warum so |
|---|---|---|
| State mit `useState`, `react-hook-form` entfernt | `react-hook-form` mit `useFieldArray` | Der Ausgangscode hatte schon `formData` mit Zutatenliste. Zwei State-Quellen für dasselbe Formular wären verwirrend (KISS). |
| Validierung über HTML5 (`required`, `min`, `type=url`) | eigene Validierung oder `react-hook-form` mit Fehlermeldungen | Kein eigener Code nötig. Nachteil: Die Meldungen kommen vom Browser und erscheinen in dessen Sprache. |
| Zutat-Felder heissen im State wie im Backend (`name`, `unit`, `amount`) | Feldnamen aus dem Ausgangscode (`ingredient`, `quantity`) behalten und beim Senden umbenennen | So gibt es kein Mapping. Nur `listId` (React-Key) wird vor dem Senden entfernt. |
| Einheiten als Konstante `UNITS` im Frontend | per Endpoint vom Backend laden | YAGNI: Das Enum ändert sich kaum. Ein Kommentar verweist auf das Backend-Enum. |
| `src/api.js` mit fester URL | `.env` mit `REACT_APP_API_URL` | Zwei Komponenten brauchen die URL, also DRY. Umgebungsvariablen braucht es erst mit einem Deployment. |
| Nach dem Speichern zurück auf die Übersicht | auf dem Formular bleiben und eine Erfolgsmeldung zeigen | Man sieht das neue Rezept sofort in der Liste. |
| `comment` einer Zutat nicht im Formular | zusätzliches Feld | Die Übersicht zeigt den Kommentar nirgends an (YAGNI). |
| Kleine Aufräumarbeiten im selben PR (tote Imports, doppelte `id`s, Tippfehler „Quanity“) | separater PR | Boy-Scout-Rule, aber nur in Dateien, die das Feature ohnehin ändert. |

## Bewusst nicht gemacht

Diese Punkte fallen auf, gehören aber nicht zum Feature, sondern in ein
eigenes Ticket:
- Navbar nutzt `href` statt Router-`Link` → jeder Klick lädt die Seite neu.
- `POST /api/recipes` antwortet mit 200 statt 201. Es gibt keine
  Backend-Validierung, ein leerer Name ist also per API möglich.
- `GET /api/recipes/recipe/{id}` gibt bei unbekannter Id 200 mit leerem Body
  statt 404 zurück.
- Ordnername `recipe-planner-fronend` (Tippfehler), Route `/new-menues`.
- `Planer` ist leer. `propTypes`/`defaultProps` sind leere Platzhalter, und
  React meldet `defaultProps` als veraltet.
- Die Labels im Formular sind weiss auf weissem Hintergrund (CSS aus dem
  Ausgangsstand).
- **Keine automatisierten Frontend-Tests.** Getestet habe ich manuell im
  Browser. Fürs Review heisst das: Die Frage „Existieren Tests?“ ist fürs
  Frontend mit Nein zu beantworten.

## Verifikation

- `npm run build` läuft durch. Auf PR 1 bleiben nur Warnungen in `App.js`
  und `Browse.js` (ungenutzte Imports aus dem Ausgangsstand).
- Im Browser getestet: Rezept mit Zutat angelegt, es erscheint in der
  Übersicht und im API-Response. Ein leeres Formular wird blockiert. Ist das
  Backend nicht erreichbar, erscheint eine Fehlermeldung.

## Review-Checkliste

Beim Review jedes PRs durchgehen:

- [ ] **Funktion:** Tut der Code, was das Feature verlangt? Lokal
  ausprobiert?
- [ ] **Logik:** Sind Randfälle abgedeckt (leere Liste, unbekannte Id,
  Backend down)?
- [ ] **Naming:** Sagen Variablen, Funktionen und Routen, was sie sind?
- [ ] **Lesbarkeit:** Versteht man den Code ohne Erklärung? Sind
  Kommentare dort, wo das *Warum* nicht offensichtlich ist?
- [ ] **Tote Reste:** Gibt es ungenutzte Imports, Variablen oder
  auskommentierten Code?
- [ ] **Tests:** Gibt es Tests? Decken sie den Hauptfall und einen
  Fehlerfall ab?
- [ ] **YAGNI / DRY / KISS:** Gibt es etwas, das (noch) niemand braucht?
  Duplikate? Geht es einfacher?
- [ ] **SRP:** Hat jede Funktion/Komponente eine Aufgabe?
- [ ] **Boy-Scout-Rule:** Ist der Code sauberer als vorher, ohne dass der
  PR ausufert?
- [ ] **Typos:** In Code, UI-Texten und Commit-Messages.

Zum Ton:
- Konstruktiv formulieren: das Problem und einen Vorschlag nennen, nicht die
  Person.
- Geschmacksfragen als Vorschlag markieren („Vorschlag:“, „nit:“), nicht als
  Forderung.
- Auch Gutes erwähnen.

## Erkenntnisse aus dem Review

*Wird nach dem gegenseitigen Review zu zweit ausgefüllt.*
