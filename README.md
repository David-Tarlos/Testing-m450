# Testing-m450 — Testen von Software

Modul 450 an der TBZ. Dieses Repository enthält zu jedem Kapitel die Theorie, die gelösten
Aufgaben und die **tatsächlich gemessenen** Ergebnisse.

> **Grundsatz:** Jede Zahl in diesem Repository stammt aus einem ausgeführten Lauf. Wo etwas
> nicht ausgeführt werden konnte, steht das als offener Punkt — nicht als geschätzter Wert.

---

## Gesamtstand

| Kapitel | Thema | Status | Ergebnis |
|---|---|---|---|
| [1](1-grundlagen/) | Grundlagen | ✅ | 3 Aufgaben, Testtreiber findet 2 Fehler |
| [2](2-Teststrategie/) | Teststrategie | ✅ | 3 Übungen, 52 Testfälle, 16 Befunde |
| [3](3-Testlevels/) | Testlevels | ✅ | 120 Tests, 100 % Coverage |
| [4](4-abhaengigkeiten-zu-schnittstellen/) | Abhängigkeiten zu Schnittstellen | ✅ | 50 Tests, Comparator korrigiert |
| [5](5-automation-testing/) | Automation Testing | ✅ | 26 Tests, Lasttest, Bonus-Feature |
| [6](6-testkonzept-vertieft/) | Testkonzept | ✅ | Testkonzept nach IEEE 829 |
| [7](7-cicd-pipelines/) | CI/CD + Deployment Environments | ✅ | 42 Tests, 95,1 % Coverage, Pipeline grün |
| [8](8-code-reviews/) | Code Reviews | ⬜ | offen |
| [9](9-test-driven-development/) | Test Driven Development | ⬜ | offen |
| Modulprojekt | eigenes Projekt | ⬜ | offen |

**Tests im Repository gesamt: 238**, alle grün.

---

## Kapitel 1 — Grundlagen

📁 [`1-grundlagen/`](1-grundlagen/) · 📄 [Lösung](1-grundlagen/README.md)

| Aufgabe | Inhalt |
|---|---|
| 1 | Formen von Tests |
| 2 | SW-Fehler und SW-Mangel — mit dem Knight-Capital-Beispiel |
| 3 | Testtreiber für eine vorgegebene Preisberechnung schreiben |

**Ergebnis:** Der Testtreiber hat **zwei echte Fehler** in `calculatePrice()` gefunden:

1. **Der 15 %-Rabatt wird nie gewährt** — `if (extras >= 3)` steht vor `else if (extras >= 5)`,
   der zweite Zweig ist unerreichbar.
2. Ein zweiter Fehler in der Rabattlogik, dokumentiert in der Lösung.

Code: [`aufgabe3/Preisberechnung.java`](1-grundlagen/aufgabe3/Preisberechnung.java) (Vorgabe,
unverändert) · [`aufgabe3/TestTreiber.java`](1-grundlagen/aufgabe3/TestTreiber.java)

---

## Kapitel 2 — Teststrategie

📁 [`2-Teststrategie/`](2-Teststrategie/) · 📄 [Theorie](2-Teststrategie/README.md)

| Übung | Thema | Lösung | Ergebnis |
|---|---|---|---|
| 1 | Rabattregeln — abstrakte und konkrete Testfälle | [uebung-1-rabattregeln.md](2-Teststrategie/uebung-1-rabattregeln.md) | **5** abstrakte (A1–A5), **18** konkrete (K1–K18) Testfälle |
| 2 | Autovermietung — funktionale Black-Box-Testfälle | [uebung-2-autovermietung-blackbox.md](2-Teststrategie/uebung-2-autovermietung-blackbox.md) | **5** Testfälle (TF-01–TF-05) |
| 3 | Bank-Software — Black-Box, White-Box, Code-Review | [uebung-3-bank-software.md](2-Teststrategie/uebung-3-bank-software.md) | **24** Testfälle (BB-01–BB-24), **8** Fehler (F-01–F-08), **8** Empfehlungen (R-01–R-08) |

**Methoden:** Äquivalenzklassenbildung, Grenzwertanalyse, funktional/nicht-funktional,
abstrakt/konkret, Black-Box vs. White-Box.

