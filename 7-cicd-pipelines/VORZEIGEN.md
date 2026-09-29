# VORZEIGEN — Kapitel 7, von oben nach unten durchgehen

> **Das ist die einzige Datei, die du beim Vorzeigen brauchst.**
> Alles steht hier drin: was du öffnest, was du tippst, was du sagst.
> Von oben nach unten abarbeiten, Haken setzen, fertig.

---

## VORBEREITUNG — 10 Minuten vor dem Zeigen

- [ ] **Docker Desktop starten.** Windows-Taste → „Docker Desktop" → warten, bis das Wal-Symbol unten rechts ruhig ist.
- [ ] **Stack vorbauen** (dauert beim ersten Mal mehrere Minuten — deshalb JETZT, nicht vor der Klasse):

```
cd C:\Projects\Testing-m450\7-cicd-pipelines\deployment-environment
docker compose build
```

- [ ] **IntelliJ öffnen** mit `C:\Projects\Testing-m450\7-cicd-pipelines\recipe-planner\recipe-planner-backend`
- [ ] **Browser-Tab vorbereiten:** https://github.com/David-Tarlos/Testing-m450/actions
- [ ] **Diese Datei offen lassen.**

> Falls IntelliJ die Lombok-Getter rot anstreicht: Settings → Plugins → *Lombok* installieren, dann Build → Compiler → Annotation Processors → *Enable annotation processing*. Ändert nichts am Build, nur an der Anzeige.

---

## ÜBERBLICK — falls gefragt wird, wo was liegt

Es sind zwei Kapitel in einem Ordner. Deshalb gibt es zwei READMEs:

```
7-cicd-pipelines/
├── VORZEIGEN.md                      ← DIESE DATEI
├── README.md                         Theorie 1: Pipeline (aus den TBZ-Unterlagen)
├── uebung-recipe-planner.md          Lösung CI/CD, Aufgabe 1–3
├── recipe-planner/                   das Projekt mit meinen Tests
└── deployment-environment/
    ├── README.md                     Theorie 2: Umgebungen (aus den TBZ-Unterlagen)
    ├── uebung-environments.md        Lösung Deployment, Aufgabe 1–2
    └── docker-compose.yml            das Setup

.github/workflows/recipe-planner.yml  die Pipeline (muss im Repo-Root liegen)
```

**Die Regel:** `README.md` = Theorie vom Lehrer. `uebung-*.md` = meine Lösung.

---

# SCHRITT 1 — Aufgabe 1: Unit Testing

## 1a) Tests laufen lassen

**Öffnen:** in IntelliJ Rechtsklick auf
`src/test/java` → **Run 'All Tests'**

**Erwartetes Ergebnis:** grüner Baum, **42 Tests**, 0 Fehler.

```
Tests run: 42, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

| Testklasse | Tests |
|---|---|
| `RecipeControllerTest` | 14 |
| `IngredientEntityMapperTest` | 11 |
| `RecipeEntityMapperTest` | 10 |
| `RecipeServiceTest` | 7 |

## 1b) Controller-Tests zeigen

**Datei öffnen:**
`src/test/java/ch/tbz/recipe/planner/controller/RecipeControllerTest.java`

**Das sagst du:**

> „Die Aufgabe war, alle Controller-Methoden zu testen. `@WebMvcTest` startet nur den Web-Layer — kein JPA, keine Datenbank. Der Service ist mit `@MockBean` durch eine Attrappe ersetzt. Dadurch teste ich wirklich nur den Controller: Routing, Statuscodes, JSON."

**Nach unten scrollen und zeigen**, dass jede der drei Methoden auch Fehlerfälle hat:

| Methode | getestet |
|---|---|
| `GET /api/recipes` | 200, leeres Array, JSON-Contract |
| `GET /api/recipes/recipe/{id}` | 200, unbekannte Id, keine UUID → 400 |
| `POST /api/recipes` | 200, kaputtes JSON → 400, falscher Content-Type → 415 |
| übergreifend | unbekannte Route → 404, DELETE → 405, CORS 200 vs. 403 |

## 1c) SoftAssertions zeigen — der wichtigste Teil

**Datei öffnen:**
`src/test/java/ch/tbz/recipe/planner/mapper/RecipeEntityMapperTest.java`
→ ganz nach unten scrollen zur Klasse **`SoftAssertionsNachweis`**

**Das sagst du:**

> „Die Aufgabe fragt, ob ich den Vorteil von SoftAssertions verstanden habe. Statt ihn hinzuschreiben, habe ich ihn bewiesen. Mit normalen Assertions bricht der Test beim ersten falschen Feld ab — bei einem Mapper mit fünf Feldern heisst das fünf Durchläufe für fünf Fehler. SoftAssertions sammelt alle und meldet sie gemeinsam."

**Dann die echte Ausgabe zeigen** (steht im Test als Kommentar und hier):

```
Multiple Failures (3 failures)
-- failure 1 --
[id]
expected: "11111111-1111-1111-1111-111111111111"
 but was: "00000000-0000-0000-0000-000000000000"
