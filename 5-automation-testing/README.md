# Modul 5 – Automation Testing

Automatisierte Tests für die **Student-App** (Spring Boot + Angular).

| | Übung | Werkzeug | Ergebnis |
|---|---|---|---|
| 1 | REST-Schnittstelle testen | Playwright (`request`) | 14 Tests, grün |
| 2 | GUI im Browser testen | Playwright (Chromium) | 12 Tests, grün |
| 3 | Backend unter Last setzen | k6 | [uebung3-lasttest.md](uebung3-lasttest.md) |
| Bonus | Feature schätzen und umsetzen | Bean Validation + Angular | [bonus-feature.md](bonus-feature.md) |

```
5-automation-testing/
├── spring-boot-angular-basic-lw2/   die Anwendung (Angular unter src/main/js/my-app)
└── automation/
    ├── playwright.config.ts         Projekte api und e2e, startet beide Server selbst
    ├── tests/api/                   Übung 1
    ├── tests/e2e/                   Übung 2
    └── load/students-load.js        Übung 3
```

## Starten

Voraussetzungen: JDK 17 oder 21, Maven, Node 18+, k6 (nur für Übung 3).

```bash
# Backend (8081) und Frontend (4200)
cd spring-boot-angular-basic-lw2 && mvn spring-boot:run
cd spring-boot-angular-basic-lw2/src/main/js/my-app && npm install && npm start

# Tests – starten die Server bei Bedarf selbst
cd automation && npm install && npx playwright install chromium
npm test            # alles
npm run test:api    # nur Übung 1
npm run test:e2e    # nur Übung 2
npm run test:headed # E2E mit sichtbarem Browser
npm run report      # HTML-Report
```

Beim Start legt ein `CommandLineRunner` fünf Studenten in der H2-Datenbank an.
Die läuft im Arbeitsspeicher – nach jedem Neustart ist der Stand wieder frisch.

> **Lombok:** Spring Boot 3.1.2 verwaltet Lombok 1.18.28, das mit JDK 21 nicht
> zurechtkommt. Die `pom.xml` pinnt deshalb `1.18.34` – damit baut das Projekt
> auf JDK 17 und 21.

---

## Übung 1 – REST-Schnittstelle testen

**Playwright statt Postman.** Die `request`-Fixture ist ein reiner HTTP-Client ohne
Browser. So deckt ein Werkzeug beide Übungen ab, und die Tests sind TypeScript
statt generiertem JSON – im Pull Request lesbar.

| Gruppe | Tests | Inhalt |
|---|---|---|
| `GET /students` | 3 | Status, Content-Type, Seed-Daten, Schema |
| `POST /students` | 4 | Anlegen, id-Vergabe, unbekannte Felder, 400 und 415 |
| CORS und Routen | 2 | nur Port 4200 erlaubt; sonst 404 |
| Eingabevalidierung | 5 | Pflichtfelder, E-Mail-Format, Länge |

Die Tests legen echte Datensätze an, deshalb nie auf eine exakte Anzahl prüfen –
überall „enthält" statt „ist gleich", und eindeutige Namen pro Lauf.

**Befunde:** `POST` antwortet mit 200 und leerem Body statt 201 Created. Es gibt
kein `GET /students/{id}`, kein `PUT`, kein `DELETE`. Und ursprünglich nahm der
Endpunkt jede Eingabe an – diese Lücke schliesst das Bonus-Feature.

---

## Übung 2 – GUI im Browser testen

**Playwright, Chromium, headless.** Gegenüber Selenium ausschlaggebend: Playwright
wartet von sich aus, bis ein Element bedienbar ist – die `sleep()`-Aufrufe
entfallen. Nichts wird gemockt, der Browser redet mit dem echten Angular, das mit
dem echten Backend. Ein Test fällt also auch um, wenn CORS falsch steht.

| Gruppe | Tests | Inhalt |
|---|---|---|
| Navigation | 2 | Startseite, Routing |
| Studentenliste | 3 | Backend-Daten, Spalten, mailto-Link |
| Studenten erfassen | 3 | Submit-Sperre, Durchstich → Liste → API, Reload |
| Formularvalidierung | 4 | Meldungen, E-Mail-Format, Backend-Fehler |

**Befund:** Die Fehlermeldungen hingen an `pristine` („noch nie angefasst") statt
an `invalid` – sie standen also auf dem leeren Formular und verschwanden, sobald
man tippte. Zuerst als Ist-Zustand festgehalten, dann vom Bonus-Feature korrigiert.

---

## Übung 3 und Bonus

**[uebung3-lasttest.md](uebung3-lasttest.md)** – Lasttest mit k6: drei Szenarien,
Thresholds als Pass/Fail. Zwei Befunde: der Stresslauf hat den eigenen Testaufbau
gemessen statt den Server, und `GET /students` liefert immer die komplette Tabelle.

**[bonus-feature.md](bonus-feature.md)** – Eingabevalidierung: Spezifikation und
Schätzung vor der ersten Codezeile, danach Umsetzung und Reflexion. Backend mit
Bean Validation und `@RestControllerAdvice`, Frontend mit korrigierter
Meldungslogik und Anzeige der Server-Fehler.

---

```
26 passed (5.7s)     14 API-Tests, 12 E2E-Tests
```
