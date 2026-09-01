# Unit Testing (m450)

Lösungen zu den vier Übungen aus `Unterlagen/unit-testing/UEBUNGEN.md`.

| Aufgabe | Thema | Wo |
|---|---|---|
| 1 | Simpler Rechner (JUnit-5-Grundlagen) | [`aufgabe1-calculator/`](aufgabe1-calculator) |
| 2 | JUnit-Zusammenfassung | [`aufgabe2-junit-zusammenfassung.md`](aufgabe2-junit-zusammenfassung.md) |
| 3 | Banken-Simulation aufsetzen und verstehen | [`aufgabe3-4-bank-simulation/DOKUMENTATION.md`](aufgabe3-4-bank-simulation/DOKUMENTATION.md) |
| 4 | Unit-Tests für die Banken-Simulation | [`aufgabe3-4-bank-simulation/src/test/`](aufgabe3-4-bank-simulation/src/test/java/ch/schule/bank/junit5) |

## Aktueller Stand

| Projekt | Tests | Line Coverage | Branch Coverage |
|---|---|---|---|
| aufgabe1-calculator | 28 grün | 100 % | 100 % |
| aufgabe3-4-bank-simulation | 92 grün | 100 % | 100 % |

(Die Klasse `Main` ist von der Coverage-Messung ausgenommen — sie ist nur eine
Demo-Einstiegsklasse ohne Fachlogik.)

## Voraussetzungen

* **JDK 17 oder neuer** (getestet mit Liberica JDK 21). Prüfen mit `java -version`.
* **Maven** muss *nicht* installiert sein — beide Projekte bringen den
  Maven-Wrapper (`mvnw` / `mvnw.cmd`) mit. Beim ersten Aufruf lädt er Maven 3.8.5
  automatisch herunter, danach läuft er offline.
* Eine IDE mit JUnit-5-Unterstützung (IntelliJ IDEA, Eclipse, VS Code) — optional,
  aber für Aufgabe 1 ausdrücklich verlangt.

## Ausführen

### Variante A: Kommandozeile (Maven)

Aus dem jeweiligen Projektordner:

```powershell
# Windows PowerShell / CMD
.\mvnw.cmd test
```

```bash
# Git Bash / Linux / macOS
./mvnw test
```

Nützliche Varianten:

| Befehl | Wirkung |
|---|---|
| `.\mvnw.cmd test` | Kompiliert und führt alle Tests aus, erzeugt den Coverage-Report |
| `.\mvnw.cmd verify` | Wie `test`, prüft zusätzlich die Coverage-Schwellwerte (min. 80 % Lines / 75 % Branches) |
| `.\mvnw.cmd test -Dtest=BankTests` | Führt nur eine Testklasse aus |
| `.\mvnw.cmd test -Dtest=BankTests#testCreate` | Führt nur eine Testmethode aus |
| `.\mvnw.cmd clean test` | Löscht `target/` vorher — bei komischen Fehlern immer der erste Versuch |

### Variante B: Entwicklungsumgebung

**IntelliJ IDEA**

1. `File > Open` und die **`pom.xml`** des jeweiligen Projekts auswählen
   (nicht den Ordner!), dann "Open as Project".
2. Warten, bis Maven die Abhängigkeiten geladen hat (Fortschritt unten rechts).
3. Grünes Dreieck neben der Testklasse oder der Testmethode anklicken →
   *Run*. Alternativ: Rechtsklick auf `src/test/java` → *Run All Tests*.
4. Coverage: statt *Run* auf *Run with Coverage* (Symbol mit dem Schild) klicken.

**VS Code**: Extension Pack for Java installieren, Ordner öffnen, im
Test-Explorer (Becherglas-Symbol) die Tests starten.

### Coverage-Report ansehen

Nach `mvnw test` liegt der HTML-Report hier:

```
<projekt>/target/site/jacoco/index.html
```

Einfach im Browser öffnen. Grün = getestet, rot = nie ausgeführt, gelb = Verzweigung
nur teilweise abgedeckt.

```powershell
# direkt öffnen
start .\target\site\jacoco\index.html
```

## Abweichungen von der Vorgabe

Die Banken-Simulation stammt aus `02_bank-vorgabe`. Folgendes wurde angepasst:

* **Encoding repariert.** In der Vorgabe waren alle Umlaute in den Kommentaren
  durch das Unicode-Ersatzzeichen U+FFFD zerstört (die Dateien wurden einmal als
  ISO-8859-1 geschrieben und als UTF-8 wieder gespeichert). Alle Dateien sind
  jetzt sauber UTF-8.
* **`java.version` von 19 auf 17** gesenkt — läuft damit auf jedem JDK ab 17.
* **JUnit-Abhängigkeit** von `junit-jupiter-engine` auf den Aggregator
  `junit-jupiter` gewechselt, damit `@ParameterizedTest` (Modul `junit-jupiter-params`)
  verfügbar ist.
* **JaCoCo-Plugin** für die Code Coverage ergänzt (Aufgabe 4).
* **Maven-Wrapper** von Maven 3.6.3 auf 3.8.5 gehoben — 3.6.3 ist mit aktuellen
  JDKs nicht mehr zuverlässig.
* Nicht übernommen wurden `.idea/`, `target/`, `*.iml` und die macOS-Reste
  (`.DS_Store`, `__MACOSX`).

Der Produktivcode unter `src/main/java` wurde **inhaltlich nicht verändert** —
er ist der Testgegenstand. Auffälligkeiten darin sind in
[`DOKUMENTATION.md`](aufgabe3-4-bank-simulation/DOKUMENTATION.md) im Abschnitt
*Stolperfallen* beschrieben statt korrigiert.
