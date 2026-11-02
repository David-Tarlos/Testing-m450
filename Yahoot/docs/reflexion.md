# Reflexion

> **Entwurf.** Die Fakten unten sind aus Git, den Pull Requests und den Testläufen belegt. Die Abschnitte
> «Unsere Sicht» müsst ihr selbst schreiben, weil es um eure eigene Erfahrung geht.

## TDD

Die Spielregeln und die Backend-Logik sind nach Red/Green entstanden, im Frontend die reine Logik und die
Komponenten. Das hat konkret etwas gebracht:

- Beim Schreiben der Kartentests zeigte das Rot, dass der Test mit zwei Dieben erst mit Comeback-Energie
  möglich ist. Dadurch wurde die Reihenfolge der Regeln klar.
- Eine Antwort vor dem Spielstart führte zum Absturz. Gefunden beim Durchlesen, zuerst als roter Test
  festgehalten, dann behoben.
- Der Fehler hinter einem HTTPS-Proxy war zuerst nur eine Vermutung. Der rote Test hat ihn belegt, bevor wir
  etwas geändert haben.

Was nicht so sauber war:

- Die Commits wurden nachträglich zusammengefasst. Die meisten roten Commits scheitern darum am Kompilieren,
  nicht an einer einzelnen Zusicherung.
- Die Tests der Karten entstanden zuerst gemeinsam und wurden danach schrittweise eingeführt.
- Die Playwright-Tests sind grösstenteils erst nach der Oberfläche entstanden.
- Grüne Tests allein sagen wenig. Erst die Gegenprobe mit absichtlich eingebauten Fehlern zeigte zwei Lücken,
  siehe [Testkonzept](testkonzept.md).

**Unsere Sicht:** _(noch zu schreiben: Wo hat TDD geholfen, wo hat es gebremst? Würdet ihr es wieder so machen?)_

## Code Reviews

- PR #25 hat 8 Kommentare, unter anderem Fragen zu `docker/setup-buildx-action` und dazu, ob ein Test sinnvoll
  ist. Eine Frage dazu wurde auf ein Gespräch nach dem Unterricht verschoben.
- Die PRs #9 bis #13 wurden innert etwa zwei Minuten ohne einen Kommentar gemergt. Das hatte Folgen:
  - Die PRs #10 bis #12 gingen in gestapelte Branches statt nach `main`. Ein grosser Teil der Arbeit fehlte
    darum in `main`, ohne dass es jemand bemerkte.
  - PR #13 hat den Deploy-Job auf `main` gebrochen, weil für den Docker-Cache `setup-buildx-action` fehlte.
    Die Beschreibung sagte selbst, dass sich erst im ersten Lauf zeigt, ob alles greift.

Beides wäre bei einem Blick auf Ziel-Branch und Pipeline vor dem Merge aufgefallen.

**Unsere Sicht:** _(noch zu schreiben: Was habt ihr im Review voneinander gelernt? Was macht ihr nächstes Mal anders?)_

## KI-Einsatz

Ein KI-Agent hat nach unseren Vorgaben einen grossen Teil von Code, Tests und Doku erstellt.

**Unsere Sicht:** _(noch zu schreiben: Was hat das gebracht, wo musstet ihr eingreifen, welche Teile könnt ihr
erklären?)_

## Scope und Zeit

**Unsere Sicht:** _(noch zu schreiben: Wie viele Stunden habt ihr wirklich investiert, und passte der Umfang?)_
