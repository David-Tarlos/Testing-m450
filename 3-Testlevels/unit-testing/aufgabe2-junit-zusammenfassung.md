# Aufgabe 2 — JUnit 5: Zusammenfassung der gängigsten Features

Kurzreferenz zu den JUnit-5-Features, die man im Modul m450 braucht — je mit
Mini-Beispiel und Anwendungsfall.

## Aufbau

JUnit 5 (= "Jupiter") besteht aus **Platform** (startet die Tests, Schnittstelle
für IDE und Maven), **Jupiter** (Annotations und Assertions, die man schreibt)
und **Vintage** (führt alte JUnit-4-Tests weiter aus).

Unterschiede zu JUnit 4: `@Before` → `@BeforeEach`, `@BeforeClass` →
`@BeforeAll`, `@RunWith` → `@ExtendWith`; Testklassen und -methoden müssen
**nicht mehr `public`** sein.

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>   <!-- API + Params + Engine -->
    <version>5.10.0</version>
    <scope>test</scope>
</dependency>
```

---

## 1. `@Test` und Lifecycle

| Annotation | Läuft | Anwendungsfall |
|---|---|---|
| `@Test` | ein Testfall | der Normalfall: ein Test pro fachlicher Regel |
| `@BeforeAll` | einmal vor allen Tests (`static`) | teure Ressource aufbauen (DB, Server) |
| `@BeforeEach` | vor **jedem** Test | frisches Testobjekt erzeugen |
| `@AfterEach` | nach jedem Test | aufräumen (Dateien, Mocks) |
| `@AfterAll` | einmal am Schluss (`static`) | Ressource schliessen |

```java
private SavingsAccount account;

@BeforeEach
void setUp() {
    account = new SavingsAccount("S-1000");   // jeder Test startet gleich
    account.deposit(13560, 10000);
}
```

JUnit erzeugt **pro Testmethode eine neue Instanz** der Testklasse — Tests
beeinflussen sich so nie gegenseitig. Genau dafür ist `@BeforeEach` da.
*Im Repo:* `SavingsAccountTests`, `SalaryAccountTests`, `BankTests`.

---

## 2. Assertions

Statisch importieren aus `org.junit.jupiter.api.Assertions`. Letzter Parameter
ist immer optional eine Fehlermeldung:
`assertFalse(account.withdraw(...), "Sparkonto darf nicht ins Minus")`.

| Assertion | Prüft |
|---|---|
| `assertEquals(erwartet, ist)` | Gleichheit (**erwartet zuerst**) |
| `assertEquals(erwartet, ist, delta)` | Fliesskomma mit Toleranz — ohne Delta wird `0.1+0.2` rot |
| `assertTrue` / `assertFalse` | Bedingung |
| `assertNull` / `assertNotNull` | Null-Prüfung |
| `assertSame` / `assertNotSame` | Identität (`==`) statt `equals` |
| `assertArrayEquals` | Arrays elementweise |
| `assertThrows` | eine Exception wird geworfen |
| `assertAll` | mehrere Assertions als Gruppe |
| `fail("...")` | Test bewusst rot machen |

**`assertAll`** — mehrere Eigenschaften desselben Objekts prüfen und *alle*
Fehler auf einmal sehen (sonst stoppt der Test bei der ersten Assertion):

```java
assertAll("Initialzustand",
        () -> assertEquals("TEST-1", account.getId()),
        () -> assertEquals(0, account.getBalance()),
        () -> assertTrue(account.canTransact(0)));
```

**`assertThrows`** — Negativtests; liefert die Exception zurück, damit man auch
die Meldung prüfen kann:

```java
var ex = assertThrows(ArithmeticException.class, () -> calculator.divide(10.0, 0.0));
assertEquals("Division durch 0 ist nicht erlaubt", ex.getMessage());
```

*Im Repo:* `CalculatorTest`, `AccountTests`.

---

## 3. Lesbarkeit: `@DisplayName` und `@Nested`

```java
@Nested @DisplayName("Ein- und Auszahlen")
class TransactionTests {

