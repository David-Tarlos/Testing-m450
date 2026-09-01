# Addressbook Backend – Unit Testing (M450)

Dokumentation zu Aufgabe 1 und 2: JUnit-5-Tests für alle Klassen, Service-Test mit
gemockter Datenbank und ein korrekt implementierter `AddressComparator`.

## Inbetriebnahme

Voraussetzung: JDK 21 und Maven.

```bash
mvn test              # alle Tests
mvn spring-boot:run   # App auf http://localhost:8080
```

Die H2-Konsole liegt auf `/h2-console` (JDBC-URL `jdbc:h2:mem:mydb`, User `sa`).

Kurzer Smoke-Test bei laufender App:

```bash
curl -X POST localhost:8080/address -H "Content-Type: application/json" \
  -d '{"id":1,"firstname":"Bert","lastname":"Beispiel","phonenumber":"044 100 00 00"}'
curl localhost:8080/address
curl "localhost:8080/address?sortBy=FIRSTNAME&direction=DESC"
```

## Was wir am produktiven Code geändert haben

### `AddressComparator` (Aufgabe 1)

Vorher stand dort `return -1;` – damit war der Comparator nicht symmetrisch und
`stream().sorted(...)` hat je nach Eingabe irgendeine Reihenfolge geliefert.

Jetzt gilt:

| Regel | Verhalten |
|---|---|
| Standard | Nachname, bei Gleichstand Vorname, aufsteigend |
| Text-Vergleich | case-insensitive (`"anna"` vor `"Bert"`, `"Muster" == "muster"`) |
| `null`-Werte | landen bei `ASC` am Ende |
| Gleichstand | die `id` entscheidet, damit die Reihenfolge deterministisch bleibt |

### `AddressComparator` erweitern (Aufgabe 2)

Zwei Enums steuern den Vergleich:

```java
new AddressComparator();                                            // LASTNAME, ASC
new AddressComparator(SortField.REGISTRATION_DATE);                 // ASC
new AddressComparator(SortField.PHONENUMBER, SortDirection.DESC);
```

`SortField`: `LASTNAME`, `FIRSTNAME`, `PHONENUMBER`, `REGISTRATION_DATE`, `ID`
`SortDirection`: `ASC`, `DESC`

Intern baut der Konstruktor einmal einen `Comparator<Address>` zusammen
(`Comparator.comparing(...).thenComparing(...)`), `compare()` hängt nur noch den
Tiebreak und ggf. die Umkehrung für `DESC` dran. `DESC` dreht das *gesamte*
Ergebnis um – also auch die Position der `null`-Werte und den Tiebreak.

Der parameterlose Konstruktor verhält sich exakt wie vorher gedacht, deshalb muss
am bestehenden Aufruf im Service nichts angepasst werden.

### Service und Controller

Damit die neue Funktionalität auch von aussen nutzbar ist:

* `AddressService.getAll(SortField, SortDirection)` als Überladung; das alte
  `getAll()` delegiert darauf mit `LASTNAME/ASC`.
* `GET /address?sortBy=FIRSTNAME&direction=DESC` – beide Parameter optional, die
  Defaults entsprechen dem bisherigen Verhalten. Die Werte sind die Enum-Namen in
  Grossbuchstaben; alles andere quittiert Spring mit `400`.

### `pom.xml`

`mockito-core` und `junit-jupiter-api` waren mit fixen Versionen eingetragen, die
nicht zu den vom Spring-Boot-Parent verwalteten Versionen passten. Wir lassen die
Versionen jetzt vom Parent verwalten und haben `mockito-junit-jupiter` ergänzt
(für `@ExtendWith(MockitoExtension.class)`). Surefire bekommt zusätzlich
`-XX:+EnableDynamicAgentLoading`, sonst warnt das JDK 21 bei jedem Testlauf über
Mockitos Agent.

## Die Tests

46 Tests, alle grün. Aufteilung nach Klasse:

