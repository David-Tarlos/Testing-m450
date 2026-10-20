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

## Aufgabe 2 – Rezept editieren

### Backend (PR 2)

Neuer Endpoint `PUT /api/recipes/{recipeId}`:
- Existiert die Id, ersetzt er Name, Beschreibung, Bild-URL und die ganze
  Zutatenliste und antwortet mit 200 und dem gespeicherten Rezept.
- Unbekannte Id → 404, ungültige Einheit → 400.
- Test `RecipeControllerUpdateTest` (MockMvc): Ersetzen inklusive Zutaten
  und der 404-Fall.

### Frontend (PR 3)

- „Edit Details“ ist ein Link auf `/recipes/:recipeId/edit`.
- `AddRecipe` lädt dort das Rezept per GET, füllt das Formular und speichert
  per PUT.
- Bei unbekannter oder ungültiger Id erscheint eine Meldung, und Submit
  bleibt gesperrt.

### Bewusst so entschieden

| Entscheidung | Alternative | Warum so |
|---|---|---|
| `PUT /api/recipes/{recipeId}` | `/api/recipes/recipe/{id}` wie beim bestehenden GET, oder `PATCH` | Das ist der übliche REST-Pfad. Der bestehende GET-Pfad mit `/recipe/` ist eher ein Ausreisser. |
| PUT ersetzt alles; fehlende Felder werden `null` | nur mitgeschickte Felder ändern (PATCH-Semantik) | Das entspricht der PUT-Semantik, und das Frontend schickt sowieso immer das ganze Rezept. |
| Zutaten werden immer neu angelegt, Ids aus dem Body werden verworfen | Zutaten per Id abgleichen (ändern / neu / löschen) | Viel einfacher. Ausserdem kann ein Client so keine Zutat eines fremden Rezepts „umhängen“. Nachteil: Zutat-Ids ändern sich bei jedem Speichern. Das stört nicht, weil nichts auf sie verweist. |
| Bestehende Zutaten-Collection leeren und füllen, statt sie zu ersetzen | `entity.setIngredients(neueListe)` | Mit `orphanRemoval = true` wirft Hibernate sonst eine Exception. Ein Kommentar im Code erklärt das. |
| Id kommt aus dem Pfad, eine Id im Body wird ignoriert | 400, wenn Pfad-Id und Body-Id verschieden sind | KISS. Das Frontend schickt gar keine Id im Body. |
| 404 bei unbekannter Id | 200 mit leerem Body wie beim GET | Der Client soll den Fehler erkennen können. Das GET habe ich bewusst nicht angepasst (anderes Ticket). |
| Service gibt `Optional<Recipe>` zurück, der Controller macht daraus 404 | Exception (`ResponseStatusException`) im Service | Der Service kennt so kein HTTP. |
| Keine Bean-Validation (z.B. leerer Name) | `@Valid` + `spring-boot-starter-validation` | Gleich wie beim bestehenden POST. Sinnvoll, aber für beide Endpoints ein eigenes Ticket. |
| Frontend: `AddRecipe` wird für Anlegen und Bearbeiten wiederverwendet (Modus über `recipeId` in der URL) | eigene `EditRecipe`-Komponente oder gemeinsame `RecipeForm`, die von beiden genutzt wird | DRY, nur ~40 Zeilen Diff. Der Name `AddRecipe` passt jetzt nicht mehr ganz. Eine Umbenennung in `RecipeForm` würde den PR aber aufblähen. |
| Route `/recipes/:recipeId/edit` | `/edit-recipe/:id` oder Query-Parameter | Die Route ist lesbar und lässt sich später erweitern (z.B. `/recipes/:id`). |
| Submit bleibt gesperrt, bis das Rezept geladen ist | Formular erst rendern, wenn die Daten da sind | So kann ein leeres Formular nie ein bestehendes Rezept überschreiben. |
| `comment` der Zutaten wird unsichtbar mitgeführt | Feld anzeigen | Sonst würde jedes Speichern den Kommentar löschen. |
| Aufräumen in `App.js`/`Browse.js` (tote Imports, fehlender `key`) | separater PR | Boy-Scout-Rule. Nebeneffekt: `@testing-library/react` fliegt aus dem Produktions-Bundle (−48 kB gzip). |

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
- Wechselt man per Client-Navigation direkt von „Edit“ zu „Add“, bliebe der
  State erhalten. Das tritt heute nicht auf, weil die Navbar die Seite neu
  lädt (siehe oben). Ein `key` auf der Route würde das beheben.
- **Keine automatisierten Frontend-Tests.** Getestet habe ich manuell im
  Browser. Fürs Review heisst das: Die Frage „Existieren Tests?“ ist fürs
  Frontend mit Nein zu beantworten, fürs Backend mit Ja (nur der neue
  Endpoint).

## Verifikation

- `npm run build` läuft durch. Auf PR 1 bleiben nur Warnungen in `App.js`
  und `Browse.js` (ungenutzte Imports aus dem Ausgangsstand).
  Auf PR 3 läuft er ohne jede Warnung.
- `mvn clean compile` und `mvn test` (2/2) laufen durch. Den PUT habe ich
  zusätzlich per curl getestet: bestehende Id, unbekannte Id, ohne Zutaten,
  ungültige Einheit.
- Im Browser getestet:
  - Rezept mit Zutat angelegt, es erscheint in der Übersicht und im
    API-Response.
  - Ein leeres Formular wird blockiert.
  - Ist das Backend nicht erreichbar, erscheint eine Fehlermeldung.
  - Rezept bearbeitet (Name + neue Zutat): Die Anzahl der Rezepte bleibt
    gleich, und der Zutat-Kommentar bleibt erhalten.
  - Unbekannte und ungültige Id im Edit-Link zeigen eine Meldung.
- Getestet habe ich auf den Ports 8081/3001, weil auf 8080/3000 andere
  Projekte liefen. Die Port-Änderungen waren nur lokal und sind nicht
  committet.

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