-- failure 2 --
[name]
expected: "Lasagne al Forno"
 but was: "Pizza"
-- failure 3 --
[imageUrl]
expected: "https://example.test/lasagne.jpg"
 but was: "https://example.test/pizza.jpg"
```

**Drei Punkte dazu nennen:**
1. Alle drei Fehler auf einmal — eine Fehlersuche statt drei.
2. Das vierte Feld war korrekt und taucht nicht auf.
3. Die `.as("id")`-Beschriftung macht den Unterschied — sonst stünde da nur ein Wert.

> Falls gefragt wird, warum der Test grün ist, obwohl er Fehler zeigt: der erwartete Fehlschlag läuft in `assertThatThrownBy` — der Nachweis ist Teil der Suite, ohne sie rot zu machen.

---

# SCHRITT 2 — Aufgabe 2: Reports

## 2a) Surefire

**Ordner öffnen:**
`recipe-planner-backend\target\surefire-reports\`

> „Surefire läuft bei Maven ohnehin mit. Pro Testklasse eine `.txt` zum Lesen und eine `.xml`, die Werkzeuge einlesen können — die Pipeline nutzt genau diese XML."

## 2b) JaCoCo — im Browser öffnen

**Datei per Doppelklick öffnen:**
`recipe-planner-backend\target\site\jacoco\index.html`

**Erwartetes Bild:** Tabelle mit den Paketen, **95,1 %** Instruction Coverage.

**Das sagst du** — die Geschichte ist der stärkste Teil der ganzen Abgabe:

> „Der erste Report sagte **38 %**, obwohl Controller und Service je 100 % hatten. Und 0 von 172 Branches — bei Datenklassen mit nur Gettern und Settern kann es keine 172 Verzweigungen geben. Das war **Lombok**: `@Data` erzeugt `equals`, `hashCode` und `toString` als echten Bytecode, und JaCoCo zählt die mit. Die 172 Branches waren die Feldvergleiche im generierten `equals()`."

> „Die Lösung ist eine Zeile in `lombok.config`: `lombok.addLombokGeneratedAnnotation = true`. Lombok markiert seinen Code dann, und JaCoCo überspringt ihn. Ergebnis: **95,1 %** — gleiche Tests, gleicher Code, nur die Messung war vorher falsch."

**Datei zeigen:** `recipe-planner-backend\lombok.config` (eine Zeile)

**Der Satz zum Merken:**

> „Eine Coverage-Zahl ist erst aussagekräftig, wenn klar ist, *was* gemessen wird. 38 % und 95 % beschreiben denselben Testumfang."

---

# SCHRITT 3 — Aufgabe 3: Pipeline

## 3a) Die Workflow-Datei

**Datei öffnen:**
`C:\Projects\Testing-m450\.github\workflows\recipe-planner.yml`

**Erste Frage, die kommt — „Warum GitHub und nicht GitLab?"**

> „Unser Repo liegt auf GitHub. Die Theorie erlaubt das ausdrücklich: ‚Sie können alternativ auch mit GitHub arbeiten.' Die Konzepte sind identisch, nur die Schlüsselwörter heissen anders."

**Die Übersetzungstabelle** (steht auch oben in der Workflow-Datei als Kommentar):

| GitLab CI | GitHub Actions |
|---|---|
| `pipeline` | `workflow` — die ganze Datei |
| `stages:` mit Reihenfolge | Jobs mit `needs:` |
| `job` | `job` |
| `image: maven:latest` | `runs-on` + `actions/setup-java` |
| `variables:` | `env:` |
| `script:` | `steps[].run` |
| `artifacts:` | `actions/upload-artifact` |
| `pages` für Reports | Artefakte + Job-Summary |
| Shared Runner | GitHub-hosted Runner |

## 3b) Den echten Lauf zeigen

**Browser:** https://github.com/David-Tarlos/Testing-m450/actions → **Lauf #1**

Direktlink: https://github.com/David-Tarlos/Testing-m450/actions/runs/35566362981

**In dieser Reihenfolge zeigen:**

1. **Der Auslöser** — der Lauf hängt an Commit `9587fdb`. Ein Push hat ihn gestartet. *Das ist genau, was die Aufgabe verlangt.*
2. **Die zwei Jobs** — `build` → `test`, mit dem Pfeil dazwischen. Das ist `needs:`, also das Gegenstück zu „stages".
3. **Das Job-Summary** — Tabelle mit Tests/Failures/Errors und die Coverage in Prozent, direkt auf der Seite ohne Download.
4. **Die Artefakte** ganz unten — `surefire-report` und `jacoco-coverage-report` zum Herunterladen.

**Das echte Ergebnis:**

| | |
|---|---|
| Lauf | #1, Commit `9587fdb` |
| Dauer | **63 Sekunden** |
| Ergebnis | **success** |
| Artefakte | `surefire-report` (29 KB), `jacoco-coverage-report` (140 KB) |

**Drei Entscheidungen erklären, falls gefragt:**

> **Pfadfilter:** „Der Workflow startet nur, wenn sich am Backend etwas ändert. Sonst würde jeder Markdown-Commit im Repo einen Maven-Build auslösen."

> **Maven-Cache:** „`cache: maven` behält `~/.m2` zwischen den Läufen. Der erste Durchlauf lädt Spring Boot komplett, die folgenden nicht mehr."

> **`if: always()`:** „Die Reports werden auch hochgeladen, wenn die Tests fehlschlagen. Ein Report ist genau dann am wertvollsten, wenn etwas rot ist — sonst hätte man keine Ausgabe, um den Fehler zu verstehen."

---

# SCHRITT 4 — Deployment Aufgabe 1: Werkzeugvergleich

**Nichts öffnen — einfach erzählen.** Die Tabelle steht in `deployment-environment/uebung-environments.md`, falls jemand sie sehen will.

**Der Kerngedanke — zwei Achsen:**

```
                Container              VM
