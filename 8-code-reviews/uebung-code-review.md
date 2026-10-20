# Übung Code-Review — Recipe Planner

> **Aufgabenstellung**
>
> *Arbeiten Sie zu zweit. Beide implementieren dasselbe Feature unabhängig voneinander.*
>
> **Aufgabe 1** — Frontend: Feature „ein neues Recipe hinzufügen", je ein Pull Request,
> gegenseitig reviewen, Erkenntnisse zusammentragen.
>
> **Aufgabe 2** — Frontend *und* Backend: Feature „ein bestehendes Recipe editieren",
> je ein PR pro Applikation, gleiches Vorgehen.

Projekt: [`recipe-planner-fronend-and-backend/`](recipe-planner-fronend-and-backend/) aus der
Pipeline-Übung (React-Frontend, Spring-Boot-Backend, H2 im Speicher).

Das Projekt war noch gar nicht im Repository. Es liegt jetzt **unverändert** als eigener Commit
auf `main` — sonst würde der erste PR-Diff aus dem kompletten Projekt bestehen statt aus dem Feature.

Zwei weitere Commits liegen direkt auf `main`, weil sie zu keinem der beiden Features gehören:

* `node_modules/`, `build/` und `.DS_Store` im `.gitignore`.
* Ein Fix an der ESLint-Config. `npm start` und `npm run build` brachen im gelieferten Projekt mit
  *„Environment key `jest/globals` is unknown"* ab — die App liess sich also gar nicht starten.
  Ursache ist `eslint-plugin-jest` 25: es lädt `@typescript-eslint/type-utils` und setzt damit
  TypeScript voraus, das in diesem JS-Projekt nicht installiert ist. Das Plugin stürzt beim Laden ab
  und übrig bleibt die irreführende Meldung. Ohne `react-app/jest` in der `eslintConfig` laufen
  Build und Dev-Server wieder, die restlichen `react-app`-Regeln bleiben aktiv.

---

## Die drei Branches

| Branch | Basis | Aufgabe | Umfang |
|---|---|---|---|
| `feature/add-recipe` | `main` | 1 — Rezept anlegen (Frontend) | 4 Dateien, +153 / −118 |
| `feature/edit-recipe-backend` | `main` | 2 — `PUT`-Endpoint (Backend) | 3 Dateien, +55 / −8 |
| `feature/edit-recipe-frontend` | `feature/add-recipe` | 2 — Edit-Formular (Frontend) | 8 Dateien, +192 / −129 |

Die Theorie nennt 200–400 geänderte Zeilen als die Grösse, bei der ein Review noch gute Ergebnisse
bringt. Alle drei PRs liegen darunter oder knapp darin — deshalb drei kleine statt einem grossen.

**Merge-Reihenfolge:** `feature/add-recipe` → `feature/edit-recipe-frontend`, dazwischen oder davor
jederzeit `feature/edit-recipe-backend`. Der Edit-Branch im Frontend baut auf dem Add-Branch auf,
weil er dasselbe Formular wiederverwendet. Solange PR 1 offen ist, muss PR 3 als Ziel-Branch
`feature/add-recipe` haben, sonst stehen die Änderungen von PR 1 doppelt im Diff.

---

## Aufgabe 1 — Rezept hinzufügen

**Ausgangslage:** Das Formular unter *Add Recipes* sah fertig aus, war aber eine Attrappe. Die
`Form.Control`-Felder hingen an keinem State, `handleSubmit` von `react-hook-form` wurde geholt und
nie benutzt, und der Submit-Button löste nichts aus. Der `POST /api/recipes` im Backend existierte
bereits — es fehlte also nur die Frontend-Seite.

**Umgesetzt:**

* Alle Felder sind an einen `recipe`-State gebunden, Zutatenzeilen werden über `AddIngredient`
  hinzugefügt, geändert und gelöscht.
* Submit schickt `POST /api/recipes` und navigiert danach zurück auf die Übersicht — dort sieht man
  das neue Rezept, das ist die Bestätigung. Schlägt der Request fehl, erscheint ein Alert.
* Die Zutaten-Felder heissen jetzt wie im Backend (`name`, `unit`, `amount` statt `ingredient`,
  `quantity`), sonst hätte es im Frontend noch ein Mapping gebraucht.
* `react-hook-form` ist raus. Für drei Pflichtfelder reicht das `required`-Attribut des Browsers —
  eine Library weniger für dasselbe Ergebnis.
