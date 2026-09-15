# Übungen — recipe-planner: Unit Tests, Reports und Pipeline

> **Aufgabenstellung**
>
> Arbeiten Sie zu zweit an diesen Aufgaben. Nehmen Sie das
> [Frontend sowie das Backend](https://gitlab.com/ch-tbz-it/Stud/m450/m450/-/blob/main/Unterlagen/projects/recipe-planner-fronend-and-backend.zip)
> in Betrieb. Für die folgenden Aufgaben werden nur changes im Backend benötigt.
>
> **Aufgabe 1 — Unit Testing**
> 1. Testen Sie alle Controller Methoden via MockMvc, RestTemplate, REST-assured oder einer
>    alternativen Methode
> 2. Testen Sie die Mapper Klasse für die zwei vorhandenen Domänen Klassen und benutzen Sie dazu
>    **SoftAssertions** in ihren Tests. Sind Sie sich bewusst, was der Vorteil von SoftAssertions sind.
>
> **Aufgabe 2 — Reports**
> * Es sollen sichtbare Reports für die Unit Tests automatisiert erstellt werden
> * Dafür können Sie etwas wie Surefire, JaCoCo oder Alternativen verwenden
>
> **Aufgabe 3 — Pipeline**
> * Eine Build-Pipeline aufsetzen; ein Push soll die Pipeline und damit die Unit Tests triggern
> * Ein Report soll pro Pipeline-Durchlauf generiert werden und einsehbar sein

Das entpackte Projekt liegt unter [`recipe-planner/`](recipe-planner/).
Theorie: [Automatisiertes Testen und Deployen](README.md).

<!-- TOC -->
- [0 Testobjekt und Setup](#0--testobjekt-und-setup)
- [1 Aufgabe 1 — Unit Testing](#1--aufgabe-1--unit-testing)
  - [1.1 Controller-Tests mit MockMvc](#11--controller-tests-mit-mockmvc)
  - [1.2 Mapper-Tests mit SoftAssertions](#12--mapper-tests-mit-softassertions)
  - [1.3 Service-Tests](#13--service-tests)
  - [1.4 Ergebnis des Testlaufs](#14--ergebnis-des-testlaufs)
- [2 Aufgabe 2 — Reports](#2--aufgabe-2--reports)
- [3 Aufgabe 3 — Pipeline](#3--aufgabe-3--pipeline)
- [4 Befunde am vorgegebenen Code](#4--befunde-am-vorgegebenen-code)
<!-- TOC -->

---

## 0  Testobjekt und Setup

Der **recipe-planner** ist eine dreischichtige Spring-Boot-Anwendung mit React-Frontend.
Fachlich kann sie Rezepte auflisten, ein einzelnes Rezept laden und ein neues anlegen. Ein Rezept
besteht aus `id`, `name`, `description`, `imageUrl` und einer Liste von Zutaten.

| Klasse | Paket | Rolle |
|---|---|---|
| `RecipePlannerApplication` | `ch.tbz.recipe.planner` | Einstiegspunkt, legt beim Start 15 Rezepte an |
| `RecipeController` | `.controller` | REST: `GET /api/recipes`, `GET /api/recipes/recipe/{id}`, `POST /api/recipes` |
| `RecipeService` | `.service` | Geschäftslogik, ruft Repository und Mapper |
| `RecipeRepository` | `.repository` | `CrudRepository`, zusätzlich `findById(UUID)` und `findAll()` |
| `Recipe`, `Ingredient`, `Unit` | `.domain` | Die **zwei Domänenklassen** plus Einheiten-Enum |
| `RecipeEntity`, `IngredientEntity` | `.entities` | JPA-Entities |
| `RecipeEntityMapper`, `IngredientEntityMapper` | `.mapper` | **MapStruct**-Interfaces, Implementierung wird beim Kompilieren generiert |

**Nicht getestet** wird bewusst: Spring Data JPA und Hibernate selbst, der von MapStruct generierte
Code als solcher (geprüft wird sein *Verhalten*, nicht sein Quelltext), Lombok, und das
React-Frontend — die Aufgabe verlangt ausdrücklich nur Änderungen im Backend.

### Setup

| | |
|---|---|
| Spring Boot | 3.2.1 |
| Java | Kompiliert gegen **Release 17**, ausgeführt mit JDK 21.0.5 (JetBrains Runtime) |
| Maven | Apache Maven 3.9.8 |
| MapStruct | 1.5.5.Final |
| Test-Bibliotheken | JUnit Jupiter, Mockito, AssertJ, MockMvc (alle über `spring-boot-starter-test`) |
| Coverage | JaCoCo 0.8.12 — **neu ergänzt**, siehe Aufgabe 2 |
| Datenbank | H2 in-memory |

```bash
cd 7-cicd-pipelines/recipe-planner/recipe-planner-backend

mvn test                    # Tests + Coverage-Report
mvn spring-boot:run         # Backend auf http://localhost:8080
```

Für AssertJ und `SoftAssertions` musste **keine** Abhängigkeit ergänzt werden — beides kommt mit
`spring-boot-starter-test`.

---

## 1  Aufgabe 1 — Unit Testing

Vier Testklassen, **42 Testfälle**. Alle nutzen `@BeforeEach`, damit jeder Test mit frischen
Objekten startet und kein Test das Ergebnis eines anderen beeinflussen kann.

| Testklasse | Anzahl | Technik | Test Double |
|---|---|---|---|
| `controller/RecipeControllerTest` | 14 | `@WebMvcTest` + MockMvc | `@MockBean` für Service, Mapper, Repository |
| `mapper/RecipeEntityMapperTest` | 10 | reines JUnit + **SoftAssertions** | keines |
| `mapper/IngredientEntityMapperTest` | 11 | reines JUnit + **SoftAssertions** | keines |
| `service/RecipeServiceTest` | 7 | Mockito | `@Mock` für das Repository |

### 1.1  Controller-Tests mit MockMvc

`@WebMvcTest` startet nur den Web-Layer — kein JPA, keine H2, kein Kontextstart der ganzen
Anwendung. Der Service wird durch ein Test Double ersetzt, sodass ausschliesslich geprüft wird, was
der Controller selbst leistet: Routing, Deserialisierung, Statuscodes, Serialisierung.

```java
@WebMvcTest(RecipeController.class)
class RecipeControllerTest {

    @MockBean private RecipeService service;
    @MockBean private RecipeEntityMapper mapper;
```

> **Annotationswahl:** Dieses Projekt läuft auf Spring Boot 3.2.1. Das neuere `@MockitoBean` gibt es
> erst ab Boot 3.4 / Spring Framework 6.2 — hier ist `@MockBean` korrekt und noch nicht deprecated.
> (In [Kapitel 4](../4-abhaengigkeiten-zu-schnittstellen/uebung-addressbook.md) lief das Projekt auf
> Boot 3.5.4, dort war `@MockitoBean` richtig.)

Alle drei Controller-Methoden sind abgedeckt, jeweils mit Happy Path **und** Fehlerpfaden:

| Methode | Geprüft |
|---|---|
| `GET /api/recipes` | 200 mit Liste · leeres Array · JSON-Contract (alle fünf Felder) |
| `GET /api/recipes/recipe/{id}` | 200 mit Rezept · Id wird durchgereicht und nicht alles geladen · unbekannte Id · nicht-UUID-förmige Id → 400 |
| `POST /api/recipes` | 200 mit angelegtem Rezept · Request-Body kommt vollständig an (ArgumentCaptor) · leeres Rezept `{}` · kaputtes JSON → 400 · falscher Content-Type → 415 |
| übergreifend | unbekannte Route → 404 · `DELETE` → 405 · CORS erlaubt `localhost:3000`, verbietet fremde Origin |

### 1.2  Mapper-Tests mit SoftAssertions

Getestet wird die von MapStruct generierte Implementierung (`RecipeEntityMapperImpl`,
`IngredientEntityMapperImpl`). Sie hat keine Abhängigkeiten und lässt sich direkt instanziieren —
es braucht weder Spring noch Mockito.

```java
Recipe ergebnis = mapper.entityToDomain(entity);

SoftAssertions softly = new SoftAssertions();
softly.assertThat(ergebnis.getId()).as("id").isEqualTo(REZEPT_ID);
softly.assertThat(ergebnis.getName()).as("name").isEqualTo("Lasagne al Forno");
softly.assertThat(ergebnis.getDescription()).as("description").isEqualTo("Mit Bechamel");
softly.assertThat(ergebnis.getImageUrl()).as("imageUrl").isEqualTo("https://example.test/lasagne.jpg");
softly.assertThat(ergebnis.getIngredients()).as("ingredients").hasSize(1);
softly.assertAll();
```

#### Der Vorteil von SoftAssertions — belegt statt behauptet

Die Aufgabe sagt: *„Sind Sie sich bewusst, was der Vorteil von SoftAssertions sind."* Statt ihn nur
hinzuschreiben, haben wir ihn gemessen.

Mit gewöhnlichen Assertions bricht der Test bei der **ersten** Abweichung ab. Bei einem Mapper mit
fünf Feldern heisst das: reparieren, laufen lassen, nächster Fehler, reparieren, laufen lassen —
fünf Durchläufe für fünf Fehler. `SoftAssertions` sammelt stattdessen alle Abweichungen und meldet
sie gemeinsam.

Ein Lauf mit drei absichtlich falschen Feldern (`id`, `name`, `imageUrl` falsch, `description`
korrekt) ergibt genau eine Fehlermeldung mit allen drei Befunden:

```text
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

Drei Dinge sind daran bemerkenswert:

1. **Alle drei Fehler auf einmal** — eine Fehlersuche statt drei.
2. **Das korrekte Feld taucht nicht auf.** `description` war richtig und wird nicht erwähnt.
3. **Die `as()`-Beschriftung macht den Unterschied.** Ohne `.as("id")` stünde dort nur der Wert.
   Bei fünf UUID-Feldern wäre sonst nicht erkennbar, welches gemeint ist.

Dieser Nachweis ist als **grüner** Test in der Suite verankert
(`RecipeEntityMapperTest.SoftAssertionsNachweis`): das Ganze läuft in `assertThatThrownBy`, sodass
der erwartete Fehlschlag geprüft wird, ohne die Suite rot zu machen.

Abgedeckt sind für **beide** Domänenklassen: alle Felder in beide Richtungen, die verschachtelte
Zutat innerhalb eines Rezepts, `null`-Behandlung, leere Listen, Listenreihenfolge, jede Konstante
des `Unit`-Enums (als `@ParameterizedTest`) und der verlustfreie Hin- und Rückweg.

### 1.3  Service-Tests

Über die Aufgabenstellung hinaus, aber für Aufgabe 2 nötig: ohne diese Tests stünde die
Service-Schicht im Coverage-Report bei 0 %. Das Repository wird gemockt, der Mapper **nicht** — die
generierte Implementierung ist reine Kopierlogik, und mit einem Mapper-Mock würde der Test nur
beweisen, dass Mockito funktioniert.

### 1.4  Ergebnis des Testlaufs

Ausgeführt am **15.09.2026** mit `mvn -B clean test`:

```text
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0 -- in ch.tbz.recipe.planner.controller.RecipeControllerTest
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0 -- in ch.tbz.recipe.planner.mapper.IngredientEntityMapperTest
Tests run:  5, Failures: 0, Errors: 0, Skipped: 0 -- in ch.tbz.recipe.planner.mapper.RecipeEntityMapperTest$EntityToDomain
Tests run:  2, Failures: 0, Errors: 0, Skipped: 0 -- in ch.tbz.recipe.planner.mapper.RecipeEntityMapperTest$DomainToEntity
Tests run:  2, Failures: 0, Errors: 0, Skipped: 0 -- in ch.tbz.recipe.planner.mapper.RecipeEntityMapperTest$RoundTrip
Tests run:  1, Failures: 0, Errors: 0, Skipped: 0 -- in ch.tbz.recipe.planner.mapper.RecipeEntityMapperTest$SoftAssertionsNachweis
Tests run:  7, Failures: 0, Errors: 0, Skipped: 0 -- in ch.tbz.recipe.planner.service.RecipeServiceTest

Tests run: 42, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

> **Der erste Lauf war rot**, und zwar alle 14 Controller-Tests mit
> `IllegalStateException: Failed to load ApplicationContext`. Ursache war nicht der Test, sondern
> ein Befund am Projekt: `RecipePlannerApplication` deklariert die Seed-Daten als
> `@Bean CommandLineRunner init(RecipeRepository)`. Die Application-Klasse ist zugleich die
> Spring-Konfiguration und wird auch im `@WebMvcTest`-Slice ausgewertet — der Kontext verlangte
> daher ein `RecipeRepository`, das es im Web-Slice gar nicht gibt
> (`No qualifying bean of type RecipeRepository available`). Gelöst mit einem zusätzlichen
> `@MockBean RecipeRepository` im Test, statt Produktivcode zu ändern. Siehe Befund **A-01**.

---

## 2  Aufgabe 2 — Reports

Zwei Reports, beide an den normalen Maven-Lebenszyklus gebunden — `mvn test` genügt, es braucht
keinen Zusatzbefehl.

| Report | Werkzeug | Wo | Was drin steht |
|---|---|---|---|
| Testresultate | **Surefire** (läuft bei Maven ohnehin) | `target/surefire-reports/` | Pro Testklasse eine `.txt` zum Lesen und eine `.xml` für Werkzeuge |
| Code Coverage | **JaCoCo 0.8.12** (neu ergänzt) | `target/site/jacoco/index.html` | Abdeckung pro Paket, Klasse und Zeile, zusätzlich als `.csv` und `.xml` |

Die JaCoCo-Konfiguration in der `pom.xml`:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>${jacoco.version}</version>
    <executions>
        <execution>
            <id>jacoco-agent-anhaengen</id>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>jacoco-report-erzeugen</id>
            <phase>test</phase>
            <goals><goal>report</goal></goals>
        </execution>
    </executions>
    <configuration>
        <excludes>
            <exclude>**/*MapperImpl.class</exclude>
        </excludes>
    </configuration>
</plugin>
```

`prepare-agent` hängt den JaCoCo-Agenten an die Test-JVM, `report` erzeugt daraus die HTML-Seite.
Ausgeschlossen ist der von MapStruct generierte Code — die handgeschriebenen Mapper-Interfaces
bleiben in der Messung.

### Die Coverage-Zahl war zuerst falsch — und das ist der lehrreiche Teil

Der erste Report meldete **38,0 %**, obwohl Controller und Service je 100 % hatten:

| Klasse | Coverage (erster Lauf) |
|---|---|
| `RecipeController` | 100,0 % |
| `RecipeService` | 100,0 % |
| `Unit` | 100,0 % |
| `RecipePlannerApplication` | 92,9 % |
| `Recipe` / `RecipeEntity` | 22,0 % |
| `Ingredient` / `IngredientEntity` | 23,5 % |
| **Gesamt** | **38,0 %** (476 von 1251 Instructions) |

Auffällig war die Branch-Coverage: **0 von 172**. Bei Klassen, die ausser Gettern und Settern nichts
enthalten, kann es gar keine 172 Verzweigungen geben.

Die Erklärung: **Lombok.** `@Data` erzeugt `equals()`, `hashCode()`, `toString()` und `canEqual()`.
Diese Methoden landen als echter Bytecode in der `.class`-Datei, JaCoCo zählt sie mit — und die 172
Verzweigungen sind die Feldvergleiche im generierten `equals()`. Sie zu testen hätte keinen Wert:
es ist Code, den niemand geschrieben hat.

Die Standardlösung ist eine `lombok.config` neben der `pom.xml`:

```properties
lombok.addLombokGeneratedAnnotation = true
```

Lombok markiert seinen erzeugten Code dann mit `@lombok.Generated`, und JaCoCo überspringt ihn
automatisch. Ergebnis mit **denselben Tests und demselben Produktivcode**:

| | vorher | nachher |
|---|---|---|
| Instruction Coverage | 38,0 % (476/1251) | **95,1 %** (252/265) |
| Branch Coverage | 0 von 172 | keine Branches mehr in der Messung |

Die verbleibenden 13 nicht abgedeckten Instructions sind die leere Konfigurationsklasse
`CommonMapperConfig` (0 %) und der `main()`-Aufruf in `RecipePlannerApplication` (92,9 %).

**Die Lehre:** Eine Coverage-Zahl ist erst aussagekräftig, wenn klar ist, *was* gemessen wird.
38 % und 95 % beschreiben hier exakt denselben Testumfang. Wer die Zahl als Zielvorgabe benutzt,
ohne den generierten Code auszuschliessen, optimiert am Ende Tests für Lombok-Methoden.

---

## 3  Aufgabe 3 — Pipeline

Umgesetzt mit **GitHub Actions** statt GitLab CI: das Repository liegt auf GitHub, und die Theorie
lässt das ausdrücklich zu (*„Sie können alternativ auch mit GitHub arbeiten"*). Damit läuft die
Pipeline ohne Spiegel-Repository und ohne zweites Konto.

Datei: [`.github/workflows/recipe-planner.yml`](../.github/workflows/recipe-planner.yml) — sie muss
im Wurzelverzeichnis des Repositories liegen, genau wie die `.gitlab-ci.yml` im Kapitel.

### Übersetzung der Kapitelbegriffe

| GitLab CI (Theorie) | GitHub Actions (unsere Umsetzung) |
|---|---|
| `pipeline` | `workflow` — die ganze Datei |
| `stages:` mit Reihenfolge | Jobs mit `needs:` — `test` startet erst, wenn `build` erfolgreich war |
| `job` | `job` |
| `image: maven:latest` | `runs-on: ubuntu-latest` + `actions/setup-java` |
| `variables:` | `env:` |
| `script:` | `steps[].run` |
| `artifacts:` | `actions/upload-artifact` |
| `pages` für Reports | Artefakte am Durchlauf + Job-Summary |
| Shared Runner | GitHub-hosted Runner |

### Aufbau

```
push / pull_request / manuell
        │
        ▼
   ┌─────────┐        ┌────────────────────────────────────┐
   │  build  │ ─────► │              test                  │
   │ compile │ needs  │  mvn test                          │
   └─────────┘        │  → Zusammenfassung ins Job-Summary │
                      │  → Surefire-Report als Artefakt    │
                      │  → JaCoCo-Report als Artefakt      │
                      └────────────────────────────────────┘
```

Drei Entscheidungen, die im Workflow stecken:

**Pfadfilter.** Der Workflow startet nur, wenn sich am Backend oder an der Workflow-Datei selbst
etwas ändert. Ohne das würde jeder Markdown-Commit im Repo einen Maven-Build auslösen — bei einem
Schul-Repo mit neun Kapiteln wäre das reine Verschwendung.

**Maven-Cache.** `cache: maven` in `setup-java` behält `~/.m2` zwischen den Läufen. Der erste
Durchlauf lädt Spring Boot komplett herunter, die folgenden nicht mehr.

**`if: always()` bei den Reports.** Ein Report ist genau dann am wertvollsten, wenn die Tests
fehlgeschlagen sind. Ohne diese Bedingung würde der Upload-Schritt nach einem roten Test
übersprungen — und man hätte keine Ausgabe, um den Fehler zu verstehen.

### Report pro Durchlauf

Die Aufgabe verlangt: *„Ein Report soll pro Pipeline-Durchlauf generiert werden und einsehbar sein."*
Umgesetzt auf zwei Wegen:

1. **Job-Summary** — direkt auf der Seite des Durchlaufs, ohne Download: eine Tabelle mit
   Tests / Failures / Errors / Skipped sowie die Instruction Coverage in Prozent. Erzeugt aus den
   Surefire-XML-Dateien und aus `jacoco.csv`, ohne fremde Actions.
2. **Artefakte** — `surefire-report` und `jacoco-coverage-report` zum Herunterladen, 30 Tage
   aufbewahrt. Der JaCoCo-Ordner enthält die vollständige HTML-Seite.

### Status

> Der Workflow ist geschrieben und die YAML-Syntax ist geprüft (als YAML geparst, Jobs `build` und
> `test` werden korrekt erkannt). **Ein Durchlauf auf GitHub hat noch nicht stattgefunden**, weil
> der Commit noch nicht gepusht ist. Sobald gepusht wurde, wird das Ergebnis hier mit der
> tatsächlichen Laufnummer und dem tatsächlichen Status ergänzt — bis dahin steht hier bewusst
> keine Erfolgsmeldung.

---

## 4  Befunde am vorgegebenen Code

Nur belegte Beobachtungen, jede mit Fundstelle. **Am Produktivcode wurde nichts geändert** — die
einzigen Ergänzungen sind das JaCoCo-Plugin und die `lombok.config` für Aufgabe 2 sowie die
Dockerfiles für das Deployment-Kapitel.

| ID | Befund | Fundstelle | Warum es stört |
|---|---|---|---|
| A-01 | Seed-Daten als `@Bean CommandLineRunner init(RecipeRepository)` in der Application-Klasse | `RecipePlannerApplication.java:39-56` | Die Application-Klasse ist zugleich Spring-Konfiguration. Jeder Test-Slice — auch ein reiner `@WebMvcTest` — muss deshalb ein Repository bereitstellen. **Im Test belegt**: ohne `@MockBean RecipeRepository` scheitert der Kontextstart |
| A-02 | 15 Rezepte werden bei **jedem** Start fest angelegt, mit `Math.random()`-Bildern | `RecipePlannerApplication.java:44-54` | Testdaten im Produktivcode, ohne Profil-Bedingung. In einer echten Umgebung würde das die Datenbank bei jedem Neustart befüllen |
| R-01 | `RecipeRepository extends CrudRepository<RecipeEntity, Long>`, der Schlüssel ist aber `UUID` | `RecipeRepository.java:12` | Der generische Typ ist schlicht falsch. Die geerbten Methoden (`findById(Long)`, `deleteById(Long)`) passen nicht zur Entity; deshalb mussten `findById(UUID)` und `findAll()` von Hand nachdeklariert werden |
| C-01 | Unbekannte Id ergibt **200 mit leerem Body** statt 404 | `RecipeController.java:34-37` | Der Service liefert `null`, der Controller verpackt das unbesehen in `ResponseEntity<>(null, HttpStatus.OK)`. Ein Client kann „nicht gefunden" nicht von „gefunden, aber leer" unterscheiden. **Im Test belegt** |
| C-02 | `POST` antwortet mit **200** statt `201 Created` | `RecipeController.java:39-42` | REST-üblich wäre 201 mit `Location`-Header. **Im Test belegt** |
| C-03 | Keine Eingabevalidierung | `RecipeController.java:40` | `@RequestBody Recipe` nimmt alles an — auch `{}`. Es gibt kein `@Valid` und keine Bean-Validation-Annotationen. **Im Test belegt** |
| C-04 | Der injizierte `RecipeEntityMapper` wird nie benutzt | `RecipeController.java:22-26` | Ein Feld, das gesetzt, aber nirgends gelesen wird. Der Controller braucht den Mapper nicht — der Service macht die Abbildung bereits |
| S-01 | `getRecipeById()` liefert `null` statt `Optional.empty()` | `RecipeService.java:29-31` | `orElse(null)` wirft die Information weg, die `Optional` gerade transportieren soll. Ursache von C-01. **Im Test belegt** |
| S-02 | Der Client bestimmt die Id beim Anlegen | `RecipeService.java:33-36` | Die mitgeschickte `id` wird ungeprüft übernommen. Zusammen mit `save()` als „insert or update" kann ein Aufrufer fremde Rezepte überschreiben. **Im Test belegt** |
| M-01 | `null`-Zutatenliste bleibt `null` | MapStruct-Standardverhalten, `RecipeEntityMapper.java:10` | Wer die Liste ungeprüft durchläuft, bekommt eine `NullPointerException`. `nullValueMappingStrategy = RETURN_DEFAULT` würde eine leere Liste liefern. **Im Test belegt** |
| M-02 | `amount` ist ein primitives `int` | `Ingredient.java:18`, `IngredientEntity.java:30` | Eine fehlende Menge wird zu `0` und ist nicht von einer bewusst eingetragenen `0` zu unterscheiden. **Im Test belegt** |
| P-01 | `maven.compiler.source/target` steht auf **20**, kompiliert wird gegen **17** | `pom.xml:18-19` vs. `pom.xml:78-79` | Die Properties werden von der expliziten Plugin-Konfiguration `<source>${java.version}</source>` überstimmt (`java.version` = 17 aus dem Boot-Parent). Der Build meldet `release 17`. Zwei widersprechende Angaben in derselben Datei |
| K-01 | Verschachtelter Schlüssel `spring.jpa.spring.jpa.database-platform` | `application.yaml:8` | Unter `spring: jpa:` steht nochmals der volle Pfad. Der Eintrag greift nicht und wird stillschweigend ignoriert; dass es trotzdem läuft, liegt an Hibernates automatischer Dialekterkennung. *(Derselbe Fehler wie im addressbook aus Kapitel 4.)* |
| K-02 | Zugangsdaten im Klartext, H2-Konsole aktiviert | `application.yaml:4-5, 9-10` | Bei H2 in-memory harmlos, als Muster für eine echte Datenbank falsch |
| F-01 | Die Backend-URL steht fest im Frontend | `Browse.js:7` | `const baseURL = "http://localhost:8080/api/recipes";` — nicht konfigurierbar. Hat direkte Folgen fürs Containerisieren, siehe [Deployment-Kapitel](deployment-environment/uebung-environments.md) |

**Keiner dieser Befunde ist schwerwiegend** im Sinne der Klassifikation aus
[Kapitel 6](../6-testkonzept-vertieft/testkonzept.md#6--kriterien-für-erfolgreiche-und-nicht-erfolgreiche-tests) —
die Anwendung stürzt in keinem Fall ab. C-01, S-01 und R-01 sind mittelschwer, der Rest geringfügig.
