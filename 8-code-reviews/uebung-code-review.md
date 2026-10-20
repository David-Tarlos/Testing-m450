# Übung Code-Review — Recipe Planner

David Tarlos und Leon Wulff

Wir haben beide unabhängig voneinander dieselben Features im Recipe Planner aus der Pipeline-Übung
umgesetzt, je einen Pull Request erstellt, uns gegenseitig reviewt und die Lösungen verglichen.

* **Aufgabe 1:** Ein neues Rezept hinzufügen (Frontend)
* **Aufgabe 2:** Ein bestehendes Rezept editieren (Frontend und Backend)

---

## Auf einen Blick

| Teil | Branch David | Branch Leon | Besser | Kurz warum |
|---|---|---|---|---|
| Aufgabe 1 — Frontend | `feature/add-recipe` (#1) | `feature/add-recipe-leon` (#4) | **beide ähnlich** | David sauberer aufgebaut, Leon robuster |
| Aufgabe 2 — Backend | `feature/edit-recipe-backend` (#2) | `feature/edit-recipe-backend-leon` (#5) | **Leon** | gleiche Logik, aber mit Tests |
| Aufgabe 2 — Frontend | `feature/edit-recipe-frontend` (#3) | `feature/edit-recipe-frontend-leon` (#6) | **David**, mit Leons Fehlerbehandlung | sauberer Aufbau, aber David hat einen Bug |

---

## So haben wir reviewt

Wir sind die Checkliste aus der Theorie durchgegangen:

* Funktioniert es? Selber ausprobiert, nicht nur gelesen
* Randfälle: leere Liste, unbekannte Id, Backend nicht erreichbar
* Naming und Lesbarkeit
* Gibt es Tests?
* DRY, KISS, YAGNI, SRP, Boy-Scout-Rule
* Tippfehler

Kommentare haben wir als **Bug**, **Vorschlag** oder **nit** markiert. So ist klar, was geändert
werden muss und was nur Geschmack ist.

---

## Aufgabe 1 — Rezept hinzufügen (Frontend)

Vorher war das Formular nur Optik: Die Felder hingen an keinem State und der Submit-Button tat
nichts.

**Bei beiden gleich:** Felder mit `useState` verbunden, Submit schickt `POST /api/recipes` und geht
zurück zur Übersicht, bei einem Fehler erscheint eine Meldung.

| Was | David | Leon | Besser |
|---|---|---|---|
| Backend-Aufrufe | alle in `recipeApi.js` gesammelt | nur die URL zentral, Aufrufe in den Komponenten | **David** |
| Doppelklick auf Submit | erzeugt zwei Rezepte | Button während dem Speichern gesperrt | **Leon** |
| Menge | 0 erlaubt | mindestens 1 | **Leon** |
| Barrierefreiheit | Zutatenfelder ohne Beschriftung | `aria-label` an jedem Feld | **Leon** |
| Aufräumen in `Browse.js` | `key` ergänzt, besserer Name, leere Platzhalter weg | nur URL ersetzt | **David** |

**Entscheidung:** Wir nehmen **Davids Lösung** als Basis, weil sie übersichtlicher aufgebaut ist,
und übernehmen von Leon die Submit-Sperre, `min="1"` und die `aria-label`s.

---

## Aufgabe 2 — Rezept editieren

### Backend

**Bei beiden gleich:** neuer Endpoint `PUT /api/recipes/{recipeId}`. Er ersetzt Name, Beschreibung,
Bild und die ganze Zutatenliste. Bei einer unbekannten Id kommt `404`.

| Was | David | Leon | Besser |
|---|---|---|---|
| Tests | keine | 2 Tests (Ändern und 404), beide grün | **Leon** |
| 404-Fehler | Exception im Service | Service gibt `Optional` zurück, Controller macht den 404 | **Leon** — der Service hat nichts mit HTTP zu tun |
| PR-Beschreibung | kurz | erklärt Verhalten und Tests genau | **Leon** |
| Aufräumen | ungenutztes Feld im Controller entfernt | — | **David** |

**Entscheidung:** Wir nehmen **Leons Backend**. Die Logik ist bei beiden gleich, aber nur bei Leon
ist sie durch Tests geprüft.

### Frontend

**Bei beiden gleich:** Der Button *Edit Details* führt auf `/recipes/:recipeId/edit`. Dort wird das
Rezept geladen, ins Formular gefüllt und mit `PUT` gespeichert.

| Was | David | Leon | Besser |
|---|---|---|---|
| Aufbau | eigenes `RecipeForm`, das Hinzufügen und Editieren beide benutzen | `AddRecipe` macht beides mit vielen `if`s | **David** — jede Komponente hat eine Aufgabe |
| Unbekannte Id im Link | **leere Seite (Bug)** | Meldung „Recipe not found" | **Leon** |
| Grösse der Änderung | ~190 Zeilen | ~40 Zeilen | **Leon** — schneller zu reviewen |
| Button-Text | „Save" | „Submit" | **David** |

**Entscheidung:** Wir nehmen **Davids Aufbau**, weil er bei weiteren Features hilft. Leons
Fehlermeldung bei unbekannter Id muss aber vorher rein — sonst wird nicht gemerged.

---

## Fazit

Keine Lösung war allein die beste.
**Leon war bei Fehlerfällen und Tests sorgfältiger, David hat den Code sauberer aufgeteilt.**

Wir nehmen:

* **Backend:** Leon (#5)
* **Frontend:** David (#1 und #3), ergänzt mit Leons Fehlerbehandlung

---

## Was wir gelernt haben

1. **Ausprobieren findet mehr als Lesen.** Den Bug mit der leeren Seite sieht man im Code kaum.
2. **Tests zeigen, dass es wirklich funktioniert.** Den 404-Fall haben wir beide eingebaut, geprüft
   hat ihn nur Leon.
3. **Gleiche Aufgabe, ähnlicher Code.** Die Unterschiede lagen vor allem bei Fehlerfällen und beim
   Aufbau — genau dort hat sich das Review gelohnt.
4. **Kleine Änderung oder sauberer Aufbau** ist eine Abwägung. Beides ist okay, man sollte es im PR
   begründen.
5. **Ein PR braucht eine Beschreibung.** Ohne Text muss der Reviewer raten, was gemeint ist.