> **Offener Punkt: BB-23** — *„Wechselkurs abfragen ohne Internetverbindung"* ist als `offen`
> markiert. Der Testfall erfordert manuelles Abschalten der Netzwerkverbindung und wurde nicht
> ausgeführt. Bewusst so dokumentiert statt als bestanden eingetragen.

---

## Kapitel 3 — Testlevels

📁 [`3-Testlevels/`](3-Testlevels/) · 📄 [Übersicht Unit-Testing](3-Testlevels/unit-testing/README.md)

| Aufgabe | Inhalt | Lösung |
|---|---|---|
| 1 | Wie wird bei uns getestet? | [testing-m450 (1).md](<3-Testlevels/testing-m450 (1).md>) |
| 2 | JUnit-5-Zusammenfassung | [aufgabe2-junit-zusammenfassung.md](3-Testlevels/aufgabe2-junit-zusammenfassung.md) |
| 3–4 | Unit-Tests für Calculator und Banken-Simulation | [unit-testing/](3-Testlevels/unit-testing/) |

**Ergebnis:**

| Projekt | Tests | Line Coverage | Branch Coverage |
|---|---|---|---|
| `aufgabe1-calculator` | **28** grün | 100 % | 100 % |
| `aufgabe3-4-bank-simulation` | **92** grün | 100 % | 100 % |

Beide Projekte haben ein JaCoCo-**Coverage-Gate** (`check` an `verify` gebunden): der Build
schlägt fehl, wenn die Abdeckung unter 80 % Lines bzw. 75 % Branches fällt.

---

## Kapitel 4 — Abhängigkeiten zu Schnittstellen

📁 [`4-abhaengigkeiten-zu-schnittstellen/`](4-abhaengigkeiten-zu-schnittstellen/) ·
📄 [Theorie](4-abhaengigkeiten-zu-schnittstellen/README.md) ·
📄 [Lösung](4-abhaengigkeiten-zu-schnittstellen/uebung-addressbook.md)

**Testobjekt:** `addressbook-backend` (Spring Boot 3.5.4, Vorgabe)

| Aufgabe | Inhalt |
|---|---|
| 1 | Tests für alle Klassen · den Service testen, indem die H2-Datenbank **weggemockt** wird · den Comparator korrekt implementieren |
| 2 | Comparator erweitern, sodass nach zusätzlichen Attributen sortiert werden kann |

**Ergebnis: `Tests run: 50, Failures: 0, Errors: 0`**

| Testklasse | Tests | Technik |
|---|---|---|
| `AddressServiceTest` | 11 | **Mockito** — `@Mock` + `@InjectMocks`, H2 komplett weggemockt |
| `AddressComparatorTest` | 21 | reines JUnit, 4 `@Nested`-Gruppen |
| `AddressRepositoryTest` | 7 | `@DataJpaTest` gegen echte H2 — bewusst **nicht** gemockt |
| `AddressControllerTest` | 6 | `@WebMvcTest` + MockMvc |
| `AddressTest` | 4 | Entity und Lombok |
| `AddressbookApplicationTests` | 1 | Smoke-Test |

**Der Befund zum Comparator:** Die Vorgabe lieferte konstant `return -1;`. Das verletzt den
`Comparator`-Vertrag zweifach (Reflexivität und Antisymmetrie). Empirisch geprüft mit 2, 5, 31, 32,
33 und 100 Elementen: **es fliegt keine Exception** — die Liste kommt einfach umgedreht zurück.
Ein vollständig stiller Fehler.

**Messbarer Nutzen des Mockings:** 11 gemockte Service-Tests in 0,587 s gegen einen einzigen
`@SpringBootTest` in 13,10 s.

**12 Befunde** am Vorgabecode dokumentiert (E-01…E-05, S-01, S-02, C-01…C-04, K-01…K-03).

---

## Kapitel 5 — Automation Testing

📁 [`5-automation-testing/`](5-automation-testing/) · 📄 [Übersicht](5-automation-testing/README.md)

**Testobjekt:** Student-App (Spring Boot + Angular, Vorgabe)

| Übung | Werkzeug | Ergebnis |
|---|---|---|
| 1 | Playwright (`request`) | **14** API-Tests, grün |
| 2 | Playwright (Chromium) | **12** E2E-Tests, grün |
| 3 | k6 | [Lasttest](5-automation-testing/uebung3-lasttest.md) — 3 Szenarien, alle Thresholds eingehalten |
| Bonus | Bean Validation + Angular | [Feature-Spezifikation und Reflexion](5-automation-testing/bonus-feature.md) |

