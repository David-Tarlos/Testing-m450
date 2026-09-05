# Übung 3 – Lasttest der Student-API mit k6

**Werkzeug:** [k6](https://k6.io) v2.2.0 (Grafana Labs)
**Ziel:** `http://localhost:8081/students` (Spring Boot 3.1.2, H2 in-memory)
**Skript:** [`automation/load/students-load.js`](automation/load/students-load.js)
**Datum:** 01.09.2026

---

## Warum k6 statt JMeter oder Postman

| | k6 | JMeter | Postman/Newman |
|---|---|---|---|
| Testdefinition | JavaScript | XML über GUI | JSON-Collection |
| Im Git reviewbar | ja, liest sich wie Code | kaum – generiertes XML | mühsam, verschachteltes JSON |
| Ressourcenbedarf | gering (Go, ein Binary) | hoch (JVM pro Thread) | nicht für Last gedacht |
| Pass/Fail für CI | Thresholds, Exit-Code 99 | über Plugins | begrenzt |
| Einstiegshürde | Skript schreiben | GUI-Klicken | sehr niedrig |

Ausschlaggebend war die **Versionierbarkeit**: eine `.jmx`-Datei im Pull Request zu
reviewen ist praktisch unmöglich, ein 200-Zeilen-JS-Skript dagegen problemlos. Dazu
kommt, dass k6 mit einem einzigen Binary auskommt – kein JDK, keine GUI, keine
Plugin-Installation.

Der Nachteil, ehrlicherweise: JMeter hat eine GUI zum Zusammenklicken und eine
riesige Protokoll-Palette (JDBC, JMS, FTP). Wer kein JavaScript schreiben will,
ist dort besser aufgehoben.

## Setup

Docker Desktop lief auf dem Rechner nicht, das MSI-Paket wollte Admin-Rechte.
Deshalb das portable ZIP – reicht völlig:

```bash
# einmalig
curl -L -o k6.zip https://github.com/grafana/k6/releases/download/v2.2.0/k6-v2.2.0-windows-amd64.zip
# entpacken, Ordner in den PATH

# Backend starten (JDK 17!), dann:
cd automation/load
k6 run students-load.js -e SCENARIO=smoke
k6 run students-load.js -e SCENARIO=load
k6 run students-load.js -e SCENARIO=stress
```

Alternativ ohne lokale Installation, sofern Docker läuft:

```bash
docker run --rm -i grafana/k6 run - < students-load.js \
  -e BASE_URL=http://host.docker.internal:8081
```

---

## Erkundete k6-Funktionalitäten

### Executors – wie die Last erzeugt wird

Das war die wichtigste Erkenntnis beim Erkunden. k6 unterscheidet zwei
grundsätzlich verschiedene Denkweisen:

**Offene vs. geschlossene Modelle.** Bei `ramping-vus` (geschlossen) gibt man eine
Anzahl gleichzeitiger Nutzer vor. Wird der Server langsamer, sinkt automatisch die
Anfragerate – die Nutzer warten ja. Bei `ramping-arrival-rate` (offen) gibt man
eine **Anfragerate** vor, die unabhängig davon gehalten wird, wie schnell der
Server antwortet. Nur das zweite Modell beantwortet die Frage „wie viele Anfragen
pro Sekunde hält das System aus".

Im Skript sind drei Szenarien hinterlegt, umschaltbar per `-e SCENARIO=`:

| Szenario | Executor | Profil | Zweck |
|---|---|---|---|
| `smoke` | `shared-iterations` | 1 VU, 10 Iterationen | Läuft die Strecke überhaupt? |
| `load` | `ramping-vus` | 0 → 20 VUs, halten, ausrampen | Verhalten bei Normallast |
| `stress` | `ramping-arrival-rate` | 50 → 600 Anfragen/s | Wo ist die Grenze? |

### Thresholds – der Lasttest als Pass/Fail-Kriterium

Der für CI wichtigste Mechanismus. Wird eine Regel gerissen, endet k6 mit
Exit-Code **99** und die Pipeline wird rot:

```js
thresholds: {
  http_req_failed:   ['rate<0.01'],              // unter 1 % Fehler
  http_req_duration: ['p(95)<500', 'p(99)<1000'],
  checks:            ['rate>0.99'],
  lese_dauer:        ['p(95)<300'],              // eigene Metrik
  schreib_dauer:     ['p(95)<600'],
}
```

### Checks – Assertions, die den Lauf nicht abbrechen

`check()` ist bewusst kein `assert`. Ein fehlgeschlagener Check zählt in die
`checks`-Rate, bricht den Lauf aber nicht ab – bei 14 000 Anfragen will man
schliesslich die Statistik und nicht den Abbruch bei der ersten Abweichung.

```js
check(response, {
  'Status 200':          (r) => r.status === 200,
  'Antwort ist ein Array': (r) => Array.isArray(r.json()),
  'Liste ist nicht leer':  (r) => r.json().length > 0,
});
```

### Eigene Metriken

`http_req_duration` mischt Lese- und Schreibzugriffe zu einer Zahl. Mit eigenen
Metriken lässt sich das trennen – und genau das hat später den interessantesten
Befund geliefert:

| Typ | Verwendung im Skript |
|---|---|
| `Trend` | `lese_dauer`, `schreib_dauer` – Verteilung von Antwortzeiten |
| `Rate` | `fachliche_fehler` – Anteil fehlgeschlagener Checks |
| `Counter` | `angelegte_studenten` – wie viele Datensätze der Lauf erzeugt hat |
| `Gauge` | (nicht genutzt) letzter Wert, z. B. Queue-Länge |

### Lifecycle, Gruppen und Tags

* `setup()` läuft **einmal** vor allen VUs – hier: prüfen, ob das Backend
  überhaupt erreichbar ist, und den Startbestand merken. Der Rückgabewert wird an
  `default()` und `teardown()` durchgereicht.
* `teardown()` läuft einmal am Ende – hier: melden, wie viele Datensätze der Lauf
  angelegt hat.
* `group()` fasst Anfragen fachlich zusammen („Studenten lesen" / „Student
  anlegen").
* Tags (`tags: { name: 'GET /students' }`) erlauben es, die Statistik pro Endpunkt
  auszuwerten.

### handleSummary – eigene Auswertung

Ersetzt die Standardausgabe. Wir schreiben zusätzlich eine JSON-Datei pro
Szenario nach `load/results/`, damit sich Läufe später vergleichen lassen:

```js
return {
  stdout: zeilen.join('\n'),
  [`results/summary-${SCENARIO}.json`]: JSON.stringify(data, null, 2),
};
```

### Was k6 sonst noch kann (nicht ausprobiert)

Ausgabe nach Prometheus, InfluxDB oder Datadog für Live-Dashboards in Grafana;
Browser-Modul für echte Browser-Last; `xk6` zum Erweitern um eigene Protokolle
(gRPC, SQL, Kafka); verteiltes Ausführen über k6 Cloud oder den Kubernetes-Operator.

---

## Messergebnisse

Alle Läufe auf dem Entwicklungsrechner, Backend und Last auf derselben Maschine –
absolute Zahlen sind also nicht auf Produktion übertragbar, die Verhältnisse schon.

| | smoke | load | stress |
|---|---|---|---|
| Dauer | 10,1 s | 56,0 s | 50,9 s |
| Max. gleichzeitige VUs | 1 | 20 | 400 |
| Anfragen total | 15 | 1 027 | 13 918 |
| Anfragen/s (erreicht) | 1,5 | 18,3 | **273,5** |
| Fehlerquote HTTP | 0,00 % | 0,00 % | 0,00 % |
| Checks bestanden | 100 % | 100 % | 100 % |
| Antwortzeit Median | 4,2 ms | 2,4 ms | 5,6 ms |
| Antwortzeit p95 | 7,1 ms | 4,8 ms | 39,7 ms |
| Antwortzeit max | 9,5 ms | 12,2 ms | 190,5 ms |
| Lesen p95 | 5,5 ms | 5,0 ms | 43,3 ms |
| Schreiben p95 | 3,5 ms | 3,6 ms | 14,1 ms |
| **Alle Thresholds** | **OK** | **OK** | **OK** |

---

## Befunde

### 1. Die Anwendung ist bei Normallast unauffällig

20 gleichzeitige Nutzer, 18 Anfragen/s, p95 unter 5 ms, keine Fehler. Für eine
Schulanwendung mit In-Memory-Datenbank völlig erwartbar – und ein gutes Zeichen,
dass die Messung stimmt.

### 2. Der Stresslauf hat *nicht* den Server gemessen

Das ist der methodisch wichtigste Punkt. Vorgabe waren 600 Anfragen/s, erreicht
wurden 273. Zunächst sah das nach einer Serverbremse aus. Die Metriken sagen etwas
anderes:

```
dropped_iterations   1917
vus_max              400   (= konfiguriertes Maximum)
```

k6 hat 1 917 Iterationen **verworfen, weil keine VUs mehr frei waren**. Jede
Iteration enthält ein `sleep(1)`; bei 400 VUs sind damit rechnerisch höchstens
~400 Iterationen/s möglich. Der Engpass war also das Testwerkzeug, nicht die
Anwendung. Die Fehlerquote von 0 % und die p95 von 40 ms bestätigen das – ein
überlasteter Server sähe anders aus.

**Lehre:** Ein Lasttest misst immer die Kombination aus System *und* Lastgenerator.
Ohne den Blick auf `dropped_iterations` hätte man hier eine Serverbremse
diagnostiziert, die es gar nicht gibt. Für eine echte Grenzwertmessung müsste
`maxVUs` deutlich höher stehen oder das `sleep()` entfallen.

### 3. `GET /students` skaliert nicht mit der Datenmenge

Der interessanteste fachliche Befund. Der Endpunkt liefert **immer die komplette
Tabelle** – kein Paging, kein Limit. Isoliert gemessen mit identischer Last
(1 VU, `smoke`), nur unterschiedlich gefüllter Datenbank:

| Datensätze | Antwortgrösse | Lesen p95 |
|---|---|---|
| 25 | ~1,4 KB | 5,5 ms |
| 2 533 | 138 KB | 14,0 ms |

Faktor 2,5 bei der Antwortzeit, Faktor 100 beim übertragenen Volumen – bei
gleicher Last. Das wächst linear weiter. Bei 100 000 Studenten wären es rund 5 MB
pro Seitenaufruf, die das Angular-Frontend auch noch rendern müsste. Für
Produktion bräuchte es Paging (`Pageable`) oder wenigstens ein Limit.

### 4. Der Lasttest hat die Datenbank vermüllt

2 334 Datensätze wie `k6-17-42 / k6-17-42@tbz.ch` sind während des Stresslaufs
entstanden. Weil H2 in-memory läuft, ist nach einem Neustart alles weg – in einer
echten Umgebung wäre das ein Problem. Ursache ist, dass `POST /students` **keinerlei
Validierung** hat und alles annimmt.

Das ist derselbe Befund, den schon die API-Tests aus Übung 1 festgehalten haben
(leere Namen, ungültige E-Mails, leeres Objekt → alles 200 OK). Genau diese Lücke
schliesst das [Bonus-Feature](bonus-feature.md).

---

## Fazit

Für diese Anwendung ist Last kein Problem – die Grenze liegt weit oberhalb dessen,
was eine Schulanwendung je sehen wird. Wertvoll war der Lasttest trotzdem, aber aus
einem anderen Grund als erwartet: Er hat zwei strukturelle Schwächen sichtbar
gemacht, die bei funktionalen Tests nicht auffallen – die fehlende Paginierung und
die fehlende Eingabevalidierung.

Und er hat eine Falle gezeigt, in die man beim ersten Lasttest zuverlässig tappt:
zu glauben, man messe den Server, während man in Wirklichkeit den Lastgenerator
misst.