| Testklasse | Tests | Was getestet wird | DB |
|---|---|---|---|
| `AddressTest` | 3 | Entity: Konstruktoren, Getter/Setter (Lombok) | – |
| `AddressComparatorTest` | 24 | Standardsortierung + alle Sortierfelder/-richtungen | – |
| `AddressServiceTest` | 8 | Service-Logik, Repository gemockt | gemockt |
| `AddressControllerTest` | 6 | HTTP-Schicht via MockMvc, Service gemockt | – |
| `AddressRepositoryTest` | 4 | Mapping und Persistenz | echtes H2 |
| `AddressbookApplicationTests` | 1 | Context startet, Beans sind verdrahtet | echtes H2 |

### Aufbau mit `@BeforeEach`

Jede Testklasse baut ihre Fixture in `@BeforeEach` neu auf, damit sich die Tests
nicht gegenseitig beeinflussen. Die drei Beispiel-Adressen sind bewusst so
gewählt, dass **jedes Attribut eine andere Reihenfolge ergibt** – sonst würde ein
Test auch dann grün, wenn nach dem falschen Feld sortiert wird:

| | id | Vorname | Nachname | Telefon | Registriert |
|---|---|---|---|---|---|
| anna | 3 | Anna | Muster | 079 300 00 00 | 2023 |
| bert | 1 | Bert | Beispiel | 044 100 00 00 | 2021 |
| clara | 2 | clara | muster | 062 200 00 00 | 2022 |

`Muster` vs. `muster` und `clara` klein geschrieben decken zusätzlich die
Gross-/Kleinschreibung ab.

### Verwendete Annotationen

* `@BeforeEach` – Fixture pro Test frisch aufbauen
* `@Nested` – trennt im Comparator-Test Aufgabe 1 von Aufgabe 2
* `@DisplayName` – lesbare Namen im Testreport
* `@ParameterizedTest` mit `@MethodSource` / `@EnumSource` – ein Test deckt alle
  fünf Sortierfelder ab, statt fünf fast identischer Methoden
* `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks` – Mocks
* `@DataJpaTest`, `@SpringBootTest` – die beiden Tests, die H2 wirklich brauchen

### Die H2-Datenbank wegmocken

`AddressServiceTest` startet weder Spring noch H2. Das Repository ist ein
Mockito-Mock, der Service wird direkt instanziert:

```java
@ExtendWith(MockitoExtension.class)
class AddressServiceTest {
    @Mock private AddressRepository addressRepository;
    @InjectMocks private AddressService addressService;

    @Test
    void getAllSortsByLastname() {
        when(addressRepository.findAll()).thenReturn(List.of(anna, clara, bert));

        List<Address> result = addressService.getAll();

        assertThat(result).extracting(Address::getId).containsExactly(1, 3, 2);
        verify(addressRepository).findAll();
    }
}
```

Der Trick: das Mock gibt die Adressen **unsortiert** zurück. Käme die Sortierung
in Wahrheit von der Datenbank (`ORDER BY`), würde der Test auffliegen. So testen
wir wirklich den Service und nicht H2 – und der ganze Test läuft in Millisekunden
statt Sekunden.

Warum trotzdem noch ein `@DataJpaTest`: das Mock würde auch dann grün bleiben,
wenn das JPA-Mapping kaputt wäre. `AddressRepositoryTest` deckt genau die Lücke
ab, die der Mock offen lässt.

### Comparator-Contract

Der ursprüngliche Bug (`return -1`) war kein Sortierfehler, sondern ein Verstoss
gegen den Contract von `Comparator`. Deshalb prüfen wir den explizit:

* **reflexiv** – `compare(a, a) == 0`
* **symmetrisch** – `signum(compare(a, b)) == -signum(compare(b, a))`
* **transitiv** – aus `a < b` und `b < c` folgt `a < c`

Der Symmetrie-Test ist der, der bei `return -1` als erstes rot geworden wäre.

## Bekannte Stolpersteine

* Hibernate liefert für `registrationDate` eine `java.sql.Timestamp` zurück, nicht
  eine `java.util.Date`. `isEqualTo()` schlägt dann trotz gleicher Zeit fehl – im
  Repository-Test vergleichen wir deshalb mit `hasSameTimeAs()`.
* `sortBy`/`direction` müssen als Enum-Namen in Grossbuchstaben übergeben werden;
  Springs Standard-Konvertierung ist case-sensitiv.
* `Address` hat kein `@GeneratedValue` – die `id` muss beim Anlegen mitgegeben
  werden, und ein `save()` mit bestehender `id` ist ein Update.
