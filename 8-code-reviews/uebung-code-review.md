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

## PRs erstellen

```bash
git push -u origin feature/add-recipe
git push -u origin feature/edit-recipe-backend
git push -u origin feature/edit-recipe-frontend
```

GitHub gibt bei jedem Push einen Link zurück, mit dem sich der PR direkt öffnen lässt. Wichtig beim
dritten: als Ziel-Branch `feature/add-recipe` wählen, nicht `main`. Die Commit-Messages taugen als
PR-Beschreibung — und die Punkte aus dem nächsten Abschnitt gehören mit hinein, damit der Reviewer
weiss, wo er hinschauen soll.

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

> *Wird nach dem gegenseitigen Review zusammen ausgefüllt — beide Implementationen nebeneinander
> legen und vergleichen.*

| Thema | Meine Lösung | Lösung des Partners | Was wir behalten |
|---|---|---|---|
| | | | |

**Diskussionspunkte:**

**Was ich beim Reviewen gelernt habe:**
