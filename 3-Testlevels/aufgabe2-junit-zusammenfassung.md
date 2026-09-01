# Testing-m450

# Aufgabe 2 — JUnit 5: Zusammenfassung der gängigsten Features

Kurzreferenz zu den Features, die man im Modul m450 tatsächlich braucht. Jedes
Feature mit einem Mini-Beispiel und dem Hinweis, wo es in diesem Repository
eingesetzt wird.

## Was ist JUnit 5?

JUnit 5 (= "JUnit Jupiter") besteht aus drei Teilen:

| Teil | Aufgabe |
|---|---|
| **JUnit Platform** | Startet Tests. Die Schnittstelle, die IDEs und Maven/Gradle ansprechen. |
| **JUnit Jupiter** | Das neue Programmiermodell: die Annotations und Assertions, die man schreibt. |
| **JUnit Vintage** | Führt alte JUnit-3/4-Tests auf der neuen Platform aus (Migration). |

Wichtigster Unterschied zu JUnit 4: die Annotations heissen anders
(`@Before` → `@BeforeEach`, `@BeforeClass` → `@BeforeAll`), Testklassen und
-methoden müssen **nicht mehr `public`** sein, und `@RunWith` ist durch
`@ExtendWith` ersetzt.

Maven-Abhängigkeit (der Aggregator bringt API, Params und Engine mit):

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.0</version>
    <scope>test</scope>
</dependency>
```

---

## 1. Der Test selbst: `@Test`

Markiert eine Methode als Testfall. Kein Rückgabewert, keine Parameter (ausser
bei parametrisierten Tests und Dependency Injection).

```java
@Test
void addiertZweiZahlen() {
    assertEquals(5.0, new Calculator().add(2.0, 3.0));
}
```

**Anwendungsfall:** der Normalfall — ein Testfall pro fachlicher Regel.

---

## 2. Lifecycle: `@BeforeEach`, `@AfterEach`, `@BeforeAll`, `@AfterAll`

| Annotation | Läuft | Typischer Einsatz |
|---|---|---|
| `@BeforeAll` | einmal vor allen Tests der Klasse (muss `static` sein) | teure Ressource aufbauen (DB-Verbindung, Testserver) |
| `@BeforeEach` | vor **jedem** Test | frisches Testobjekt erzeugen |
| `@AfterEach` | nach jedem Test | aufräumen (Dateien löschen, Mocks zurücksetzen) |
| `@AfterAll` | einmal am Schluss (`static`) | Ressource schliessen |

```java
private SavingsAccount account;

@BeforeEach
void setUp() {
    account = new SavingsAccount("S-1000");   // jeder Test startet gleich
    account.deposit(13560, 10000);
}
```

**Warum das wichtig ist:** JUnit erzeugt pro Testmethode eine **neue Instanz** der
Testklasse. Tests dürfen sich deshalb nie gegenseitig beeinflussen — genau dafür
ist `@BeforeEach` da.

*Im Repo:* `SavingsAccountTests`, `SalaryAccountTests`, `BankTests`.

---

## 3. Assertions — die eigentliche Prüfung

Alle statisch importieren aus `org.junit.jupiter.api.Assertions`.

| Assertion | Prüft |
|---|---|
| `assertEquals(erwartet, ist)` | Gleichheit (Reihenfolge: **erwartet zuerst**) |
| `assertEquals(erwartet, ist, delta)` | Fliesskommazahlen mit Toleranz |
| `assertTrue` / `assertFalse` | Bedingung |
| `assertNull` / `assertNotNull` | Null-Prüfung |
| `assertSame` / `assertNotSame` | Identität (`==`) statt Gleichheit (`equals`) |
| `assertArrayEquals` | Arrays elementweise |
| `assertThrows` | eine Exception wird geworfen |
| `assertAll` | mehrere Assertions als Gruppe |
| `fail("...")` | Test bewusst rot machen (z.B. für noch nicht Implementiertes) |

Jede Assertion nimmt als **letzten** Parameter optional eine Fehlermeldung:

```java
assertFalse(account.withdraw(13561, 10001), "Ein Sparkonto darf nicht ins Minus");
```

### `assertAll` — alle Fehler auf einmal sehen

Ohne `assertAll` stoppt der Test bei der ersten fehlgeschlagenen Assertion. Mit
`assertAll` werden alle ausgeführt und gemeinsam gemeldet:

```java
assertAll("Initialzustand",
        () -> assertEquals("TEST-1", account.getId()),
        () -> assertEquals(0, account.getBalance()),
        () -> assertTrue(account.canTransact(0)));