Anwendung       Docker Compose    │    Vagrant
                Kubernetes        │
                ──────────────────┼──────────────
Infrastruktur                     │    Terraform
```

**Das sagst du:**

> „Die vier Werkzeuge unterscheiden sich entlang zwei Achsen. Erstens: Container oder VM. Ein Container teilt sich den Kernel des Hosts und startet in Sekunden, eine VM bringt ein eigenes Betriebssystem mit — Minuten und Gigabytes. Zweitens: beschreibt das Werkzeug die **Anwendung** oder die **Infrastruktur**? Compose und Kubernetes sagen, *welche Dienste* laufen. Terraform sagt, *worauf* sie laufen. Deshalb konkurrieren die beiden nicht — in der Praxis legt Terraform den Kubernetes-Cluster an, und Kubernetes betreibt darin die Container."

**Die Zuordnung:**

| Umgebung | Werkzeug | Warum |
|---|---|---|
| Development | Vagrant oder Compose | schnell hoch, schnell weg, überall gleich |
| Testing | **Docker Compose** | mehrere Dienste, eine Maschine, eine Datei |
| Staging | Kubernetes + Terraform | muss der Produktion entsprechen |
| Production | Terraform + Kubernetes | Skalierung, Ausfallsicherheit, versionierte Infrastruktur |

**Falls „Vagrant vs. Terraform" gefragt wird** (beide von HashiCorp):

> „Vagrant ist für Entwicklungsumgebungen gedacht, Terraform für Infrastruktur, die nicht auf dem eigenen Laptop steht. Vagrant-VMs sind zum Wegwerfen, Terraform-Infrastruktur soll Bestand haben."

---

# SCHRITT 5 — Deployment Aufgabe 2: Live vorführen

## 5a) Hochfahren

```
cd C:\Projects\Testing-m450\7-cicd-pipelines\deployment-environment
docker compose up -d
docker compose ps
```

**Erwartetes Ergebnis:**

```
NAME                      STATUS                    PORTS
recipe-planner-backend    Up 27 seconds (healthy)   0.0.0.0:8080->8080/tcp
recipe-planner-frontend   Up 11 seconds             0.0.0.0:3000->3000/tcp
```

## 5b) Im Browser zeigen

- **Frontend:** http://localhost:3000 — *beim ersten Aufruf ~15 Sekunden, der Dev-Server kompiliert*
- **API:** http://localhost:8080/api/recipes — 15 Rezepte

**Das sagst du:**

> „Ich habe ein **Testing Environment** aufgesetzt. Nicht Development — das ist laut Theorie die Workstation des Entwicklers, und dort läuft es ohnehin direkt. Interessant ist die Stufe danach: eine Umgebung, die auf jedem Rechner identisch hochkommt, ohne dass vorher jemand JDK 17, Maven und Node 18 installiert."

## 5c) Der beste Punkt zum Erzählen

**Datei zeigen:** `deployment-environment\docker-compose.yml`, die Zeile `- "8080:8080"`

> „Port 8080 musste ich auf den Host veröffentlichen, obwohl beide Container im selben Docker-Netzwerk liegen. Grund: das Frontend hat die URL `http://localhost:8080` **fest im Code**. Dieser Aufruf passiert im Browser des Benutzers, nicht im Container — ein interner Name wie `http://backend:8080` würde dort ins Leere laufen. Und Port 3000 ist auch nicht frei wählbar, weil der Controller per `@CrossOrigin` genau diese Origin erlaubt."