**Gesamtergebnis: `26 passed`**

**Lasttest-Messwerte** (smoke / load / stress): bis 273,5 Anfragen/s, Fehlerquote **0,00 %** in
allen Szenarien.

Zwei Befunde, die funktionale Tests nicht gefunden hätten:

1. **Der Stresslauf hat nicht den Server gemessen, sondern den Lastgenerator** —
   `dropped_iterations 1917` bei erschöpftem VU-Pool. Ohne diesen Blick hätte man eine
   Serverbremse diagnostiziert, die es nicht gibt.
2. **`GET /students` skaliert nicht:** 25 Datensätze → 1,4 KB / 5,5 ms; 2 533 Datensätze →
   138 KB / 14,0 ms bei identischer Last. Kein Paging.

**Bonus-Feature:** Eingabevalidierung (Backend + Frontend), vor der ersten Codezeile spezifiziert
und auf 45 min geschätzt. Die lehrreichste Stelle: Das Feature war zuerst **teilweise wirkungslos**,
weil `maxlength="100"` im Formular den einzigen rein serverseitigen Fehler unerreichbar machte —
implementiert, getestet und trotzdem toter Code. Aufgefallen erst beim Versuch, einen Test dafür
zu schreiben.

---

## Kapitel 6 — Testkonzept

📁 [`6-testkonzept-vertieft/`](6-testkonzept-vertieft/) ·
📄 [Theorie](6-testkonzept-vertieft/README.md) ·
📄 [Testkonzept](6-testkonzept-vertieft/testkonzept.md)

**Aufgabe:** Ein Testkonzept für das eigene Projekt schreiben, ca. 1–2 Seiten, mit den Elementen
nach **IEEE 829**.

**Ergebnis:** Testkonzept mit allen acht geforderten Abschnitten:

1. Zusammenfassung · 2. Big Picture mit Test Items (Architekturdiagramm) · 3. Test Features ·
4. Features not to be tested · 5. Testvorgehen nach TDD · 6. Pass/Fail-Kriterien ·
7. Testumgebung · 8. Kurze Planung

> **Einschränkung:** Das Modulprojekt ist noch nicht gewählt. Als Testobjekt dient vorläufig die
> Student-App aus Kapitel 5; das steht als Hinweis im Dokument. Sobald das Projekt feststeht,
> werden vier Abschnitte ausgetauscht.

---

## Kapitel 7 — CI/CD-Pipeline und Deployment Environments

📁 [`7-cicd-pipelines/`](7-cicd-pipelines/) ·
📄 **[Zum Vorzeigen: VORZEIGEN.md](7-cicd-pipelines/VORZEIGEN.md)**

Zwei Kapitel in einem Ordner:

| | Theorie | Lösung |
|---|---|---|
| CI/CD | [README.md](7-cicd-pipelines/README.md) | [uebung-recipe-planner.md](7-cicd-pipelines/uebung-recipe-planner.md) |
| Deployment Environments | [deployment-environment/README.md](7-cicd-pipelines/deployment-environment/README.md) | [uebung-environments.md](7-cicd-pipelines/deployment-environment/uebung-environments.md) |

**Testobjekt:** `recipe-planner` (Spring Boot 3.2.1 + React, Vorgabe)

### CI/CD