```

**Anwendungsfall:** mehrere Eigenschaften **desselben** Objekts prüfen.

### `assertThrows` — Fehlerfälle testen

```java
ArithmeticException ex = assertThrows(
        ArithmeticException.class,
        () -> calculator.divide(10.0, 0.0));

assertEquals("Division durch 0 ist nicht erlaubt", ex.getMessage());
```

**Anwendungsfall:** Negativtests. Die Methode gibt das Exception-Objekt zurück,
sodass man auch die Meldung prüfen kann.

### Fliesskommazahlen immer mit Delta

```java
assertEquals(0.3, calculator.add(0.1, 0.2), 1e-9);   // ohne Delta: rot!
```

*Im Repo:* `CalculatorTest` (Delta, assertThrows, assertAll), `AccountTests` (assertAll).

---

## 4. `@DisplayName` — lesbare Testnamen

```java
@Test
@DisplayName("Genau auf die Kreditlimite abheben ist erlaubt (Grenzfall)")
void testWithdrawExactlyToCreditLimit() { ... }
```

**Anwendungsfall:** der Testreport wird zur Spezifikation. Statt
`testWithdrawExactlyToCreditLimit` steht im Report der ganze Satz — auch für
Leute lesbar, die den Code nicht kennen. Umlaute und Leerzeichen sind erlaubt.

---

## 5. `@Nested` — Tests gruppieren

```java
class BankTests {
    @Nested
    @DisplayName("Konten eröffnen")
    class CreateTests { ... }