> **Die Lehre: Containerisierbarkeit entscheidet sich im Anwendungscode, nicht im Compose-File.**

**Zweiter Punkt, wenn Zeit ist:**

> „Der Healthcheck war die wichtigste Zeile. `depends_on` allein wartet nur darauf, dass der Container *gestartet* ist — nicht darauf, dass Spring Boot *antwortet*. Mit `condition: service_healthy` startet das Frontend erst, wenn die API wirklich erreichbar ist."

**Dritter Punkt — ehrlich:**

> „Das Frontend-Image ist 1,4 GB gross, das Backend nur 537 MB. Ursache ist der React-Dev-Server mit dem ganzen `node_modules`-Baum. Für Produktion würde man `npm run build` machen und die statischen Dateien von einem nginx ausliefern. Genau solche Unterschiede zwischen Test- und Produktions-Image sind der Grund, warum es Staging gibt."

## 5d) Aufräumen

```
docker compose down
```

---

# THEORIE-SPICKZETTEL

Falls zwischendurch gefragt wird.

## Pipeline / Stage / Job / Runner

```
Pipeline          der ganze Ablauf, eine Datei
 └── Stage        eine Phase: build, test, deploy
      └── Job     ein Prozess darin
Runner            die Maschine, die es ausführt
```

**Stage vs. Job:** Jobs im selben Stage laufen **parallel**, Stages laufen **nacheinander**. Deshalb können Unit-Test und Lint gleichzeitig laufen, aber Deploy erst, wenn beide durch sind.

**Runner:** die Applikation, die Jobs ausführt — selbst gehostet oder geteilt beim Anbieter. Bei uns ein GitHub-gehosteter `ubuntu-latest`, der für jeden Lauf frisch hochkommt. Deshalb muss jeder Job das JDK neu einrichten, und deshalb braucht es Artefakte — **was nicht gesichert wird, ist nach dem Lauf weg.**

## Die vier Umgebungen

```
Development  →  Testing  →  Staging  →  Production
```

| Umgebung | Zweck |
|---|---|
| Development | Workstation des Entwicklers, Code schreiben und sofort ausprobieren |
| Testing | neuen Code prüfen, automatisiert, isoliert vom Entwicklerrechner |
| Staging | **Probe für den Ernstfall** — entspricht der Produktion möglichst genau |
| Production | live, die Benutzer |

**„Warum Staging, wenn es Testing gibt?"**

