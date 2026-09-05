# Bonus – Feature „Eingabevalidierung", Schätzung und Reflexion

> Die Spezifikation und die Schätzung unten wurden **vor** der ersten Codezeile
> geschrieben. Die Ist-Zeiten und die Reflexion sind danach ergänzt worden.

---

## Warum dieses Feature

Sowohl die API-Tests aus Übung 1 als auch der Lasttest aus Übung 3 sind über
dieselbe Lücke gestolpert: `POST /students` nimmt **alles** an.

| Eingabe | Antwort heute | Ergebnis in der Datenbank |
|---|---|---|
| `{"name":"","email":""}` | 200 OK | leerer Datensatz |
| `{"name":"X"}` | 200 OK | `email = ""` |
| `{}` | 200 OK | zwei leere Felder |
| `{"name":"Z","email":"keine-email"}` | 200 OK | ungültige Adresse |

Im Frontend sieht es ähnlich aus: die Fehlermeldungen hängen an `pristine`
(„noch nie angefasst") statt an `invalid`.

```html
<div [hidden]="!name.pristine" class="alert alert-danger">Name is required</div>
```

Die Meldung steht also auf dem unberührten Formular und verschwindet, sobald man
tippt – auch wenn man das Feld danach wieder leert. Genau dann bräuchte man sie.
Der Nutzer sieht einen gesperrten Submit-Button ohne Begründung.

---

## Spezifikation

### Backend

**B1 – Abhängigkeit.** `spring-boot-starter-validation` in die `pom.xml`.

**B2 – Regeln an der Entity.** `Student`:

| Feld | Regel | Meldung |
|---|---|---|
| `name` | `@NotBlank` | „Name darf nicht leer sein" |
| `name` | `@Size(max = 100)` | „Name darf höchstens 100 Zeichen lang sein" |
| `email` | `@NotBlank` | „E-Mail darf nicht leer sein" |
| `email` | `@Email` | „E-Mail ist keine gültige Adresse" |

**B3 – Controller.** `addStudent(@Valid @RequestBody Student)`.

**B4 – Fehlerantwort.** Ein `@RestControllerAdvice` fängt
`MethodArgumentNotValidException` und antwortet mit **400** und diesem Body:

```json
{
  "status": 400,
  "error": "Validierungsfehler",
  "fields": {
    "name": "Name darf nicht leer sein",
    "email": "E-Mail ist keine gültige Adresse"
  }
}
```

Ein maschinenlesbares `fields`-Objekt, damit das Frontend die Meldungen direkt
am richtigen Feld anzeigen kann.

### Frontend

**F1 – Meldungslogik korrigieren.** Fehlermeldung anzeigen, wenn das Feld
`invalid` **und** (`dirty` oder `touched`) ist. Auf dem frisch geladenen Formular
also keine Meldung.

**F2 – E-Mail-Format prüfen.** `type="email"` plus Angulars `email`-Validator, mit
eigener Meldung für „Format falsch" im Unterschied zu „leer".

**F3 – Backend-Fehler anzeigen.** `onSubmit()` behandelt den Fehlerfall und zeigt
die Meldungen aus `fields` über dem Formular an. Kein stiller Fehlschlag mehr.

### Tests

**T1** – die drei API-Tests der Gruppe „Validierungslücken" auf **400** umstellen
und die Feldmeldungen prüfen.
**T2** – die zwei E2E-Tests „Bekannter Fehler" auf das korrigierte Verhalten
umstellen.
**T3** – neue Tests: gültige Eingabe geht weiterhin durch; ungültiges
E-Mail-Format sperrt den Button; Fehlermeldung erscheint nach dem Leeren eines
Feldes.

### Ausdrücklich **nicht** im Scope

Damit die Schätzung ehrlich bleibt: kein `201 Created` statt `200`, keine
Paginierung, keine Duplikatsprüfung auf E-Mail, keine Übersetzung der Meldungen,
kein Umbau auf Reactive Forms.

---

## Schätzung (vor der Umsetzung)

| Teil | Aufwand |
|---|---|
| B1–B4 Backend (Dependency, Annotationen, `@Valid`, Advice) | 15 min |
| F1–F3 Frontend (Meldungslogik, E-Mail-Validator, Fehleranzeige) | 15 min |
| T1–T3 Tests anpassen und ergänzen | 10 min |
| Durchlauf, Verifikation, Doku nachziehen | 5 min |
| **Total** | **45 min – eine Lektion** |

**Angenommene Risiken:** Das Backend braucht einen Neustart, damit die Validierung
greift (kein DevTools-Hot-Reload konfiguriert). Beim Angular-Formular könnte die
Umstellung von `[hidden]` auf `*ngIf` die bestehenden Selektoren der E2E-Tests
brechen.

---

## Umsetzung

### Geänderte Dateien

| Datei | Änderung |
|---|---|
| `pom.xml` | `spring-boot-starter-validation` ergänzt |
| `repository/entities/Student.java` | `@NotBlank`, `@Size`, `@Email` an den Feldern |
| `controller/StudentController.java` | `@Valid` am `@RequestBody` |
| `controller/ValidationExceptionHandler.java` | **neu** – `@RestControllerAdvice`, 400 + `fields` |
| `student-form.component.html` | Meldungslogik korrigiert, `type="email"`, Server-Fehlerblock |
| `student-form.component.ts` | `serverFehler`, Error-Handler im `subscribe` |
| `automation/tests/api/students.api.spec.ts` | 3 Lücken-Tests → 7 Validierungstests |
| `automation/tests/e2e/students.e2e.spec.ts` | 2 Ist-Zustand-Tests → 5 Validierungstests |

### Verhalten vorher / nachher

| Eingabe | vorher | nachher |
|---|---|---|
| `{"name":"","email":""}` | 200, leerer Datensatz | 400, beide Felder gemeldet |
| `{}` | 200, leerer Datensatz | 400, beide Felder gemeldet |
| `{"name":"X"}` | 200, `email = ""` | 400, nur `email` gemeldet |
| `{"name":"Z","email":"keine-email"}` | 200 | 400, `email` gemeldet |
| Name mit 101 Zeichen | 200 | 400, Längenmeldung |
| gültige Eingabe | 200 | 200 (unverändert) |

Beispielantwort:

```json
{
  "status": 400,
  "error": "Validierungsfehler",
  "fields": { "email": "E-Mail ist keine gueltige Adresse" }
}
```

**Testabdeckung:** 20 Tests grün (11 API, 9 E2E), davon 7 für dieses Feature.

---

## Zeitaufwand

| Teil | Geschätzt | Tatsächlich |
|---|---|---|
| Spezifikation schreiben | – (nicht geschätzt) | 0:31 min |
| B1–B4 Backend | 15 min | ~1 min |
| F1–F3 Frontend | 15 min | ~1 min |
| T1–T3 Tests | 10 min | ~1 min |
| Nacharbeit `maxlength` (ungeplant) | 0 min | ~0:45 min |
| **Total Implementation** | **45 min** | **3:47 min** |

> **Wichtige Einordnung:** Diese Ist-Zeiten sind für die Übung wenig aussagekräftig,
> weil hier eine KI getippt hat und nicht ein Mensch. Die Schätzung von 45 Minuten
> war für Handarbeit gedacht. Interessant ist deshalb nicht die Uhrzeit, sondern
> die Frage: **hat die Schätzung die richtigen Arbeitsschritte erfasst?**
> Darauf zielt die Reflexion unten.

---

## Reflexion

### Was die Schätzung richtig hatte

**Die Aufteilung stimmte.** Backend, Frontend und Tests haben ungefähr im
geschätzten Verhältnis 15/15/10 Aufwand verursacht. Wer schätzt, ohne die Arbeit
in Teile zu zerlegen, rät – die Zerlegung war das Nützlichste an der ganzen
Schätzung.

**Das vorhergesagte Risiko trat ein.** In der Spezifikation stand, die Umstellung
von `[hidden]` auf `*ngIf` könnte die E2E-Selektoren brechen. Genau das passierte:
beide Tests der Gruppe „Bekannter Fehler" mussten umgeschrieben werden. Weil es
eingeplant war, war es keine Überraschung, sondern nur Arbeit.

**Der Scope-Ausschluss hat gehalten.** Die Liste „ausdrücklich nicht im Scope"
(kein 201 Created, keine Paginierung, keine Duplikatsprüfung) war beim
Implementieren mehrfach in Versuchung – vor allem `201 Created` hätte gut
gepasst. Die Liste hat genau das verhindert, wofür sie da war.

### Was die Schätzung nicht hatte

**Das Feature war zuerst teilweise wirkungslos.** Nachdem alles fertig und grün
war, fiel auf: die Server-Fehleranzeige (F3) konnte gar nicht mehr auslösen. Weil
das Formular `maxlength="100"` hatte, war der einzige Fehler, den nur das Backend
erkennt, im Browser gar nicht mehr eintippbar. F3 war implementiert, getestet – und
toter Code.

Das ist der interessanteste Punkt des ganzen Bonus. Die Lösung war eine
Design-Entscheidung, keine Zeile Mehrarbeit im ursprünglichen Sinn: `maxlength`
aus dem HTML entfernen und die Längenregel bewusst dem Backend überlassen. Dann
hat die Fehleranzeige einen echten Zweck, und ein E2E-Test kann den ganzen Weg
GUI → 400 → Anzeige durchlaufen.

**Diese Art Aufwand steht in keiner Schätzung.** Sie entsteht nicht aus dem
Schreiben von Code, sondern aus dem Nachdenken darüber, ob der geschriebene Code
überhaupt etwas tut. Aufgefallen ist es erst beim Versuch, einen Test dafür zu
schreiben – ein gutes Argument dafür, Tests nicht ans Ende zu schieben.

**Der Testumfang ist gewachsen.** Geplant war „3 API-Tests umstellen, 2 E2E-Tests
umstellen". Geworden sind es zuerst 7 API- und 5 E2E-Tests, weil beim Schreiben laufend
weitere sinnvolle Fälle auffielen (zu langer Name, „speichert nichts bei
Ablehnung", „Meldung verschwindet wieder"). Das ist die typische Unterschätzung
bei Tests: man plant, die bestehenden anzupassen, und schreibt am Ende doppelt so
viele.

### Was ich beim nächsten Mal anders schätzen würde

1. **Einen Posten „ist es überhaupt wirksam?" einplanen.** Nicht nur „bauen" und
   „testen", sondern explizit prüfen, ob jeder Teil des Features im echten
   Zusammenspiel auslösen kann. Das hätte den `maxlength`-Fund vorweggenommen.
2. **Tests grosszügiger schätzen.** Faustregel für das nächste Mal: geplante
   Testanzahl mal zwei. Beim Schreiben fallen einem immer Fälle ein, die man beim
   Planen nicht auf dem Schirm hatte.
3. **Client- und Server-Validierung zusammen entwerfen.** Die Frage „welche Regel
   gehört wohin?" separat zu entscheiden hat den Doppelaufwand erzeugt. Eine
   Tabelle „Regel → Client / Server / beide" in der Spezifikation hätte gereicht.

### Fazit

Die Schätzung war als Zeitangabe nicht überprüfbar, als **Arbeitsplan** aber
brauchbar: die Zerlegung stimmte, das benannte Risiko trat ein, der
Scope-Ausschluss hielt. Danebengelegen hat sie dort, wo Schätzungen meistens
danebenliegen – nicht beim Schreiben des Codes, sondern bei der Nacharbeit, die
erst sichtbar wird, wenn man das Ergebnis ernsthaft zu testen versucht.
