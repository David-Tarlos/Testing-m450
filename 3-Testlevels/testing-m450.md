# Testing – Modul 450

## Aufgabe 1: Wie wird bei uns getestet?

**Test Levels:** Hauptsächlich Unit Tests und End-to-End Tests. Bei den E2E-Tests arbeiten wir mit Playwright. Eine eigene Integrationstest-Ebene gibt es nicht, das wird über die E2E-Tests mit abgedeckt.

**Wann:** Automatisiert in der CI/CD-Pipeline. Bei jedem Push auf einen Branch laufen Build und Testsuite. Zusätzlich lokal während der Entwicklung.

**Testing- oder QA-Team:** Kein eigenes Testing-Team. Im Dev testen wir Entwickler selbst mit Unit Tests und Playwright. In der QA-Umgebung prüft eine Person manuell die Benutzeroberfläche und die Funktionalitäten.

**Testing Life Cycle:**
1. Feature entwickeln, Unit Tests dazu schreiben
2. Lokal testen
3. Push → Pipeline führt Build und Tests aus
4. Merge nur bei grüner Pipeline
5. Manuelle Prüfung in der QA-Umgebung
6. Release

**Fazit:** Stark automatisiert und nah am Entwickler, dadurch schnelles Feedback. Schwachstellen: keine klare Integrationstest-Ebene, und das manuelle Testen hängt an einer einzigen Person.

---

## Aufgabe 2: Einordnung der Begriffe

Die Begriffe bauen von abstrakt nach konkret aufeinander auf:

```
Approach    → warum und wann getestet wird
Levels      → auf welcher Ebene
Types       → was geprüft wird
Techniques  → wie Testfälle entstehen
Tactics     → womit umgesetzt wird
```

**Testing Approach** – die Grundhaltung: präventiv (Shift Left, TDD), reaktiv, risikobasiert, automatisiert oder manuell.

**Testing Levels** – die Grösse des Testobjekts: Unit → Integration → System → Abnahmetest (UAT).

**Testing Types** – das Ziel: funktional, nicht-funktional (Performance, Security, Usability), strukturell (Code Coverage), änderungsbezogen (Regression, Fehlernachtest).

**Testing Techniques** – wie man Testfälle ableitet: Black-Box (Äquivalenzklassen, Grenzwertanalyse, Entscheidungstabelle), White-Box (Statement- und Branch Coverage), erfahrungsbasiert (Error Guessing, Exploratory Testing).

**Testing Tactics** – die Umsetzung: Tools wie JUnit oder Playwright, Testpyramide, Mocks und Testdaten, Quality Gates in der Pipeline.

### Abhängigkeiten

Der Approach entscheidet, welche Levels überhaupt bespielt werden. Jedes Level kann mehrere Types enthalten, ein Systemtest kann funktional oder ein Lasttest sein. Level und Type sind also unabhängige Dimensionen. Jeder Type lässt sich mit verschiedenen Techniques umsetzen, und die Techniques bestimmen, welche Tools und Tactics man braucht.

### Unser Vorgehen eingeordnet

| Begriff | Bei uns |
|---|---|
| Approach | Präventiv, automatisiert, entwicklergetrieben über CI/CD |
| Levels | Unit und E2E, kein Integrationstest, manuelle Abnahme in QA |
| Types | Funktional und Regression, nicht-funktionale Tests fehlen |
| Techniques | Black-Box in Playwright, White-Box-Anteile in den Unit Tests |
| Tactics | Playwright und Unit-Test-Framework, Ausführung bei jedem Push |