> „Testing prüft, ob die Software funktioniert. Staging prüft, ob das **Ausrollen** funktioniert — Installations-, Konfigurations- und Migrationsskripte. Deshalb muss Staging der Produktion entsprechen, inklusive echter Vernetzung. Lasttests gehören auch dorthin, weil die Ergebnisse stark von der Umgebung abhängen."

## Patch / Update / Upgrade

| | Was | Beispiel |
|---|---|---|
| **Patch** | behebt ein konkretes Problem, zeitnah, meist im laufenden Betrieb | Sicherheitslücke |
| **Update** | bringt auf den neuesten Stand, **ändert den Funktionsumfang nicht substantiell** | Windows-Update |
| **Upgrade** | neue Produktklasse, neue Funktionen, teils neue Struktur | Windows 10 → 11 |

Merksatz: **Patch repariert, Update aktualisiert, Upgrade verändert.**

---

# WAHRSCHEINLICHE FRAGEN

**„Was ist der Unterschied zwischen `@Mock` und `@MockBean`?"**
> `@Mock` ist ein nacktes Java-Objekt, Spring weiss nichts davon. `@MockBean` legt den Mock in den Spring-Kontext. Bei `@WebMvcTest` braucht es `@MockBean`, weil Spring die Objekte selbst verdrahtet. — *Nebenbei: `@MockitoBean` gibt es erst ab Boot 3.4, dieses Projekt läuft auf 3.2.1.*

**„Warum hast du das Repository nicht gemockt, den Service aber schon?"**
> Ein gemocktes Repository würde nur beweisen, dass Mockito funktioniert — Spring Data generiert die Implementierung ja selbst.

**„Warum ist die Coverage nicht 100 %?"**
> Die fehlenden 13 Instructions sind die leere Konfigurationsklasse `CommonMapperConfig` und der `main()`-Aufruf. Beides zu testen hätte keinen Wert.

**„Hast du Fehler im Vorgabecode gefunden?"**
> 15 Stück, alle in `uebung-recipe-planner.md` dokumentiert. Die drei besten:
> - `RecipeRepository extends CrudRepository<RecipeEntity, Long>` — der Schlüssel ist aber `UUID`
> - Eine unbekannte Id ergibt **200 mit leerem Body** statt 404
> - `spring.jpa.spring.jpa.database-platform` in der YAML ist doppelt verschachtelt, der Schlüssel greift nicht und wird still ignoriert

**„Warum gibt es keinen Datenbank-Container?"**
> Die Anwendung nutzt H2 in-memory im Backend-Prozess. Für eine Testumgebung ist das sogar erwünscht — nach `down` ist der Stand weg, jeder Lauf startet gleich.

**„Gab es Probleme?"**
> Zwei. Erstens waren beim ersten Lauf alle 14 Controller-Tests rot — `Failed to load ApplicationContext`. Ursache war nicht der Test, sondern das Projekt: die Seed-Daten hängen als `@Bean CommandLineRunner` in der Application-Klasse, und die ist zugleich die Spring-Konfiguration. Dadurch verlangt selbst ein reiner Web-Slice ein Repository. Gelöst mit einem zusätzlichen `@MockBean`, ohne Produktivcode zu ändern.
> Zweitens lief der Docker-Daemon nicht — das CLI war installiert, aber Docker Desktop war nicht gestartet. Ein installiertes CLI heisst nicht, dass die Engine läuft.

---

# CHECKLISTE — alles abgehakt?

- [ ] 42 Tests grün gezeigt
- [ ] Controller-Tests erklärt (`@WebMvcTest`, `@MockBean`)
- [ ] SoftAssertions-Nachweis gezeigt und den Vorteil erklärt
- [ ] Surefire-Ordner gezeigt
- [ ] JaCoCo-HTML geöffnet, 95,1 % gezeigt
- [ ] Die 38 %-Lombok-Geschichte erzählt
- [ ] Workflow-Datei gezeigt, GitLab→GitHub übersetzt
- [ ] Lauf #1 auf GitHub gezeigt: grün, 63 s, zwei Artefakte
- [ ] Werkzeugvergleich erklärt (zwei Achsen)
- [ ] Compose-Stack live hochgefahren, Frontend und API gezeigt
- [ ] Port-8080-Erklärung gebracht
- [ ] `docker compose down` gemacht

**Wenn alle Haken sitzen, sind beide Kapitel vollständig vorgezeigt.**