| Aufgabe | Inhalt | Ergebnis |
|---|---|---|
| 1 | Alle Controller-Methoden via MockMvc · Mapper-Tests mit **SoftAssertions** | **42 Tests**, grün |
| 2 | Automatisierte Reports | Surefire + JaCoCo, **95,1 %** Coverage |
| 3 | Pipeline, durch Push getriggert | [Lauf #1](https://github.com/David-Tarlos/Testing-m450/actions/runs/35566362981): **success in 63 s** |

**Die Coverage-Geschichte:** Der erste Report meldete **38,0 %**, obwohl Controller und Service je
100 % hatten — und 0 von 172 Branches. Bei Datenklassen mit nur Gettern kann es keine 172
Verzweigungen geben. Ursache war **Lombok**: `@Data` erzeugt `equals`, `hashCode` und `toString` als
echten Bytecode, den JaCoCo mitzählt. Eine Zeile in `lombok.config` löst es:

```properties
lombok.addLombokGeneratedAnnotation = true
```

Ergebnis mit denselben Tests und demselben Code: **95,1 %**.
*Eine Coverage-Zahl ist erst aussagekräftig, wenn klar ist, was gemessen wird.*

**SoftAssertions belegt statt behauptet:** Ein Lauf mit drei absichtlich falschen Feldern meldet
`Multiple Failures (3 failures)` — alle drei auf einmal, das korrekte vierte Feld nicht.
Der Nachweis ist als grüner Test in der Suite verankert.

**Pipeline:** GitHub Actions statt GitLab CI (das Repo liegt auf GitHub, die Theorie lässt es
ausdrücklich zu). Jobs `build` → `test` via `needs`, Reports als Artefakt **und** als Job-Summary.

### Deployment Environments

| Aufgabe | Inhalt | Ergebnis |
|---|---|---|
| 1 | Docker Compose, Kubernetes, Vagrant, Terraform vergleichen und den Umgebungen zuordnen | Vergleich entlang zweier Achsen: Container/VM und Anwendung/Infrastruktur |
| 2 | Eine Umgebung aufsetzen (~1 Lektion) + Reflexion | **Testing Environment** mit Docker Compose, live hochgefahren |
| 3 | optional — vollautomatisiertes Setup | nicht bearbeitet |

**Live verifiziert:** Backend HTTP 200 (healthy), Frontend HTTP 200, 15 Seed-Rezepte,
CORS 200 gegen 403, Container-zu-Container-Auflösung funktioniert.

**Der interessanteste Befund:** Port 8080 musste auf den Host veröffentlicht werden, obwohl beide
Container im selben Netzwerk liegen — das Frontend hat `http://localhost:8080` fest im Code, und
dieser Aufruf passiert im Browser, nicht im Container.
*Containerisierbarkeit entscheidet sich im Anwendungscode, nicht im Compose-File.*

**15 Befunde** am Vorgabecode dokumentiert, darunter `CrudRepository<RecipeEntity, Long>` bei einem
`UUID`-Schlüssel und eine unbekannte Id, die **200 mit leerem Body** statt 404 ergibt.

---

## Kapitel 8 und 9 — offen

`8-code-reviews/` und `9-test-driven-development/` sind noch leer. Beide gehören zu den Bausteinen
des Modulprojekts.

---

## Modulprojekt — offen

Laut `Unterlagen/projekt/` im Kurs-Repository ist ein **eigenes Projekt** zu realisieren:

| Bereich | Anforderung |
|---|---|
| Tests | Unit- und Component-Tests, Schnittstellen per Mocking |
| Reports | automatisierte Test-Reports, Coverage einsehbar (z. B. Sonar) |
| Vorgehen | TDD praktizieren |
| CI/CD | automatisierte Testausführung beim Deploy auf `main` |
| Reviews | mindestens 3 Pull Requests mit aktiven Kommentaren |
| Doku | Projektplanung, visualisierte Architektur, **Testkonzept**, Reflexion zu TDD und Code Reviews |
| Präsentation | 10 Minuten: Demo, Testing, Reports, Reflexion, Fazit |

Das Testkonzept aus Kapitel 6 ist bereits ein Baustein davon.

---

## Werkzeuge im Repository

| Werkzeug | Eingesetzt in |
|---|---|
| JUnit 5 | alle Java-Kapitel |
| Mockito | Kapitel 4, 7 |
| AssertJ / SoftAssertions | Kapitel 7 |
| MockMvc, `@WebMvcTest`, `@DataJpaTest` | Kapitel 4, 7 |
| JaCoCo | Kapitel 3, 7 |
| Playwright | Kapitel 5 |
| k6 | Kapitel 5 |
| GitHub Actions | Kapitel 7 |
| Docker Compose | Kapitel 7 |

---

## Hinweis zur Nutzung von KI

Die Ausarbeitungen in diesem Repository sind unter Einsatz eines KI-Assistenten entstanden —
als Werkzeug zum Schreiben, Recherchieren und Review. Alle Testläufe, Messwerte und
Pipeline-Ergebnisse wurden tatsächlich ausgeführt und sind in den jeweiligen Kapitel-Dokumenten
mit der echten Ausgabe belegt; es wurde kein Ergebnis behauptet, das nicht gelaufen ist.
