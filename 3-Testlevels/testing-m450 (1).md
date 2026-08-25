# Testing – Modul 450

## Aufgabe 1: Wie wird bei uns getestet?

**Test Levels:** Unit Tests (einzelne Methoden und Komponenten) und End-to-End Tests mit Playwright (Klick durch die fertige Anwendung). Integrationstests als eigene Stufe haben wir nicht, das läuft über die E2E-Tests mit.

**Wann:** Automatisch in der CI/CD-Pipeline bei jedem Push auf einen Branch. Zusätzlich lokal während der Entwicklung.

**Testing- oder QA-Team:** Kein eigenes Team. Wir Entwickler schreiben die Tests selbst. In der QA-Umgebung prüft eine Person manuell Oberfläche und Funktionen.

**Testing Life Cycle:**
1. Feature entwickeln, Unit Tests dazu schreiben
2. Lokal testen
3. Push, Pipeline baut und testet
4. Merge nur bei grüner Pipeline
5. Manuelle Prüfung in QA
6. Release

**Fazit:** Schnelles Feedback, weil fast alles automatisch läuft. Schwachstellen: keine echte Integrationstest-Stufe, und das manuelle Testen hängt an einer einzigen Person.

---

## Aufgabe 2: Einordnung der Begriffe

Die fünf Begriffe sind fünf Fragen zum selben Test, von abstrakt nach konkret:

```
Approach    → warum und wann wird getestet
Levels      → wie gross ist das Testobjekt
Types       → was wird geprüft
Techniques  → wie kommt man auf die Testfälle
Tactics     → womit wird es umgesetzt
```

**Approach** ist die Grundhaltung: früh testen und Tests parallel zum Code schreiben (präventiv, Shift Left, TDD), erst am Schluss testen (reaktiv), oder dort ansetzen, wo ein Fehler am meisten Schaden anrichtet (risikobasiert).

**Levels** sagen, wie gross das Getestete ist: Unit (eine Methode), Integration (Zusammenspiel mehrerer Teile), System (ganze Anwendung), Abnahme (Kunde prüft). Je weiter unten, desto schneller und billiger, aber desto weniger Aussage über das Ganze.

**Types** sagen, welche Eigenschaft geprüft wird: funktional (tut es, was es soll), nicht-funktional (wie schnell, wie sicher, wie bedienbar), strukturell (wie viel Code wird durchlaufen, Coverage), änderungsbezogen (Bug wirklich weg, nichts anderes kaputt = Regression).

**Techniques** sind Verfahren, um systematisch auf Testfälle zu kommen statt zu raten. Black-Box ohne Codekenntnis: Äquivalenzklassen (Eingaben gruppieren, die sich gleich verhalten), Grenzwertanalyse (die Ränder testen, dort sitzen die Fehler), Entscheidungstabelle (Kombinationen durchspielen). White-Box mit Codekenntnis: wurde jede Zeile, jeder if-Zweig durchlaufen. Erfahrungsbasiert: Exploratory Testing, Error Guessing.

**Tactics** ist die Umsetzung: welche Tools (JUnit, Playwright), wie verteilt (Testpyramide: viele Unit, wenige E2E), woher die Testdaten (Mocks, Testdatenbank), wann bricht die Pipeline ab.

### Abhängigkeiten

Der Approach gibt vor, welche Levels überhaupt genutzt werden. Wer erst am Schluss testet, hat selten viele Unit Tests.

Jedes Level kann mehrere Types enthalten. Ein Systemtest kann funktional sein oder ein Lasttest. **Level und Type sind unabhängig voneinander.**

Jeder Type lässt sich mit verschiedenen Techniques umsetzen, und die Techniques bestimmen die Tools. White-Box braucht ein Coverage-Tool, Black-Box nicht.

### Unser Vorgehen eingeordnet

| Begriff | Bei uns |
|---|---|
| Approach | Früh und automatisiert, entwicklergetrieben über CI/CD |
| Levels | Unit und E2E, kein Integrationstest, manuelle Abnahme in QA |
| Types | Funktional und Regression, nicht-funktionale Tests fehlen |
| Techniques | Black-Box bei Playwright, White-Box-Anteile bei Unit Tests |
| Tactics | Playwright und Unit-Test-Framework, Ausführung bei jedem Push |

### Beispiel

Ein Playwright-Test, der prüft, ob eine Kategorie-Spalte richtig eingefärbt wird, ist gleichzeitig: früh und automatisiert (Approach), Systemtest (Level), funktional und ab dem zweiten Lauf Regression (Type), Black-Box mit Äquivalenzklassen (Technique), Playwright in der Pipeline (Tactics).