    @Nested
    @DisplayName("Ein- und Auszahlen")
    class TransactionTests { ... }
}
```

**Anwendungsfall:** grosse Testklassen strukturieren. Die innere Klasse sieht die
Felder der äusseren, kann aber ein **eigenes** `@BeforeEach` haben — praktisch,
wenn eine Gruppe von Tests ein zusätzliches Setup braucht. Wichtig: die inneren
Klassen dürfen **nicht `static`** sein.

*Im Repo:* `BankTests`, `AccountTests`, `CalculatorTest`.

---

## 6. `@ParameterizedTest` — derselbe Test mit vielen Werten

Braucht das Modul `junit-jupiter-params`.

```java
@ParameterizedTest(name = "Einzahlung {0} -> Saldo {1}")
@CsvSource({
        "100,   101",
        "1000,  1010",
        "99,    99"     // Bonus faellt wegen Integer-Division weg
})
void testBonusRounding(long amount, long expectedBalance) {
    account.deposit(13560, amount);
    assertEquals(expectedBalance, account.getBalance());
}
```

Wichtigste Quellen für die Werte:

| Quelle | Liefert |
|---|---|
| `@ValueSource(ints/longs/doubles/strings = {...})` | genau **einen** Parameter pro Durchlauf |
| `@CsvSource({"a,1", "b,2"})` | mehrere Parameter, direkt im Code |
| `@CsvFileSource(resources = "/daten.csv")` | mehrere Parameter aus einer Datei |
| `@EnumSource(Kontotyp.class)` | alle Werte eines Enums |
| `@MethodSource("werteLiefern")` | beliebige Objekte aus einer statischen Methode |
| `@NullAndEmptySource` | `null` und Leerstring |

**Anwendungsfall:** Äquivalenzklassen und Grenzwertanalyse aus dem Modul
kompakt abbilden, statt zehnmal fast dieselbe Testmethode zu schreiben. Jeder
Datensatz erscheint im Report als eigener Testfall — man sieht also genau,
welcher Wert gescheitert ist.

---

## 7. `@Disabled` — Test vorübergehend abschalten

```java
@Test
@Disabled("Wartet auf die Umstellung auf BigDecimal, Ticket #42")
void rechnetMitRappenGenau() { ... }
```

**Anwendungsfall:** ein Test, der bewusst (noch) nicht laufen soll. **Immer mit
Begründung** — ein `@Disabled` ohne Text ist nach zwei Wochen ein Rätsel.
Auskommentieren ist die schlechtere Variante: der Test verschwindet dann
komplett aus dem Report.

---

## 8. `@Tag` — Tests kategorisieren

```java
@Tag("unit")
class CalculatorTest { ... }
```

Ausführen einer Kategorie:

```bash
mvn test -Dgroups=unit
mvn test -DexcludedGroups=slow
```

**Anwendungsfall:** schnelle Unit-Tests bei jedem Commit, langsame
Integrationstests nur nachts in der Pipeline.

---

## 9. `assertTimeout` und `@Timeout` — Laufzeit begrenzen

```java
@Test
@Timeout(value = 500, unit = TimeUnit.MILLISECONDS)
void findetKontoSchnell() { ... }
```

**Anwendungsfall:** Endlosschleifen abfangen, Performance-Zusagen absichern.
Vorsicht: auf langsamen Build-Servern werden solche Tests gerne "flaky".

---

## 10. `Assumptions` — Test nur unter Bedingungen ausführen

```java
@Test
void nurAufWindows() {
    assumeTrue(System.getProperty("os.name").startsWith("Windows"));
    // ab hier nur auf Windows
}
```

Unterschied zu einer Assertion: eine nicht erfüllte **Assumption** macht den Test
*übersprungen* (grau), nicht *rot*. Ergänzend gibt es Annotations wie
`@EnabledOnOs(WINDOWS)` oder `@EnabledIf`.

**Anwendungsfall:** umgebungsabhängige Tests, ohne den Build rot zu färben.

---

## 11. `@RepeatedTest` und `@TestMethodOrder`

```java
@RepeatedTest(10)
void bleibtStabil() { ... }
```

`@RepeatedTest` wiederholt einen Test — nützlich bei Zufallswerten oder
Nebenläufigkeit.

`@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` zusammen mit `@Order(1)`
erzwingt eine Reihenfolge. **Faustregel: nicht verwenden.** Unit-Tests sollen
unabhängig sein; wer eine Reihenfolge braucht, hat meistens ein Testdesign-Problem.

---

## 12. `@TestInstance` — eine Instanz für alle Tests

```java
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TeuresSetupTests {
    @BeforeAll
    void setUp() { ... }   // darf jetzt nicht-static sein
}
```

**Anwendungsfall:** wenn ein teures Setup nur einmal laufen soll. Preis: die Tests
teilen sich den Zustand und können sich beeinflussen.

---

## Konventionen, die sich bewährt haben

* **AAA-Muster**: *Arrange* (Testdaten aufbauen) — *Act* (Methode aufrufen) —
  *Assert* (Ergebnis prüfen). Am besten durch Leerzeilen sichtbar getrennt.
* **Ein Testfall = eine Regel.** Wenn ein Testname ein "und" enthält, sind es
  meistens zwei Tests.
* **Testnamen sagen, was gilt**, nicht was aufgerufen wird:
  `testWithdrawBeyondCreditLimit` statt `test3`.
* **Grenzwerte testen**, nicht Zufallswerte: bei einer Limite von 5000 sind
  4999 / 5000 / 5001 interessant, 137 ist es nicht.
* **Auch Fehlerfälle testen** — negative Beträge, `null`, unbekannte IDs.
  In der Banken-Simulation sind das über die Hälfte der Testfälle.
* **Namenskonvention Maven/Surefire**: Testklassen müssen `*Test`, `*Tests`,
  `*TestCase` oder `Test*` heissen, sonst werden sie beim Build ignoriert.

---

## Referenzen

* **[JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)** —
  die offizielle Referenz. Vollständig, mit lauffähigen Beispielen zu jeder
  Annotation. Empfohlener Einstieg: Kapitel *2. Writing Tests*.
* [JUnit 5 Javadoc (`org.junit.jupiter.api`)](https://junit.org/junit5/docs/current/api/org.junit.jupiter.api/org/junit/jupiter/api/package-summary.html)
  — zum schnellen Nachschlagen einer einzelnen Assertion.
* [Baeldung: A Guide to JUnit 5](https://www.baeldung.com/junit-5) — kompakter,
  beispielorientierter Einstieg.
* [JaCoCo Maven Plugin](https://www.jacoco.org/jacoco/trunk/doc/maven.html) —
  für die Code Coverage aus Aufgabe 4.