    @Test
    @DisplayName("Genau auf die Kreditlimite abheben ist erlaubt (Grenzfall)")
    void testWithdrawExactlyToCreditLimit() { ... }
}
```

**`@DisplayName`** macht den Testreport zur Spezifikation — Umlaute und
Leerzeichen erlaubt. **`@Nested`** gruppiert grosse Testklassen; die innere
Klasse darf ein eigenes `@BeforeEach` haben, aber **nicht `static`** sein.
*Im Repo:* `BankTests`, `AccountTests`, `CalculatorTest`.

---

## 4. `@ParameterizedTest` — derselbe Test mit vielen Werten

Braucht `junit-jupiter-params`. Jeder Datensatz erscheint im Report als eigener
Testfall, man sieht also genau, welcher Wert gescheitert ist.

```java
@ParameterizedTest(name = "Einzahlung {0} -> Saldo {1}")
@CsvSource({"100, 101", "1000, 1010", "99, 99"})   // 99: Bonus faellt weg
void testBonusRounding(long amount, long expectedBalance) {
    account.deposit(13560, amount);
    assertEquals(expectedBalance, account.getBalance());
}
```

| Quelle | Liefert |
|---|---|
| `@ValueSource(ints/strings = {...})` | genau **einen** Parameter pro Durchlauf |
| `@CsvSource({"a,1", "b,2"})` | mehrere Parameter, direkt im Code |
| `@CsvFileSource(resources = "/daten.csv")` | mehrere Parameter aus einer Datei |
| `@EnumSource(Kontotyp.class)` | alle Werte eines Enums |
| `@MethodSource("werteLiefern")` | beliebige Objekte aus einer statischen Methode |
| `@NullAndEmptySource` | `null` und Leerstring |

**Anwendungsfall:** Äquivalenzklassen und Grenzwertanalyse kompakt abbilden,
statt zehnmal fast dieselbe Testmethode zu schreiben.

---

## 5. Steuerung der Ausführung

| Feature | Beispiel | Anwendungsfall |
|---|---|---|
| `@Disabled` | `@Disabled("Wartet auf BigDecimal, #42")` | Test bewusst abschalten — **immer mit Begründung**; besser als auskommentieren, denn so bleibt er im Report |
| `@Tag` | `@Tag("unit")`, dann `mvn test -Dgroups=unit` | schnelle Unit-Tests bei jedem Commit, langsame Integrationstests nur nachts |
| `@Timeout` | `@Timeout(value = 500, unit = MILLISECONDS)` | Endlosschleifen abfangen; auf langsamen Buildservern schnell "flaky" |
| `Assumptions` | `assumeTrue(os.startsWith("Windows"))` | umgebungsabhängige Tests *überspringen* (grau) statt rot färben; ebenso `@EnabledOnOs` |
| `@RepeatedTest` | `@RepeatedTest(10)` | Zufallswerte, Nebenläufigkeit |
| `@TestMethodOrder` | mit `@Order(1)` | erzwingt Reihenfolge — **Faustregel: nicht verwenden**, Tests sollen unabhängig sein |
| `@TestInstance(PER_CLASS)` | erlaubt nicht-statisches `@BeforeAll` | teures Setup nur einmal; Preis: geteilter Zustand |

---

## Konventionen

* **AAA-Muster**: *Arrange* — *Act* — *Assert*, durch Leerzeilen getrennt.
* **Ein Testfall = eine Regel.** Ein "und" im Testnamen heisst meist: zwei Tests.
* **Testnamen sagen, was gilt**: `testWithdrawBeyondCreditLimit` statt `test3`.
* **Grenzwerte testen**, nicht Zufallswerte: bei Limite 5000 sind 4999/5000/5001
  interessant, 137 ist es nicht.
* **Auch Fehlerfälle testen** — negative Beträge, `null`, unbekannte IDs.
* **Maven/Surefire**: Testklassen müssen `*Test`, `*Tests`, `*TestCase` oder
  `Test*` heissen, sonst werden sie beim Build ignoriert.

---

## Referenzen

* **[JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)** —
  die offizielle Referenz, Einstieg über Kapitel *2. Writing Tests*.
* [JUnit 5 Javadoc](https://junit.org/junit5/docs/current/api/org.junit.jupiter.api/org/junit/jupiter/api/package-summary.html)
  — zum Nachschlagen einzelner Assertions.
* [Baeldung: A Guide to JUnit 5](https://www.baeldung.com/junit-5) — kompakter,
  beispielorientierter Einstieg.
* [JaCoCo Maven Plugin](https://www.jacoco.org/jacoco/trunk/doc/maven.html) —
  Code Coverage für Aufgabe 4.
