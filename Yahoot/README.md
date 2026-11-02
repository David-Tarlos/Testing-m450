# Yahoot

Live-Quiz im Stil von Kahoot für Beamer und Handy, mit einem Unterschied: Wer richtig antwortet,
sammelt Energie und kann damit Mitspieler sabotieren. Details stehen im [Projektplan](docs/plan.md).

Modulprojekt m450. Backend mit Spring Boot, Frontend mit Angular, verbunden über WebSocket.

## Teststrategie

Die Spielregeln sind der riskanteste Teil, darum testen wir sie am gründlichsten und nach TDD. Die
Schichten darüber prüfen wir nur so weit, dass das Zusammenspiel sicher funktioniert.

| Ebene | Werkzeug | Was geprüft wird | Report |
|---|---|---|---|
| Unit Backend | JUnit, AssertJ, Mockito | Spielregeln, GameService mit gemockten Schnittstellen | Surefire, JaCoCo |
| Unit Frontend | Vitest | Dumb Components, Container mit gemocktem Socket-Service | JUnit-XML, Coverage |
| Integration | Spring Boot Test | echter Client gegen den echten Server | Failsafe, JaCoCo |
| End-to-End | Playwright | ganzes Spiel mit Host und Handys im Browser | HTML, JUnit-XML |

Gemockt werden Quiz-Quelle, Event-Versand und Uhr im Backend sowie der Socket-Service im Frontend. So
sind die Tests schnell und hängen nicht von echter Zeit ab.

Die Pipeline (`.github/workflows/yahoot.yml`) führt bei jedem Push und jedem Pull Request alle Tests aus
und legt die Reports als Artefakte ab. Ein neuer Push bricht den alten Lauf auf demselben Branch ab. Das
Docker-Image wird nur auf `main` veröffentlicht, und nur wenn alle Tests grün sind. Es bekommt neben
`latest` auch den Commit-Hash als Tag, damit sich jede Version zurückverfolgen lässt.

**Stand:** Das Spiel ist von der Lobby bis zum Sieger spielbar und mit 57 Unit-Tests und 12
Integrationstests im Backend, 51 Unit-Tests im Frontend und 13 Playwright-Tests abgesichert. Das
vollständige [Testkonzept](docs/testkonzept.md) und die [Schnittstelle](docs/api.md) sind dokumentiert.

## Tests lokal ausführen

Alle Befehle laufen vom Ordner `Yahoot/` aus. Die Klammern sorgen dafür, dass man danach wieder dort steht.

```bash
(cd backend && mvn verify)          # Unit und Integration, Report: target/site/jacoco
(cd frontend && npm run test:ci)    # Report: coverage/ und reports/

# End-to-End testet das fertige JAR
(cd frontend && npm run build)
(cd backend && mvn -Pwith-frontend -DskipTests package)
(cd e2e && npx playwright install chromium && npm test && npm run report)
```

Falls `npm install` mit `Cannot read properties of null (reading 'edgesOut')` abbricht: Das ist ein
Fehler von npm 11.4.2, `npx npm@11.21.1 install` funktioniert. `npm ci` ist davon nicht betroffen.

## KI-Nutzung

Wir nutzen einen KI-Agenten als Werkzeug. Idee, Technologien, Anforderungen und Umfang geben wir vor, und
wir entscheiden, was ins Projekt kommt: Jedes Paket übernehmen wir über einen eigenen Pull Request.

Der Agent erstellt nach diesen Vorgaben Code, Tests und Dokumentation.
