# Übung TDD — MyFancyClass

> Lösung zu den Aufgaben aus Kapitel 9. Vorgabe: `FancyApp.zip` aus dem Kurs-Repository
> (`Unterlagen/test-driven-development/x_gitres/FancyApp.zip`). Alle Ausgaben unten stammen aus
> echten Läufen mit **PHP 8.3.33** und **PHPUnit 10.5.66** unter Windows 11.

<!-- TOC -->
- [1 — Aufgabe 1 — Setup environment](#1--aufgabe-1--setup-environment)
- [2 — Aufgabe 2 — Methoden implementieren](#2--aufgabe-2--methoden-implementieren)
  - [2.1 — shortString](#21--shortstring)
  - [2.2 — calcAverage](#22--calcaverage)
  - [2.3 — getOpposite](#23--getopposite)
  - [2.4 — Refactor](#24--refactor)
  - [2.5 — Endergebnis](#25--endergebnis)
- [3 — Aufgabe 3 — Auswerten](#3--aufgabe-3--auswerten)
- [4 — Reflexion](#4--reflexion)
<!-- TOC -->

---

## 1 — Aufgabe 1 — Setup environment

| Schritt | Befehl / Aktion |
|---|---|
| PHP installieren | `winget install PHP.PHP.8.3` |
| `php.ini` anlegen | Kopie von `php.ini-development`, aktiviert: `extension_dir`, `mbstring`, `openssl`, `curl`, `zip` |
| PHPUnit | `composer.json` mit `phpunit/phpunit ^10.5`, dann `composer install` |
| Tests ausführen | `cd FancyApp/Testcases` → `php ../vendor/bin/phpunit Test.php` |

Die Tests laufen aus `Testcases/`, weil `Test.php` die Klasse mit dem relativen Pfad
`require_once('../Application/MyFancyClass.php')` lädt.

### Lauf 0 — Vorgabe völlig unverändert

```
An error occurred inside PHPUnit.

Message:  Class "PHPUnit_Framework_TestCase" not found
Location: ...\FancyApp\Testcases\Test.php:14
```

`PHPUnit_Framework_TestCase` ist der Klassenname aus PHPUnit ≤ 5. Gleichzeitig verlangt
`setUp() : void` aber PHPUnit ≥ 8. **Die Vorgabe läuft so mit keiner PHPUnit-Version.** Der
Kommentar in der Datei sagt, was zu tun ist. Genau diese zwei Zeilen wurden geändert, sonst nichts:

```diff
-//use PHPUnit\Framework\TestCase;
+use PHPUnit\Framework\TestCase;

-class Test extends PHPUnit_Framework_TestCase // for newer PHPUnit: ...
+class Test extends TestCase // for newer PHPUnit: ...
```

### Lauf 1 — Rot

```
PHPUnit 10.5.66 by Sebastian Bergmann and contributors.

Runtime:       PHP 8.3.33

There were 3 failures:

1) Test::testShortString
Failed asserting that null matches expected 'Ich bin Text mit 35 Char Länge (35)'.
...\Testcases\Test.php:37

2) Test::testCalcAverage
Failed asserting that null matches expected 5.
...\Testcases\Test.php:62

3) Test::testGetOpposite
Failed asserting that null matches expected '4.5,5,5.5'.
...\Testcases\Test.php:83

FAILURES!
Tests: 3, Assertions: 5, Failures: 3.
```

Wie in der Aufgabe beschrieben: alle drei rot. Auffällig sind aber die **5 Assertions**. Zwei davon
sind mit leeren Methoden *grün*:

* Zeile 36: `assertEquals('', shortString('', 100))`, also `null == ''`
* Zeile 61: `assertEquals(0, calcAverage([]))`, also `null == 0`

Mehr dazu in [Aufgabe 3](#3--aufgabe-3--auswerten).

---

## 2 — Aufgabe 2 — Methoden implementieren

Vorgehen: eine Methode nach der anderen, jeweils die **einfachste** Implementierung, dann Test
laufen lassen (`--filter testXyz`), dann nur so viel ergänzen, wie der nächste rote Assert verlangt.

### 2.1 — shortString

| Iteration | Code | Ergebnis |
|---|---|---|
| 1 | `strlen($text) <= $length` → Text, sonst `substr(...) . $ending` | 🔴 Zeile 42: `Failed asserting that '' is false.` |
| 2 | + `strlen($ending) > $length` → `false`, **vor** der Längenprüfung | 🔴 Zeile 49 (siehe unten) |
| 3 | `strlen`/`substr` → `mb_strlen`/`mb_substr` | 🟢 `OK (1 test, 12 assertions)` |

**Iteration 2** ist das Lehrstück dieser Übung:

```
1) Test::testShortString
Failed asserting that two strings are equal.
--- Expected
+++ Actual
@@ @@
-'Ich bin Text mit 35 Char Länge (35)'
+'I am a end and a complete sentence.'

...\Testcases\Test.php:49
```

Der Text heisst ausdrücklich «35 Char». Weil `Test.php` in UTF-8 gespeichert ist, zählt `strlen`
aber **36 Bytes** (`ä` = 2 Bytes). Der Text gilt deshalb als zu lang und wird auf 0 Zeichen +
Ende gekürzt. Nachgemessen:

```
strlen("Länge")    → int(6)
mb_strlen("Länge") → int(5)
```

Ohne diesen Test wäre der Fehler nur bei Umlauten aufgefallen, also vermutlich erst in
Produktion bei deutschen Texten.

**Warum die Reihenfolge wichtig ist:** Zeile 42 verlangt `false` für einen **leeren** Text mit
Länge 20 und 35-Zeichen-Ende. Der leere Text würde problemlos passen. Die Prüfung «Ende zu lang»
muss deshalb vor «Text passt» stehen. Zeile 49 (35 Zeichen, Länge 35, Ende 35) zeigt, dass
«zu lang» `>` heisst und nicht `>=`.

### 2.2 — calcAverage

| Iteration | Code | Ergebnis |
|---|---|---|
| 1 | leer → `0`, sonst `array_sum / count` | 🔴 Zeile 64: `Failed asserting that 1.1466666666666667 is false.` · `Warnings: 1` |
| 2 | + jeder Wert muss `is_numeric` sein, sonst `false` | 🟢 `OK (1 test, 4 assertions)` |

Iteration 1 zeigt, warum der Test nötig ist: `['3.44', 'bestanden', 'nicht bewertbar']` wirft
in PHP 8.3 **keinen Fehler**. Die zwei Wörter zählen als 0, das Ergebnis ist 3.44 / 3 = 1.1467,
und PHPUnit meldet nur eine Warnung. Ein Notenschnitt von 1.15 für einen Lernenden mit «bestanden»
wäre ein stiller, realer Fehler.

### 2.3 — getOpposite

| Iteration | Code | Ergebnis |
|---|---|---|
| 1 | `implode($delimiter, $value)` | 🔴 Zeile 87: `TypeError: implode(): Argument #2 ($array) must be of type ?array, string given` |
| 2 | + `is_string` → `explode(...)` | 🔴 Zeile 96: `TypeError: implode(): ... MyFancyClass given` |
| 3 | + `is_array` → `implode`, sonst `false` | 🟢 `OK (3 tests, 27 assertions)`: **alles grün** |

Der Name «Opposite» ist wörtlich gemeint: String → Array, Array → String. Die Asserts auf
Zeile 92–94 prüfen genau diese Umkehrbarkeit (`getOpposite(getOpposite($x)) == $x`).
`'4.5,5,5.5'` (Zeile 83) entsteht von selbst, weil PHP den Float `5.0` als `"5"` ausgibt.

### 2.4 — Refactor

Nach Grün, ohne neuen Test:

* kurze PHPDoc pro Methode, die das Verhalten aus den Tests in einem Satz festhält
* Kommentar, warum `mb_*` (sonst «optimiert» es jemand zurück auf `strlen`)
* `getOpposite` mit leerem Trennzeichen gibt `false` zurück. `explode('', ...)` würde in PHP 8 einen
  `ValueError` werfen. Das testen die Vorgabe-Tests nicht, es passt aber zum Rückgabewert
  `false` für ungültige Eingaben.

Danach erneut: `OK (3 tests, 27 assertions)`.

### 2.5 — Endergebnis

```
PHPUnit 10.5.66 by Sebastian Bergmann and contributors.

Runtime:       PHP 8.3.33

Unnamed Tests
 ✔ Short string
 ✔ Calc average
 ✔ Get opposite

OK (3 tests, 27 assertions)
```

Code: [`FancyApp/Application/MyFancyClass.php`](FancyApp/Application/MyFancyClass.php)

---

## 3 — Aufgabe 3 — Auswerten

> **Offener Punkt:** Der Vergleich mit den Lösungen anderer Gruppen (Text-Compare-Tool) ist
> **noch nicht durchgeführt**, weil dafür die Lösungen der anderen Gruppen nötig sind.

Vorbereitung dafür: Die 27 Asserts legen das Verhalten **nicht vollständig** fest. An diesen
Stellen können zwei Lösungen beide grün sein und sich trotzdem unterschiedlich verhalten. Darauf
ist beim Vergleich zu achten. Die Spalte «meine Lösung» ist mit `php -r` nachgemessen.

| # | Stelle | Spielraum | meine Lösung |
|---|---|---|---|
| 1 | `shortString`, Zeichen zählen | Die Tests erzwingen `mb_strlen` nur, weil *ein* Testtext ein `ä` enthält. `iconv_strlen` oder `grapheme_strlen` wären ebenfalls grün. | `mb_strlen` |
| 2 | `shortString('abcd', 3, '...')` (Ende genau so lang wie Limit) | `>` oder `>=` bei «Ende zu lang» ist nur für passende Texte geprüft. Für zu lange Texte ist beides grün. | `"..."` |
| 3 | `shortString('abc', -1, '')` | negative Länge nicht spezifiziert | `false` |
| 4 | `calcAverage([])` | `assertEquals(0, …)` ist **lose**. Nachgemessen mit PHPUnit 10.5: `0`, `0.0`, `false`, `null` und `'0'` sind alle grün. Eine Methode **ohne** Leer-Prüfung (Division durch 0 → Fehler) fällt auf, ein `return;` aber nicht. | `int(0)` |
| 5 | `calcAverage(['1e2', 0])`, `[' 5', 5]`, `[true, 1]` | Was «numerisch» heisst, ist offen: `is_numeric` akzeptiert Exponent und Leerzeichen, `is_float`/`filter_var` nicht | `50` · `5` · `false` |
| 6 | `getOpposite('a,b', '')` | leeres Trennzeichen nicht getestet | `false` (andere: `ValueError`) |
| 7 | `getOpposite(42)` | Nur ein **Objekt** ist als ungültig getestet. Eine Zahl könnte genauso gut als String behandelt werden. | `false` |

**Erwartung für den Vergleich:** Der Code wird sich stark unterscheiden (Reihenfolge der Prüfungen,
Schleife vs. `array_filter`, `if` vs. `match`). Das ist vernachlässigbar. Funktional relevant
sind nur die Zeilen 4 und 6: Dort kann eine grüne Lösung abstürzen oder etwas Falsches liefern.

---

## 4 — Reflexion

* **Die Tests haben die Fehler gefunden, nicht ich.** Sowohl das `strlen`-Problem als auch der
  stille Durchschnitt aus Text wären in der ersten, «offensichtlichen» Implementierung gelandet.
* **Kleine Schritte zahlen sich aus.** Weil jede Iteration nur einen Assert weiterkam, war bei
  jedem Rot sofort klar, welche Zeile Code schuld ist.
* **Grün heisst nicht korrekt.** Aufgabe 3 zeigt es: Tests sind eine Spezifikation, aber nur so
  vollständig, wie sie geschrieben wurden. `assertEquals(0, null)` ist grün, und ein
  `assertSame` an diesen Stellen hätte den Spielraum geschlossen.
* **Die Vorgabe selbst lief nicht.** Auch Testcode muss gewartet werden, sonst testet er am Ende
  nichts mehr.
