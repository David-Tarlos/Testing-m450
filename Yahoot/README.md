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

**Stand:** Das Spiel ist von der Lobby bis zum Sieger spielbar und mit 57 Unit-Tests und 15
Integrationstests im Backend, 54 Unit-Tests im Frontend und 14 Playwright-Tests abgesichert. Das
vollständige [Testkonzept](docs/testkonzept.md) und die [Schnittstelle](docs/api.md) sind dokumentiert.

## Spiel starten

```bash
docker run -p 8080:8080 ghcr.io/david-tarlos/yahoot:latest
# oder ohne Docker, nach dem Bauen wie unten beschrieben:
java -jar backend/target/yahoot-0.0.1-SNAPSHOT.jar
```

Den Beamer nie über `localhost` öffnen: Der QR-Code und der Link zeigen sonst auf `localhost`, und die Handys
kommen nicht ans Spiel. Stattdessen die Adresse nehmen, die auch die Handys erreichen, also die öffentliche
Adresse beim Hosting oder die IP des Laptops (`http://192.168.x.x:8080/host`). Schul-WLANs blockieren
Verbindungen zwischen Geräten oft. Dann hilft der Hotspot eines Handys, mit dem sich alle verbinden.

Beim Hosting den Port über die Umgebungsvariable `PORT` setzen, falls der Anbieter ihn vorgibt. Der Anbieter muss
WebSockets unterstützen.

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

Ein KI-Agent wurde als Werkzeug eingesetzt. Idee, Technologien, Anforderungen und Umfang wurden zuerst mit der KI besprochen, um eine solide Planung zu erhalten. Auf Grundlage dieser Vorgaben erstellte der Agent anschliessend Code, Tests und Dokumentation.

Beim Test-Driven Development kam die KI bewusst nur zurückhaltend zum Einsatz. Sie diente vor allem für Erklärungen und teilweise für wiederkehrende Tests, die gleich aufgebaut sind und sich nur in der getesteten Funktionalität unterscheiden. Alle Änderungen liefen über Pull Requests, und der Code wurde vor dem Merge selbst reviewt.
