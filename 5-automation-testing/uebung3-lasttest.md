# Übung 3 – Lasttest der Student-API mit k6

**Werkzeug:** [k6](https://k6.io) v2.2.0 (Grafana Labs)
**Ziel:** `http://localhost:8081/students` (Spring Boot 3.1.2, H2 in-memory)
**Skript:** [`automation/load/students-load.js`](automation/load/students-load.js)

---

## Warum k6 statt JMeter oder Postman

| | k6 | JMeter | Postman/Newman |
|---|---|---|---|
| Testdefinition | JavaScript | XML über GUI | JSON-Collection |
| Im Git reviewbar | ja | kaum – generiertes XML | mühsam |
| Ressourcenbedarf | gering (ein Binary) | hoch (JVM pro Thread) | nicht für Last gedacht |
| Pass/Fail für CI | Thresholds, Exit-Code 99 | über Plugins | begrenzt |

Ausschlaggebend war die **Versionierbarkeit**: eine `.jmx`-Datei im Pull Request zu
reviewen ist praktisch unmöglich, ein 100-Zeilen-JS-Skript dagegen problemlos.

Der Nachteil, ehrlicherweise: JMeter hat eine GUI zum Zusammenklicken und viel mehr
Protokolle (JDBC, JMS, FTP). Wer kein JavaScript schreiben will, ist dort besser
aufgehoben.

## Starten

k6 als portables ZIP von der Projektseite laden und entpacken – kein Installer,
keine Admin-Rechte. Backend muss laufen.

```bash
cd automation/load
k6 run students-load.js -e SCENARIO=smoke     # 10 s
k6 run students-load.js -e SCENARIO=load      # 56 s
k6 run students-load.js -e SCENARIO=stress    # 51 s
```

---

## Erkundete k6-Funktionalitäten

**Executors.** Die wichtigste Erkenntnis: k6 kennt zwei grundsätzlich verschiedene
Denkweisen. Bei `ramping-vus` gibt man eine **Nutzerzahl** vor – wird der Server
langsamer, sinkt die Anfragerate automatisch mit, weil die Nutzer ja warten. Bei
`ramping-arrival-rate` gibt man eine **Anfragerate** vor, die unabhängig davon
gehalten wird. Nur das zweite Modell beantwortet, wie viele Anfragen pro Sekunde
das System aushält.

| Szenario | Executor | Profil | Zweck |
|---|---|---|---|
| `smoke` | `shared-iterations` | 1 VU, 10 Iterationen | Läuft die Strecke? |
| `load` | `ramping-vus` | 0 → 20 VUs, halten, ausrampen | Normallast |
| `stress` | `ramping-arrival-rate` | 50 → 600 Anfragen/s | Wo ist die Grenze? |

**Thresholds.** Machen aus dem Lasttest ein Pass/Fail – wird eine Regel gerissen,
endet k6 mit Exit-Code 99 und eine Pipeline könnte rot werden.

```js
thresholds: {
  http_req_failed:   ['rate<0.01'],   // unter 1 % Fehler
  http_req_duration: ['p(95)<500'],
  checks:            ['rate>0.99'],
  lese_dauer:        ['p(95)<300'],   // eigene Metrik
  schreib_dauer:     ['p(95)<600'],
}
```

**Checks.** Prüfungen, die den Lauf nicht abbrechen – ein Fehlschlag zählt nur in
die Quote. Bei 14 000 Anfragen will man die Statistik, nicht den Abbruch beim
ersten Ausreisser.

**Eigene Metriken.** `http_req_duration` mischt Lesen und Schreiben zu einer Zahl.
Getrennt gemessen (`lese_dauer`, `schreib_dauer`) – das hat den interessantesten
Befund geliefert.

**setup / teardown.** Je einmal vor und nach dem Lauf: prüfen, ob das Backend
erreichbar ist, und am Ende den Datenbestand melden.

---

## Ergebnisse

| | smoke | load | stress |
|---|---|---|---|
| Dauer | 10,1 s | 56,0 s | 50,9 s |
| Gleichzeitige Nutzer | 1 | 20 | 400 |
| Anfragen total | 15 | 1 027 | 13 918 |
| Anfragen/s | 1,5 | 18,3 | 273,5 |
| Fehlerquote | 0 % | 0 % | 0 % |
| Antwortzeit p95 | 7,1 ms | 4,8 ms | 39,7 ms |
| Thresholds | OK | OK | OK |

---

## Befunde

**Bei Normallast ist die App unauffällig.** 20 gleichzeitige Nutzer, keine Fehler,
p95 unter 5 ms.

**Der Stresslauf hat den Lastgenerator gemessen, nicht den Server.** Vorgegeben
waren 600 Anfragen/s, erreicht wurden 273 – das sah zuerst nach einer Grenze des
Servers aus. Die Metriken sagten etwas anderes:

```
dropped_iterations   1917
vus_max               400   (= unser konfiguriertes Maximum)
```

k6 hat 1 917 Durchläufe verworfen, weil keine virtuellen Nutzer mehr frei waren.
Jeder Durchlauf enthält eine Sekunde Denkzeit, also sind mit 400 Nutzern nicht mehr
als etwa 400 Durchläufe pro Sekunde möglich. Dass die Fehlerquote bei 0 % lag,
bestätigt das – ein überlasteter Server sähe anders aus. **Ein Lasttest misst immer
System und Lastgenerator zusammen.**

**`GET /students` liefert immer die ganze Tabelle.** Kein Paging, kein Limit.
Gleiche Last, nur unterschiedlich volle Datenbank:

| Datensätze | Antwortgrösse | Lesen p95 |
|---|---|---|
| 25 | 1,4 KB | 5,5 ms |
| 2 533 | 138 KB | 14,0 ms |

Das wächst linear weiter. Funktional ist der Endpunkt völlig korrekt – deshalb
hätte das kein Test aus Übung 1 oder 2 je gefunden.

**Der Lasttest hat die Datenbank vollgeschrieben.** Rund 2 300 Datensätze wie
`k6-17-42@tbz.ch`, weil `POST /students` damals noch jede Eingabe annahm. Diese
Lücke schliesst das [Bonus-Feature](bonus-feature.md).
