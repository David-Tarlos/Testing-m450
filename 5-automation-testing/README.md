# Modul 5 – Automation Testing

Automatisierte Tests für die **Student-App** (Spring Boot + Angular): REST-Tests,
End-to-End-Tests im Browser und ein Lasttest.

| | Übung | Werkzeug | Ergebnis |
|---|---|---|---|
| 1 | REST-Schnittstelle automatisiert testen | Playwright (`request`) | 11 Tests, grün |
| 2 | GUI im Browser automatisiert testen | Playwright (Chromium) | 9 Tests, grün |
| 3 | Backend unter Last setzen | k6 | [uebung3-lasttest.md](uebung3-lasttest.md) |
| Bonus | Feature schätzen und umsetzen | Bean Validation + Angular | [bonus-feature.md](bonus-feature.md) |

```
5-automation-testing/
├── spring-boot-angular-basic-lw2/   die Anwendung (Backend + Angular unter src/main/js/my-app)
├── automation/
│   ├── playwright.config.ts         zwei Projekte: api und e2e, startet beide Server selbst
│   ├── tests/api/                   Übung 1
│   ├── tests/e2e/                   Übung 2
│   └── load/students-load.js        Übung 3
├── uebung3-lasttest.md              Lasttest: Werkzeug, Messungen, Befunde
└── bonus-feature.md                 Feature-Spezifikation, Schätzung, Reflexion
```

---

## Inbetriebnahme

**Voraussetzungen:** JDK 17 oder 21, Maven, Node 18+, k6 (nur für Übung 3).

> **Hinweis – Lombok.** Spring Boot 3.1.2 verwaltet Lombok 1.18.28, und das kennt
> die javac-Interna von JDK 21 nicht: `NoSuchFieldError: JCTree$JCImport ... qualid`.
> Die `pom.xml` pinnt deshalb `<lombok.version>1.18.34</lombok.version>` – damit baut
> das Projekt sowohl auf JDK 17 als auch auf 21, ohne dass man umschalten muss.

```bash
# Backend (Port 8081)
cd spring-boot-angular-basic-lw2
mvn spring-boot:run

# Frontend (Port 4200)
cd spring-boot-angular-basic-lw2/src/main/js/my-app
npm install && npm start
```

Beim Start legt ein `CommandLineRunner` fünf Studenten in der H2-In-Memory-Datenbank
an (Jonas, Patrick, Yves, Peter, Ann). Nach jedem Neustart ist der Stand wieder frisch.

**Die Tests starten die Server bei Bedarf selbst.** In `playwright.config.ts` sind
beide als `webServer` hinterlegt – läuft schon etwas auf dem Port, wird es
weiterverwendet. `npm test` genügt also auch bei kaltem Start.

```bash
cd automation
npm install
npx playwright install chromium

npm test            # alles
npm run test:api    # nur Übung 1
npm run test:e2e    # nur Übung 2
npm run test:headed # E2E mit sichtbarem Browser
npm run report      # HTML-Report öffnen
```

---

## Übung 1 – REST-Schnittstelle automatisiert testen

**Werkzeug: Playwright.** Naheliegend wären Postman/Newman oder REST Assured
gewesen. Playwright kann beides: die `request`-Fixture ist ein reiner HTTP-Client
ohne Browser. Damit deckt **ein** Werkzeug Übung 1 und 2 ab, es gibt einen
gemeinsamen HTML-Report, und die Tests sind normales TypeScript statt
generiertem JSON – im Pull Request lesbar.

Ehrlicher Nachteil: Postman ist zum Erkunden einer API bequemer, weil man
Requests interaktiv zusammenklickt.

### Was getestet wird (11 Tests)

| Gruppe | Tests | Inhalt |
|---|---|---|
| `GET /students` | 2 | Status, Content-Type, Schema/Contract, Seed-Daten |
| `POST /students` | 3 | Anlegen, id-Vergabe, unbekannte Felder, kaputtes JSON (400), falscher Content-Type (415) |
| CORS und Routen | 2 | Port 4200 erlaubt / fremde Origin nicht; nicht implementierte Routen → 404 |
| Eingabevalidierung | 4 | Pflichtfelder, E-Mail-Format, Länge, nichts gespeichert bei Ablehnung (Bonus-Feature) |

Zwei Dinge, die beim Schreiben wichtig waren:

**Nie auf eine exakte Anzahl prüfen.** Die Tests legen echte Datensätze an. Ein
`toHaveLength(5)` wäre beim zweiten Lauf rot. Deshalb überall „enthält" statt
„ist gleich", und eindeutige Namen pro Lauf (`Ada-1756738-x7k2q`).

**Das Schema mitprüfen.** `expect(Object.keys(student).sort()).toEqual(['email','id','name'])`
schlägt an, sobald jemand ein Feld ergänzt oder umbenennt – der Test schützt den
Vertrag, nicht nur den Statuscode.

### Befunde

* `POST /students` antwortet mit **200 und leerem Body**. REST-üblich wäre 201
  Created mit `Location`-Header und dem angelegten Objekt.
* Es gibt **kein** `GET /students/{id}`, kein `PUT`, kein `DELETE` – die API kann
  nur anlegen und alles lesen.
* Unbekannte Felder werden stillschweigend verworfen statt abgelehnt.
* Ursprünglich nahm der Endpunkt **jede** Eingabe an (leere Namen, ungültige
  E-Mails, sogar `{}`). Diese Lücke schliesst das Bonus-Feature.