* Neu: `src/apis/recipeApi.js`. Die Basis-URL stand vorher in `Browse.js` und wäre mit jedem
  weiteren Request erneut kopiert worden.

---

## Aufgabe 2 — Rezept editieren

### Backend (`feature/edit-recipe-backend`)

`PUT /api/recipes/{recipeId}` ist dazugekommen. Der Service lädt das Rezept, überschreibt Name,
Beschreibung und Bild-URL und **ersetzt die Zutatenliste komplett** — `orphanRemoval` räumt die
alten Zeilen weg. Dass die Ids der Zutaten dabei vorher genullt werden, ist kein Detail zum
Überlesen: sonst würden in derselben Transaktion Zeilen eingefügt, deren Primärschlüssel gerade
gelöscht wird.

Eine unbekannte Id gibt jetzt **404** statt einer leeren 200-Antwort (`RecipeNotFoundException`).
`@CrossOrigin` am Controller gilt für alle Methoden, für PUT war also nichts nachzuziehen.

### Frontend (`feature/edit-recipe-frontend`)

Der Button *Edit Details* auf der Rezeptkarte war bisher Dekoration. Er führt jetzt auf
`/recipes/:recipeId/edit`.

Statt ein zweites Formular zu bauen, ist das bestehende in eine eigene Komponente `RecipeForm`
gewandert. `AddRecipe` und `EditRecipe` sind nur noch die Hüllen drumherum — der einzige Unterschied
ist die `onSubmit`-Funktion (`createRecipe` bzw. `updateRecipe`). `EditRecipe` rendert das Formular
erst, wenn die Daten geladen sind, damit es direkt mit den gespeicherten Werten startet.

---

## Ausprobieren

Backend und Frontend laufen getrennt, das Backend muss zuerst da sein:

```bash
cd 8-code-reviews/recipe-planner-fronend-and-backend/recipe-planner-backend
mvn spring-boot:run

cd 8-code-reviews/recipe-planner-fronend-and-backend/recipe-planner-fronend
npm install
npm start
```

Beim Start legt das Backend 15 Beispielrezepte in der H2-Datenbank an. Die liegt im Speicher —
nach einem Neustart sind alle selbst angelegten Rezepte wieder weg.

---

## Die Pull Requests

Beide haben je drei PRs erstellt, alle mit Ziel `main`. Leons Branches haben das Suffix `-leon`,
seine eigene Doku liegt auf seinem Branch als `uebung-code-review-leon.md`.

| Feature | David | Leon |
|---|---|---|
| Rezept anlegen (Frontend) | #1 `feature/add-recipe` | #4 `feature/add-recipe-leon` |
| Rezept ändern (Backend) | #2 `feature/edit-recipe-backend` | #5 `feature/edit-recipe-backend-leon` |
| Rezept ändern (Frontend) | #3 `feature/edit-recipe-frontend` | #6 `feature/edit-recipe-frontend-leon` |

#3 und #6 bauen auf dem jeweiligen Add-Branch auf, zeigen auf `main` und enthalten darum die
Änderungen von #1 bzw. #4 noch einmal. Am Code ändert das nichts, das Review wird aber mühsamer.

---

## Bewusst so entschieden — Punkte fürs Review

Das sind die Stellen, an denen man anderer Meinung sein kann:

| Thema | Entscheidung | Warum diskutabel |
|---|---|---|
| Pfad des neuen Endpoints | `PUT /api/recipes/{id}` | Der GET liegt auf `/api/recipes/recipe/{id}`. Ich bin dem bestehenden Muster bewusst nicht gefolgt, weil es ein Segment zu viel hat. Konsistenz gegen sauberen Pfad. |
| `repository.save()` im `@Transactional` | drin gelassen | Bei einer geladenen Entity schreibt Hibernate ohnehin zurück. Der Aufruf ist technisch überflüssig, macht aber sichtbar, dass hier gespeichert wird. |
| Zutaten ersetzen statt abgleichen | komplett ersetzen | Einfacher Code, aber die Zutaten bekommen bei jedem Speichern neue Ids. Für diese App egal, sobald etwas auf Zutaten verweist, nicht mehr. |
| Einheiten im Frontend | Konstante in `AddIngredient.js` | Doppelt zum `Unit`-Enum im Backend. Alternative wäre ein Endpoint, der die Einheiten liefert — mehr Aufwand als Nutzen bei fünf festen Werten. |
| Validierung | nur `required` und `type="number"` im Browser | Reicht fürs Formular, aber das Backend nimmt weiterhin alles an. |
| Aufräumen im Vorbeigehen | fehlender `key` in `Browse`, `post` → `recipes`, ungenutzte Imports raus | Betrifft nur Dateien, die ich sowieso angefasst habe. Wer es streng sieht, will das trotzdem in einem eigenen PR. |

