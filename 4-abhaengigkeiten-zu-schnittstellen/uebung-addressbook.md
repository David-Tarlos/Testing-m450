# Übungen — addressbook-backend: Tests mit Test Doubles

> **Aufgabenstellung**
>
> * Bilden Sie zweier Gruppen
> * Nehmen Sie das addressbook-backend (Java Version 21) in Betrieb
>
> **Aufgabe 1**
> * Schreiben Sie Tests für alle Klassen
> * Schauen Sie, dass Sie auch zusätzliche Annotationen wie `@BeforeEach` benutzen
> * Fangen Sie an mit dem Testen von Adressen, welche Sie erstellen
> * Versuchen Sie den Service zu testen indem Sie die h2 Datenbank weg mocken
> * Implementieren Sie die Comparator Klasse korrekt
>
> **Aufgabe 2**
> * Erweitern Sie die Comparator Klasse, sodass nach zusätzlichen Attributen verglichen werden kann
> * Testen Sie entsprechend die neue Funktionalität

Quelle: [`Unterlagen/schnittstellen/addressbook-backend-v1-1.zip`](https://gitlab.com/ch-tbz-it/Stud/m450/m450/-/blob/main/Unterlagen/schnittstellen/addressbook-backend-v1-1.zip)
Das entpackte Projekt liegt unter [`addressbook-backend/`](addressbook-backend/).

---

## 1  Testobjekt und Abgrenzung

Getestet wird das Backend der Adressverwaltung — eine dreischichtige Spring-Boot-Anwendung:

| Klasse | Datei | Rolle |
|---|---|---|
| `AddressbookApplication` | `AddressbookApplication.java` | Einstiegspunkt |
| `Address` | `repository/Address.java` | JPA-Entity mit Lombok-Gettern/Settern |
| `AddressRepository` | `repository/AddressRepository.java` | `JpaRepository<Address, Integer>`, ohne eigene Methoden |
| `AddressComparator` | `util/AddressComparator.java` | Sortierlogik — **war fehlerhaft**, siehe Abschnitt 4 |
| `AddressService` | `service/AddressService.java` | Geschäftslogik zwischen Controller und Repository |
| `AddressController` | `controller/AddressController.java` | REST-Schnittstelle unter `/address` |

**Nicht getestet** wird bewusst: das Verhalten von Spring Data JPA selbst (`save`, `findById`, `findAll` sind
Framework-Code), die Korrektheit von Lombok-generiertem Bytecode über den Rahmen von `AddressTest` hinaus, sowie
alles Nicht-Funktionale (Antwortzeiten, Last, gleichzeitige Zugriffe).

**Warum Test Doubles hier überhaupt nötig sind:** `AddressService` hängt über `AddressRepository` an einer echten
Datenbank. Ohne Double müsste für jeden Service-Test H2 hochfahren — langsam, und ein Fehlschlag sagt nicht, ob der
Service oder die Datenbank schuld ist. Genau das Problem beschreibt die Einführung des Kapitels.

---

## 2  Setup und Ausführung

| | |
|---|---|
| Java | 21.0.5 (JetBrains Runtime, mit IntelliJ IDEA Community 2024.2.4 gebündelt) |
| Maven | Apache Maven 3.9.8 |
| Spring Boot | 3.5.4 |
| Test-Bibliotheken | JUnit Jupiter 5.13.4, Mockito 5.19.0, Spring Boot Test / MockMvc |
| Datenbank | H2 in-memory (`jdbc:h2:mem:mydb`) |

Die `pom.xml` musste **nicht** angepasst werden — `spring-boot-starter-test`, `mockito-core` und
`junit-jupiter-api` waren bereits deklariert.

```bash
cd 4-abhaengigkeiten-zu-schnittstellen/addressbook-backend

# Tests ausführen
./mvnw test

# Anwendung starten (REST unter http://localhost:8080/address)
./mvnw spring-boot:run
```

Unter Windows `mvnw.cmd` statt `./mvnw`.

---

## 3  Aufgabe 1 — Tests für alle Klassen

Sechs Testklassen unter `src/test/java/ch/tbz/m450/`, **50 Testfälle**. Jede Klasse nutzt `@BeforeEach`, um die
Testdaten vor jedem einzelnen Test frisch aufzubauen — so kann kein Test das Ergebnis eines anderen beeinflussen.

| Testklasse | Anzahl | Technik | Test Double | Was geprüft wird |
|---|---|---|---|---|
| `repository/AddressTest` | 4 | reines JUnit | keines | Entity und Lombok: `@AllArgsConstructor`, `@NoArgsConstructor`, Setter, fehlendes `equals()` |
| `util/AddressComparatorTest` | 21 | reines JUnit | keines | Comparator-Vertrag, Standardreihenfolge, Aufgabe 2, Randfälle |
| `service/AddressServiceTest` | 11 | Mockito | **Mock + Stub** | Geschäftslogik ohne Datenbank — Kernstück der Aufgabe |
| `controller/AddressControllerTest` | 6 | `@WebMvcTest` + MockMvc | **Mock** (`@MockitoBean`) | Routing, Statuscodes 201/200/404/400 |
| `repository/AddressRepositoryTest` | 7 | `@DataJpaTest` | **keines** (echte H2) | Entity-Mapping gegen eine reale Datenbank |
| `AddressbookApplicationTests` | 1 | `@SpringBootTest` | keines | Smoke-Test: startet der Kontext, ist alles verdrahtet |

### 3.1  Die h2-Datenbank wegmocken

Der ausdrücklich verlangte Teil der Aufgabe. `AddressService` bekommt sein Repository über den Konstruktor —
dadurch lässt es sich austauschen, ohne den Produktivcode anzufassen:

```java
@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;   // Attrappe statt echtem Repository

    @InjectMocks
    private AddressService addressService;         // bekommt die Attrappe über den Konstruktor
```

Es läuft dabei **weder Spring noch H2**. Messbar an den Laufzeiten aus dem Testlauf: die elf Service-Tests brauchen
zusammen `0.587 s`, der eine `@SpringBootTest` allein `13.10 s`.

Bewusst kommen beide Kategorien aus dem Kapitel vor:

**Stub — State Testing.** Feste Daten hineingeben, das Ergebnis prüfen. Der Stub liefert absichtlich *unsortiert*,
damit belegt ist, dass die Sortierung wirklich vom Service kommt:

```java
when(addressRepository.findAll()).thenReturn(List.of(meierAnna, meierBeat, aebiZoe));

List<Address> result = addressService.getAll();

assertEquals(List.of(2, 3, 1), result.stream().map(Address::getId).toList());
```

**Mock — Behavioral Testing.** Nicht das Ergebnis zählt, sondern *ob und wie* der Kollaborateur aufgerufen wurde:

```java
addressService.getAddress(42);

verify(addressRepository).findById(42);
verify(addressRepository, never()).findAll();   // lädt nicht die ganze Tabelle
```

Zusätzlich zeichnet ein `ArgumentCaptor` auf, was tatsächlich an das Repository übergeben wurde — die Rolle, die im
Kapitel dem **Spy** zugeschrieben wird:

```java
verify(addressRepository).save(savedAddress.capture());
assertEquals("Meier", savedAddress.getValue().getLastname());
```

### 3.2  Mocken auf der Controller-Ebene

Derselbe Gedanke eine Schicht höher: `@WebMvcTest` startet nur den Web-Layer, JPA und H2 bleiben aussen vor. Der
Service wird per `@MockitoBean` ersetzt, sodass ausschliesslich geprüft wird, was der Controller selbst leistet.

`@MockitoBean` löst das seit Spring Boot 3.4 veraltete `@MockBean` ab.

```java
@WebMvcTest(AddressController.class)
class AddressControllerTest {

    @MockitoBean
    private AddressService addressService;
```

Der interessanteste Fall ist das Mapping von `Optional.empty()` auf HTTP 404 (`AddressController.java:35-41`) —
Logik, die nur der Controller hat und die sich sonst nirgends prüfen lässt.

### 3.3  Wo bewusst *nicht* gemockt wird

`AddressRepositoryTest` läuft mit `@DataJpaTest` gegen die echte H2-Datenbank. Ein gemocktes Repository würde hier
nichts beweisen ausser, dass Mockito funktioniert. Erst der reale Lauf zeigt, ob das Entity-Mapping stimmt — und
genau dort ist Befund **E-01** aufgefallen.

### 3.4  Ergebnis des Testlaufs

Ausgeführt am **25.08.2026** mit `mvn -B clean test`:

```
[INFO] Tests run: 1,  Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 13.10 s -- in ch.tbz.m450.AddressbookApplicationTests
[INFO] Tests run: 6,  Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.764 s -- in ch.tbz.m450.controller.AddressControllerTest
[INFO] Tests run: 7,  Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.121 s -- in ch.tbz.m450.repository.AddressRepositoryTest
[INFO] Tests run: 4,  Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.021 s -- in ch.tbz.m450.repository.AddressTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.587 s -- in ch.tbz.m450.service.AddressServiceTest
[INFO] Tests run: 4,  Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.015 s -- in ch.tbz.m450.util.AddressComparatorTest$EdgeCaseTests
[INFO] Tests run: 8,  Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.025 s -- in ch.tbz.m450.util.AddressComparatorTest$ConfigurableSortFieldTests
[INFO] Tests run: 4,  Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.015 s -- in ch.tbz.m450.util.AddressComparatorTest$DefaultOrderTests
[INFO] Tests run: 5,  Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.022 s -- in ch.tbz.m450.util.AddressComparatorTest$ContractTests
[INFO] Results:
[INFO] Tests run: 50, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  39.792 s
```

**50 Tests · 0 Failures · 0 Errors.**

> **Ein Test schlug im ersten Durchgang fehl**, und zwar der zu Befund E-01: angenommen war, dass eine zweite Adresse
> ohne Id den ersten Datensatz still überschreibt. Tatsächlich wirft Hibernate eine `NonUniqueObjectException`, die
> Spring in eine `DuplicateKeyException` übersetzt. Der Test wurde auf das **gemessene** Verhalten korrigiert, nicht
> die Annahme beibehalten.

---

## 4  Aufgabe 1 — Comparator korrekt implementieren

### Der Fehler in der Vorgabe

```java
@Override
public int compare(Address a1, Address a2) {
    // Wrong implementation, please change me
    return -1;
}
```

Konstant `-1` verletzt den Vertrag von `java.util.Comparator` gleich zweifach:

| Regel | Gefordert | Vorgabe lieferte |
|---|---|---|
| Reflexivität | `compare(a, a) == 0` | `-1` |
| Antisymmetrie | `signum(compare(a,b)) == -signum(compare(b,a))` | beide `-1` |

**Gemessene Auswirkung** (eigener Probelauf gegen die unveränderte Vorgabe, vor der Korrektur):

```
compare(x,x) = -1
compare(a,b) = -1   compare(b,a) = -1
n=2, 5, 31, 32, 33, 100  ->  keine Exception, Liste kommt in umgekehrter Einfügereihenfolge zurück
```

Bemerkenswert: Es fliegt **keine** `IllegalArgumentException: Comparison method violates its general contract!` —
auch bei 100 Elementen nicht. TimSort erkennt die Vertragsverletzung in diesem Muster nicht. Der Fehler bleibt also
komplett still: `getAll()` liefert eine Reihenfolge, die aussieht wie eine Sortierung, aber die Daten nie anschaut.
Ein stiller Fehler ist der unangenehmere Fall — eine Exception wäre sofort aufgefallen.

### Die Korrektur

Sortiert wird nach **Nachname, dann Vorname, dann Id**. Die Id am Schluss garantiert, dass die Reihenfolge auch bei
Namensgleichheit eindeutig und reproduzierbar bleibt. `null`-Werte landen hinten, Text wird ohne Rücksicht auf
Gross-/Kleinschreibung verglichen.

Abgesichert durch fünf Vertragstests in `AddressComparatorTest.ContractTests`, darunter der Regressionstest
`sortingActuallyLooksAtTheData()`, der gezielt ausschliesst, dass das Ergebnis bloss die umgedrehte Eingabe ist.

---

## 5  Aufgabe 2 — Sortierung nach zusätzlichen Attributen

### Design

Der Comparator bekommt ein Enum mit allen sortierbaren Attributen und einen Varargs-Konstruktor:

```java
public enum SortField { ID, FIRSTNAME, LASTNAME, PHONENUMBER, REGISTRATION_DATE }

public AddressComparator()                      // Default: LASTNAME, FIRSTNAME, ID
public AddressComparator(SortField... fields)   // beliebige Felder, mehrstufig
```

Drei Überlegungen dahinter:

1. **Rückwärtskompatibel.** Der parameterlose Konstruktor bleibt erhalten, damit der bestehende Aufruf
   `new AddressComparator()` in `AddressService.java:26` unverändert weiterläuft.
2. **Mehrstufig statt einstufig.** `new AddressComparator(LASTNAME, ID)` sortiert nach Nachname und bei Gleichstand
   nach Id — mit einzelnen Comparator-Klassen pro Feld wäre das nicht ohne Zusatzcode möglich.
3. **Absteigend gratis.** Weil `AddressComparator` weiterhin `Comparator<Address>` implementiert, funktioniert
   `new AddressComparator(SortField.ID).reversed()` ohne eigene Implementierung.

Ungültige Konfigurationen werden früh abgewiesen: kein Feld oder ein `null`-Feld führt zu einer
`IllegalArgumentException` im Konstruktor statt zu einem Fehlverhalten beim Sortieren.

### Erreichbarkeit über den Service

Damit die neue Funktion nicht nur theoretisch existiert, gibt es eine Methode am Service:

```java
public List<Address> getAllSortedBy(AddressComparator.SortField... sortFields) {
    return addressRepository.findAll().stream().sorted(new AddressComparator(sortFields)).toList();
}
```

Bewusst mit eigenem Namen statt als Überladung von `getAll()` — ein argumentloser Aufruf `getAll()` würde sonst
zwischen beiden Methoden mehrdeutig aussehen (Java wählt zwar die parameterlose Variante, für Lesende ist es aber
eine Stolperstelle).

> Dies und der korrigierte Comparator sind die **einzigen** Änderungen am vorgegebenen Produktivcode.
> Alle weiteren Befunde in Abschnitt 6 wurden nur dokumentiert, nicht behoben.

### Testabdeckung

| Testgruppe | Anzahl | Inhalt |
|---|---|---|
| `ContractTests` | 5 | Reflexivität, Antisymmetrie, Transitivität, Regression gegen den alten Fehler, 100 Elemente |
| `DefaultOrderTests` | 4 | Nachname · Vorname als Tiebreaker · Id als letzter Tiebreaker · gemeldete Sortierfelder |
| `ConfigurableSortFieldTests` | 8 | jedes der fünf Felder einzeln, Mehrfachsortierung, `reversed()`, ungültige Konstruktorargumente |
| `EdgeCaseTests` | 4 | `null`-Feldwerte, `null`-Adressen, Gross-/Kleinschreibung, leere Liste |

---

## 6  Befunde am vorgegebenen Code

Nur belegte Beobachtungen, jede mit Zeilenverweis. Ausser dem Comparator wurde nichts davon geändert.

### Entity

| ID | Befund | Fundstelle | Warum es stört |
|---|---|---|---|
| E-01 | `@Id` ohne `@GeneratedValue` auf einem primitiven `int` | `Address.java:21-24` | Die Datenbank vergibt keine Id. Wer keine setzt, speichert auf Id 0; ein zweiter solcher Datensatz wird mit `DuplicateKeyException` abgewiesen. **Im Test belegt** (`AddressRepositoryTest.twoAddressesWithoutAnIdCollide`) |
| E-02 | Kein `equals()` / `hashCode()` | `Address.java:14-20` | Zwei inhaltsgleiche Adressen gelten als verschieden. Lombok bietet `@EqualsAndHashCode(of = "id")` dafür an. **Im Test belegt** (`AddressTest.twoIdenticalAddressesAreNotEqual`) |
| E-03 | Entity liegt im Package `repository` | `Address.java:1` | Vermischt Datenmodell und Datenzugriff; üblich wäre `model` oder `domain` |
| E-04 | `java.util.Date` statt `java.time.LocalDate` | `Address.java:12,28` | `Date` ist veränderbar und seit Java 8 abgelöst; ein Aufrufer kann das Datum eines gespeicherten Objekts nachträglich ändern |
| E-05 | Spaltenname `user_id` für das Feld `id` einer Adresse | `Address.java:22` | Irreführend — es ist kein Fremdschlüssel auf einen Benutzer |

### Service und Controller

| ID | Befund | Fundstelle | Warum es stört |
|---|---|---|---|
| S-01 | `new AddressComparator()` fest im Code | `AddressService.java:26` | Die Sortierstrategie lässt sich nicht von aussen austauschen und nicht durch ein Test Double ersetzen — genau die Art harter Abhängigkeit, um die es in diesem Kapitel geht |
| S-02 | Kein `update` und kein `delete` | `AddressService.java:13-41` | `JpaRepository` bietet beides; die API kann Adressen nur anlegen und lesen |
| C-01 | `@CrossOrigin("*")` | `AddressController.java:14` | Erlaubt Zugriffe von jeder beliebigen Herkunft — in Produktion eine offene Tür |
| C-02 | Keine Eingabevalidierung | `AddressController.java:25` | `@RequestBody Address` nimmt alles an: leere Namen, `null`-Felder, beliebige Ids. `@Valid` plus Bean-Validation-Annotationen fehlen |
| C-03 | Entity direkt als REST-Modell | `AddressController.java:25,29,35` | Datenbankstruktur und API-Vertrag sind gekoppelt; jede Feldumbenennung ist ein Breaking Change für Clients. Üblich wäre ein DTO |
| C-04 | Client bestimmt die Id beim Anlegen | `AddressController.java:25` | Zusammen mit E-01 kann ein Client fremde Datensätze überschreiben |

### Konfiguration

| ID | Befund | Fundstelle | Warum es stört |
|---|---|---|---|
| K-01 | Verschachtelter Schlüssel `spring.jpa.spring.jpa.database-platform` | `application.yaml:7-8` | Unter `spring: jpa:` steht nochmals der volle Pfad `spring.jpa.database-platform`. Der Eintrag greift nicht — er wird stillschweigend ignoriert. Dass trotzdem alles läuft, liegt nur an der automatischen Dialekterkennung von Hibernate |
| K-02 | Zugangsdaten im Klartext | `application.yaml:4-5` | Bei H2 in-memory harmlos, als Muster für eine echte Datenbank aber falsch |
| K-03 | H2-Konsole aktiviert | `application.yaml:9-10` | Offene Datenbank-Oberfläche; gehört höchstens ins Entwicklungsprofil |

---

## 7  Bezug zur Theorie des Kapitels

Welche Test-Double-Art in diesem Projekt wo vorkommt — und welche nicht:

| Art | In dieser Lösung | Wo |
|---|---|---|
| **Stub** | ja | `when(addressRepository.findAll()).thenReturn(...)` — feste Daten hinein, Ergebnis prüfen (State Testing) |
| **Mock** | ja | `verify(addressRepository).findById(42)`, `verify(..., never())` — prüft die Interaktion (Behavioral Testing) |
| **Spy** | sinngemäss | `ArgumentCaptor` zeichnet auf, was übergeben wurde, und lässt die Bewertung dem Test — dieselbe Rolle wie im Kapitel beschrieben. Ein echter `Mockito.spy()` auf einer Teilimplementierung war hier nicht nötig |
| **Dummy** | nein | Kein Konstruktor verlangt ein Objekt, das bloss vorhanden sein muss |
| **Fake** | nein | Wäre eine In-Memory-Implementierung von `AddressRepository`. Nicht nötig, weil H2 selbst schon eine In-Memory-Datenbank ist — H2 übernimmt hier faktisch die Rolle des Fake |

Der Kernsatz des Kapitels lässt sich am Projekt direkt zeigen: `AddressService` liess sich nur deshalb ohne
Datenbank testen, weil das Repository über den **Konstruktor** hereinkommt. Wäre es wie der Comparator mit `new`
fest verdrahtet (Befund S-01), gäbe es keine Stelle, an der ein Test Double eingesetzt werden könnte — dann bliebe
nur, für jeden Test die ganze Datenbank hochzufahren.