---

## Übung 2 – Angular-GUI im Browser testen

**Werkzeug: Playwright**, Chromium, headless (`--headed` zeigt den Browser).
Gegenüber Cypress und Selenium ausschlaggebend: das automatische Warten. Playwright
wartet von sich aus, bis ein Element sichtbar und bedienbar ist – die
`sleep()`-Aufrufe, die Selenium-Tests unzuverlässig machen, entfallen komplett.

Es wird **nichts gemockt**: der Browser redet mit dem echten Angular-Dev-Server
auf 4200, der mit dem echten Backend auf 8081. Ein Test fällt also auch dann um,
wenn CORS falsch steht oder das Backend tot ist – genau das ist bei E2E gewollt.

### Was getestet wird (9 Tests)

| Gruppe | Tests | Inhalt |
|---|---|---|
| Navigation | 1 | Startseite, Routing auf `/students` und `/addstudents` |
| Studentenliste | 2 | Backend-Daten und Spalten im GUI, mailto-Link |
| Studenten erfassen | 3 | Submit-Sperre, Durchstich Formular → Liste → API, Persistenz nach Reload |
| Formularvalidierung | 3 | Meldungslogik, E-Mail-Format, Backend-Fehleranzeige (Bonus-Feature) |

Der eigentliche End-to-End-Durchstich:

```ts
await page.goto('/addstudents');
await page.locator('#name').fill(name);
await page.locator('#email').fill(email);
await page.getByRole('button', { name: 'Submit' }).click();

await expect(page).toHaveURL(/\/students$/);
await expect(page.getByRole('cell', { name, exact: true })).toBeVisible();
```

GUI → HTTP → Spring → H2 → GUI, in einem Test. Ein zweiter Test holt denselben
Datensatz danach noch über die API ab – damit ist bewiesen, dass wirklich
gespeichert wurde und nicht nur die Anzeige stimmt.

Selektoren gehen wo möglich über die Rolle (`getByRole('button', { name: 'Submit' })`)
statt über CSS-Klassen. Das überlebt ein Umgestalten des Layouts.

### Befund

Beim Schreiben der Tests fiel ein echter Bug im Formular auf: die
Fehlermeldungen hingen an `pristine` statt an `invalid`.

```html
<div [hidden]="!name.pristine" class="alert alert-danger">Name is required</div>
```

`pristine` heisst „noch nie angefasst". Die Meldung stand also auf dem
unberührten Formular und verschwand, sobald man tippte – auch wenn man das Feld
danach wieder leerte. Genau dann bräuchte man sie. Der Nutzer sah einen
gesperrten Submit-Button ohne jede Begründung.

Zuerst als Ist-Zustand festgehalten, dann vom [Bonus-Feature](bonus-feature.md)
korrigiert.

---

## Übung 3 – Lasttest

Vollständig in **[uebung3-lasttest.md](uebung3-lasttest.md)**. Kurzfassung:

k6 statt JMeter, weil ein JS-Skript im Git reviewbar ist und eine `.jmx`-Datei
nicht. Drei Szenarien (smoke / load / stress) mit unterschiedlichen Executors,
Thresholds als CI-taugliches Pass/Fail, eigene Metriken für Lesen und Schreiben.

Bei Normallast (20 Nutzer, 18 Anfragen/s) ist die App unauffällig: p95 unter 5 ms,
keine Fehler. Zwei Befunde waren trotzdem wertvoll:

* Der Stresslauf hat **nicht den Server gemessen**, sondern den Lastgenerator –
  1 917 verworfene Iterationen bei erschöpftem VU-Pool. Ohne Blick auf
  `dropped_iterations` hätte man eine Serverbremse diagnostiziert, die es nicht gibt.
* `GET /students` liefert immer die komplette Tabelle. Bei gleicher Last stieg die
  Lesezeit von 5,5 ms (25 Datensätze, 1,4 KB) auf 14,0 ms (2 533 Datensätze,
  138 KB) – linear wachsend, ohne Paginierung.

---

## Bonus – Feature schätzen und umsetzen

Vollständig in **[bonus-feature.md](bonus-feature.md)**: Spezifikation und
Schätzung (45 min) wurden vor der ersten Codezeile geschrieben, danach Umsetzung,
Ist-Zeiten und Reflexion.

Umgesetzt wurde die **Eingabevalidierung** – die Lücke, über die sowohl die
API-Tests als auch der Lasttest gestolpert waren. Backend: Bean Validation plus
ein `@RestControllerAdvice`, das 400 mit feldgenauen Meldungen liefert. Frontend:
Meldungslogik korrigiert, E-Mail-Format geprüft, Backend-Fehler angezeigt.

Die lehrreichste Stelle: Nachdem alles grün war, stellte sich heraus, dass die
Server-Fehleranzeige gar nicht auslösen konnte – das `maxlength="100"` im Formular
machte den einzigen rein serverseitigen Fehler unerreichbar. Implementiert,
getestet, und trotzdem toter Code. Aufgefallen ist es erst beim Versuch, einen
Test dafür zu schreiben.

---

## Gesamtstand

```
20 passed (5.5s)
```

11 API-Tests, 9 E2E-Tests, drei Lastszenarien mit allen Thresholds eingehalten.