**Keine Tests geschrieben.** Die Aufgabe verlangt einen Pull Request, keine Tests — aber
„Existieren Tests?" steht in der Review-Checkliste. Das ist damit ein ehrlicher Findpunkt fürs
Review und keine Lücke, die versteckt wird.

Geprüft ist nur, dass es übersetzt — nicht, dass es fachlich stimmt:

| Branch | Prüfung | Ergebnis |
|---|---|---|
| `feature/add-recipe` | `npm run build` | erfolgreich, 4 ESLint-Warnungen |
| `feature/edit-recipe-backend` | `mvn clean compile` | BUILD SUCCESS |
| `feature/edit-recipe-frontend` | `npm run build` | erfolgreich, keine Warnungen |

Die vier Warnungen sind ungenutzte Imports in `App.js` und stammen aus dem gelieferten Projekt.
Auf dem Edit-Branch sind sie weg, weil `App.js` dort für die neue Route ohnehin angefasst wird.

---

## Bewusst nicht gemacht (Follow-ups)

Laut Theorie gehören Vorschläge, die über das Feature hinausgehen, in ein eigenes Ticket:

* `GET /api/recipes/recipe/{id}` gibt bei unbekannter Id weiterhin `200` mit leerem Body.
* `react-hook-form` steht noch in der `package.json`, wird aber nirgends mehr benutzt.
* `Planer.js` ist eine leere Hülle.
* `RecipeForm.css` (vorher `AddRecipe.css`) stylt `body`, `form` und `button` global und wirkt
  dadurch auf die ganze App.

---

## Review-Checkliste

Kurzfassung aus der Theorie — die Punkte, an denen sich beim Durchgehen tatsächlich etwas zeigt:

| | Worauf schauen |
|---|---|
| Funktion | Tut der Code, was der PR-Titel behauptet? Selber durchklicken, nicht nur lesen. |
| Logik | Fehler, Sonderfälle, leere Listen, unbekannte Ids. |
| Naming | Heissen Dinge im Frontend gleich wie im Backend? Sagt der Name, was drin ist? |
| Lesbarkeit | Lange Zeilen, verschachtelte Bedingungen, Kommentare — zu viele oder zu wenige. |
| Tote Reste | Ungenutzte Imports, Variablen, auskommentierter Code. |
| Tests | Existieren sie, decken sie die Fälle ab? |
| YAGNI / DRY / KISS | Nur das Nötige, nichts doppelt, so einfach wie möglich. |
| SRP | Ist jede Komponente für genau eine Sache zuständig? |
| Boy-Scout-Rule | Ist der Code gepflegter als vorher? |
| Typos | Auch in Labels und Meldungen. |

**Zum Verhalten:** konstruktiv formulieren und begründen. Was nur Geschmack ist, als Vorschlag
markieren („ginge auch so…"), nicht als Forderung. Alles, was über das Feature hinausgeht, als
Follow-up notieren statt im PR einfordern.

---

## Erkenntnisse aus dem Review

Beide Lösungen nebeneinander gelegt, Leons Stand selber nachgeprüft:

| PR (Leon) | Prüfung | Ergebnis |
|---|---|---|
| #4 `feature/add-recipe-leon` | `npm run build` | erfolgreich, 6 ESLint-Warnungen (ungenutzte Imports in `App.js` und `Browse.js`) |
| #5 `feature/edit-recipe-backend-leon` | `mvn test` | 2 Tests, 2 grün |
| #6 `feature/edit-recipe-frontend-leon` | `npm run build` | erfolgreich, keine Warnungen |

Vieles ist bei beiden fast Zeile für Zeile gleich — das Feature lässt nicht viele Wege offen. Die
Entscheidungen unten gehen trotzdem jeden Teil durch, auch die gleichen.

### Aufgabe 1 — Rezept anlegen (Frontend, #1 gegen #4)

**Bei beiden gleich:** `react-hook-form` raus, Felder über `useState` gebunden, Zutatenfelder
heissen wie im Backend (`name`, `unit`, `amount`), Einheiten als Konstante `UNITS`, `listId` wird
vor dem Senden entfernt und `amount` in eine Zahl umgewandelt, nach dem Speichern zurück auf `/`,
bei Fehler ein rotes `Alert`, Tippfehler „Quanity" korrigiert.

| Thema | David | Leon | Wir nehmen | Warum |
|---|---|---|---|---|
| Backend-Zugriff | `recipeApi.js` mit `fetchRecipes()` / `createRecipe()` | `api.js` nur mit der URL, `axios` bleibt in den Komponenten | **David** | Die Komponente sagt nur *was* sie will, nicht *wie*. Ändert sich ein Endpoint, ist es eine Stelle. |
| Doppelklick auf Submit | nicht verhindert | Button gesperrt, solange gespeichert wird (`saving`) | **Leon** | Bei langsamem Backend entstehen sonst doppelte Rezepte. Drei Zeilen Code. |
| Menge | `min="0"` | `min="1"`, `step="1"` | **Leon** | 0 Gramm Mehl ist keine Zutat. Das Backend nimmt nur ganze Zahlen. |
| Barrierefreiheit | Zutatenfelder ohne Beschriftung | `aria-label` an jedem Zutatenfeld | **Leon** | Die Felder haben nur einen Platzhalter, ein Screenreader sagt sonst nichts. |
| Feld-Handler | `name`-Attribut + `target.name` | `handleChange('name')` | egal | Beides kurz und lesbar. Geschmack. |
| Fehlermeldung | unter dem Formular, beim Button | oben unter dem Titel | **David** | Man sieht sie dort, wo man gerade geklickt hat. Knapp, eher Geschmack. |
| Aufräumen in `Browse.js` | `post` → `recipes`, `key` ergänzt, `null`-Prüfung durch leere Liste ersetzt | nur die URL ersetzt, ungenutzte Imports bleiben | **David** | Datei wird sowieso angefasst — Boy-Scout-Rule. |
| Tote Reste | leere `propTypes`/`defaultProps` entfernt | stehen gelassen | **David** | Leere Platzhalter sagen nichts aus. |

### Aufgabe 2 — Rezept ändern, Backend (#2 gegen #5)

**Bei beiden gleich:** `PUT /api/recipes/{recipeId}`, `@Transactional`, Rezept laden, Name,
Beschreibung und Bild überschreiben, Zutaten-Ids auf `null` setzen, die bestehende Liste leeren
und neu füllen (wegen `orphanRemoval`), unbekannte Id → 404, ungültige Einheit → 400.

| Thema | David | Leon | Wir nehmen | Warum |
|---|---|---|---|---|
| 404 bei unbekannter Id | `RecipeNotFoundException` mit `@ResponseStatus` | Service gibt `Optional` zurück, Controller macht 404 | **Leon** | Der Service bleibt frei von HTTP. Bei David hängt eine Spring-Web-Annotation im Service-Paket. |
| Zutaten umwandeln | eigener `IngredientEntityMapper` im Service | ganzes Rezept mit `RecipeEntityMapper` umwandeln, Zutaten daraus nehmen | **Leon** | Eine Abhängigkeit weniger, gleiches Ergebnis (KISS). |
| Tests | keine | 2 MockMvc-Tests: Ersetzen inkl. Zutaten, 404 | **Leon** | Ohne Test ist das Verhalten nur behauptet. Das ist der grösste Unterschied. |
| Dokumentation im Code | ein Kommentar zum Ersetzen der Zutaten | Javadoc zum Verhalten + Kommentar | **Leon** | Wer den Service aufruft, sieht sofort, was mit Ids passiert. |
| Aufräumen | ungenutzter Mapper aus dem Controller, Felder `private final` | nichts | **David** | Kleiner, sicherer Gewinn in Dateien, die eh angefasst werden. |
| PR-Beschreibung | kurz, verweist auf sich selber („siehe PR-Beschreibung") | ausführlich: Verhalten, Randfälle, wie getestet | **Leon** | Der Reviewer weiss, was er prüfen muss. |

### Aufgabe 2 — Rezept ändern, Frontend (#3 gegen #6)

**Bei beiden gleich:** Route `/recipes/:recipeId/edit`, „Edit Details" als `Link`, Rezept per
`GET` laden, per `PUT` speichern, danach zurück auf `/`, ungenutzte Imports aus `App.js` entfernt.

| Thema | David | Leon | Wir nehmen | Warum |
|---|---|---|---|---|
| Aufbau | neue `RecipeForm`, `AddRecipe`/`EditRecipe` als dünne Hüllen | `AddRecipe` mit zwei Modi über `recipeId` | **David** | Jede Komponente hat eine Aufgabe (SRP), die Namen stimmen. Bei Leon steht `recipeId ? … : …` an vier Stellen. |
| Grösse des Diffs | ~190 Zeilen | ~40 Zeilen | Leon wäre schneller reviewt | Ehrlicher Zielkonflikt. Wir nehmen trotzdem die Aufteilung, weil sie bei jedem weiteren Feature hilft. |
| Unbekannte Id | **leere Seite** (Bug) | Meldung „Recipe not found", Submit gesperrt | **Leon** | Muss rein. Das Backend antwortet hier mit 200 und leerem Body, das hat nur Leon abgefangen. |
| Laden | Formular erst rendern, wenn das Rezept da ist | Formular sofort, Submit gesperrt bis geladen | **David** | Kein kurzes Aufblitzen eines leeren Formulars. Schützen tun beide gleich gut. |
| Wechsel Edit → Add | eigene Komponenten, kein Problem | State bleibt stehen (Leon nennt das selber) | **David** | Fällt durch die Aufteilung gar nicht erst an. |
| Button-Text | „Save" beim Ändern | „Submit" bei beidem | **David** | Sagt, was passiert. |

### Prozess

| Thema | David | Leon | Wir nehmen |
|---|---|---|---|
| PR-Text | #1 und #2 mit Beschreibung, #3 leer | #5 sehr gut, #4 und #6 leer | Jeder PR braucht ein paar Sätze — was, warum, wie getestet. |
| Ziel-Branch der gestapelten PRs | `main` | `main` | Ziel auf den Add-Branch stellen, sonst steht #1/#4 doppelt im Diff. |
| Commits direkt auf `main` | — | `test`, `test delete` ohne PR | Alles über PRs, auch Kleinigkeiten. |
| Eigene Doku | im Repo auf `main` | als eigene Datei auf dem Feature-Branch | Bleibt so — so sieht man beide Sichten. |

### Fazit — was wir wählen

Keine der beiden Lösungen ist allein die beste. Wir nehmen:

* **Backend: Leons #5** — gleiche Logik wie #2, aber getestet, ohne HTTP im Service und besser
  beschrieben. #2 wird geschlossen. Mein Aufräumen im Controller (ungenutzter Mapper) kommt als
  Nachtrag mit.
* **Frontend: Davids #1 und #3** als Grundlage, wegen `recipeApi.js` und `RecipeForm`. Bevor
  gemerged wird, kommen Leons Punkte dazu: Meldung bei unbekannter Id (Bug), Submit-Sperre beim
  Speichern, `min="1"`, `aria-label`. #4 und #6 werden geschlossen.

Kurz gesagt: **Leon ist an den Rändern sorgfältiger, David hat sauberer aufgeteilt.**

### Was wir gelernt haben

* **Durchklicken findet mehr als Lesen.** Der Bug mit der leeren Seite ist im Code kaum zu sehen —
  er hängt an einem Verhalten des Backends, das gar nicht im Diff steht.
* **Tests machen Fehlerfälle sichtbar.** Den 404-Fall haben beide gebaut, aber nur bei Leon ist er
  durch einen Test belegt — bei mir ist er bloss behauptet.
* **Gleiche Aufgabe, gleicher Kern.** Die eigentliche Logik ist bei beiden fast identisch. Die
  Unterschiede liegen in Fehlerfällen und Struktur — genau dort lohnt sich ein Review.
* **Kleiner Diff gegen saubere Struktur** ist ein echter Zielkonflikt. Beides ist begründbar, man
  muss es nur im PR sagen.
* **Ein PR ohne Text ist schwer zu reviewen.** Bei #5 wusste man sofort, was zu prüfen ist, bei den
  leeren musste man es aus dem Code erraten.
* **Ton:** Kommentare als „Bug / Vorschlag / nit" markieren — dann ist klar, was den Merge blockiert
  und was Geschmack ist.
