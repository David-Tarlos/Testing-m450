# Übung 3 — Bank-Software: Black-Box, White-Box und Code-Review

> **Aufgabenstellung**
>
> Sie haben eine simple Bank-Software. Machen Sie sich mit dem Code vertraut und finden Sie grob heraus, was für
> Testfälle es in dieser Software gibt.
> * Identifizieren Sie mögliche **Black-Box-Testfälle**, welche Sie als Benutzer testen können.
> * Welche Methoden im Code könnten für **White-Box-Testfälle** verwendet werden?
> * Was würden Sie am Code generell verbessern, welche **Best Practices** fallen Ihnen ein?

**Analysierte Quelle:** `bank-software-mvn.zip` aus
[Unterlagen/teststrategie](https://gitlab.com/ch-tbz-it/Stud/m450/m450/-/tree/main/Unterlagen/teststrategie)
(Maven-Variante, Package `ch.tbz.bank.software`, 5 Klassen).

> **Testdurchführung:** Die Black-Box-Testfälle in Abschnitt 2 wurden am **18.08.2026** tatsächlich ausgeführt;
> die eingetragenen Resultate sind gemessen, nicht angenommen. Einzige Ausnahme ist **BB-23** (Verhalten ohne
> Internetverbindung), der eine manuelle Netzabschaltung erfordert und offen bleibt. Die Befunde in Abschnitt 4
> stammen aus der Analyse des Quelltexts und sind mit Zeilenangabe belegt.
>
> **Testumgebung:** Windows 11 · JDK 21.0.5 (JetBrains Runtime, mit IntelliJ IDEA Community 2024.2.4 gebündelt) ·
> Maven aus derselben IntelliJ-Installation · Projekt mit `mvn clean compile` gebaut · Start über
> `java -cp target/classes;<deps> ch.tbz.bank.software.Main`.
> Jeder Testfall lief in einer **frisch gestarteten** Anwendung mit den unveränderten Demokonten; die Eingaben
> wurden zeilenweise über die Standardeingabe zugeführt, damit die Läufe reproduzierbar sind.

---

## 1  Überblick über die Software

Konsolenanwendung, die einen Bankschalter simuliert: Konten anlegen, einzahlen, abheben, Kontostand abfragen,
überweisen, löschen sowie einen Wechselkurs über eine Web-API abfragen.

| Klasse | Verantwortung | Wichtigste Elemente |
|---|---|---|
| `Main` | Einstiegspunkt; legt 5 Demokonten an und startet die Menüschleife | `main()`, ausserdem das Enum `Currency` (USD/EUR/CHF) im selben File |
| `Account` | Datenhaltung eines Kontos: Id, Nachname, Währung, Saldo | `deposit()`, `withdraw()`, `printBalance()`, `getBalance()` |
| `Bank` | Verwaltet die Kontenliste (`ArrayList<Account>`) | `createAccount()`, `deleteAccount()`, `getAccount()`, `printAccountsList()` |
| `Counter` | Der Schalter: gesamte Benutzerführung, Eingabeprüfung und Ablauflogik | `chooseAccount()`, `editAccount()`, `transferAmount()`, `convertCurrency()`, `createAccount()` |
| `ExchangeRateOkhttp` | Holt Wechselkurse per HTTP von `api.apilayer.com` | `getExchangeRate()`, innere Klasse `Rate` |

**Abhängigkeiten** (`pom.xml`): OkHttp `5.0.0-alpha.11` (HTTP-Client), Gson `2.8.2` (JSON-Parsing), Java 20.
Es gibt **kein** `src/test/java` und **keine** Test-Dependency — die Software enthält aktuell keinen einzigen Test.

**Architektur in einem Satz:** Benutzerführung, Eingabeprüfung, Fachlogik und Ausgabe liegen alle in `Counter`;
`Account` und `Bank` sind reine Datenhalter. Genau diese Vermischung ist der Hauptgrund, warum die Software heute
kaum automatisiert testbar ist (siehe Abschnitt 4).

---

## 2  Black-Box-Testfälle (als Benutzer über die Konsole prüfbar)

Getestet wird ausschliesslich über Menüeingaben und Konsolenausgabe, ohne Kenntnis des Codes.
Ausgangslage jeweils: Anwendung frisch gestartet, 5 Demokonten (Nr. 1–5) vorhanden.

| ID | Beschreibung | Erwartetes Resultat | Effektives Resultat | Status | Mögliche Ursache |
|----|--------------|---------------------|---------------------|--------|------------------|
| BB-01 | Konto 1 wählen, `e` (einzahlen), Betrag `100` | Kontostand steigt um exakt 100.00, neuer Stand wird ausgegeben | "Aktueller Kontostand: 1600.00 USD" (vorher 1500.00) | OK | – |
| BB-02 | Konto 1 wählen, `a` (abheben), Betrag `500` | Kontostand sinkt um den Betrag, neuer Stand wird ausgegeben | "Aktueller Kontostand: 1000.00 USD" | OK | – |
| BB-03 | Abheben von `999999` — **grösser** als der Kontostand | Meldung "Kontostand zu niedrig", Saldo bleibt unverändert | "! Kontostand zu niedrig (momentan 1500.0 USD)."; Saldo unverändert 1500.00, Betrag wird erneut abgefragt | OK | Schönheitsfehler: Betrag in der Meldung unformatiert ("1500.0" statt 1500.00), da String-Verkettung statt `printf` |
| BB-04 | Abheben von **genau** dem Kontostand `1500` (Grenzwert) | Buchung wird ausgeführt, neuer Saldo ist 0.00 | "Aktueller Kontostand: 0.00 USD" | OK | – |
| BB-05 | Einzahlen eines **negativen** Betrags (`-100`) | Eingabe wird abgewiesen; Saldo darf nicht sinken | Buchung wird ausgeführt: Saldo **sinkt** von 1500.00 auf **1400.00 USD** — Einzahlen wirkt als Abhebung | **Fehler** | **F-01** — keine Prüfung auf `amount > 0` in `Account.deposit()` |
| BB-06 | Abheben eines **negativen** Betrags (`-100`) | Eingabe wird abgewiesen; Saldo darf nicht steigen | Buchung wird ausgeführt: Saldo **steigt** von 1500.00 auf **1600.00 USD** — Geld wird aus dem Nichts erzeugt | **Fehler** | **F-01** — `-100 > 1500` ist `false`, also `balance -= -100` |
| BB-07 | Betragseingabe `abc` beim Einzahlen | Meldung "Ungültige Eingabe", erneute Abfrage, Saldo unverändert | "! Ungültige Eingabe, bitte nochmals!", erneute Abfrage; danach `100` → 1600.00 USD | OK | – |
| BB-08 | Betragseingabe `NaN` beim Einzahlen | Eingabe wird abgewiesen, Saldo bleibt eine gültige Zahl | Eingabe wird akzeptiert: "Aktueller Kontostand: **NaN** USD" — Konto danach unbrauchbar | **Fehler** | **F-04** — `Double.parseDouble("NaN")` wirft nicht, `NaN`-Vergleiche sind immer `false` |
| BB-09 | Überweisung 100 von Konto 1 (USD) auf Konto 3 (CHF) | Betrag wird abgezogen und beim Zielkonto **umgerechnet** gutgeschrieben | Konto 1: 1500.00 → 1400.00 USD; Konto 3: 23500.00 → **23611.00 CHF** (Faktor 1.11 angewendet) | OK | Umrechnung erfolgt, allerdings mit **fest verdrahtetem** Kurs — siehe V-05 |
| BB-10 | Überweisung 100 von Konto 2 (EUR) auf Konto 3 (CHF) | Ebenfalls korrekte Währungsumrechnung | "! Es wurde keine Umrechnung vorgenommen." Konto 2: 2000.00 → 1900.00 EUR; Konto 3: 23500.00 → **23600.00 CHF** — 100 EUR wurden 1:1 als 100 CHF gutgeschrieben | **Fehler** | **F-02** — Paar EUR→CHF fehlt in `convertCurrency()`, Buchung läuft trotzdem weiter |
| BB-11 | Überweisung auf **das eigene** Konto | Meldung "Bitte ein anderes Konto auswählen", keine Buchung | "! Bitte ein anderes Konto als das momentane Konto auswählen!", keine Buchung | OK | – |
| BB-12 | Überweisung auf eine **nicht existierende** Kontonummer (`99`) | Meldung "Konto nicht vorhanden", keine Buchung | "! Ein Konto mit dieser Nummer ist nicht vorhanden!", keine Buchung | OK | – |
| BB-13 | Überweisung mit Betrag `999999` grösser als Saldo | Meldung "Kontostand zu niedrig"; **beide** Konten bleiben unverändert | "! Kontostand zu niedrig! (momentan 1500.0 USD)"; Konto 1 unverändert 1500.00 USD | OK | – |
| BB-14 | Konto 1 löschen, Bestätigung mit `j` | Konto verschwindet aus der Liste (`a`), Meldung "wurde gelöscht" | "Konto mit Nummer 1 wurde gelöscht."; Liste zeigt danach nur noch Nr. 2–5 | OK | – |
| BB-15 | Konto 1 löschen, Bestätigung mit `n` | Meldung "Aktion abgebrochen", Konto bleibt in der Liste | "! Aktion abgebrochen."; Liste zeigt weiterhin Nr. 1–5 | OK | – |
| BB-16 | Im Hauptmenü eine nicht existierende Kontonummer eingeben (`99`) | Meldung "Ein Konto mit dieser Nummer ist nicht vorhanden", Menü erscheint erneut | "Ein Konto mit dieser Nummer ist nicht vorhanden!", Menü erscheint erneut | OK | – |
| BB-17 | Im Hauptmenü Text eingeben, der einen Menübuchstaben enthält (`hallo`) | Klare Meldung "Ungültige Eingabe", Menü erscheint erneut | **Keinerlei Ausgabe** — das Menü erscheint wortlos erneut, der Benutzer erhält keine Rückmeldung | **Fehler** | **R-02 / R-03** — `find()` akzeptiert das `a` in "hallo", der `NumberFormatException` wird stillschweigend verschluckt |
| BB-18 | Im Aktionsmenü nur **Enter** drücken (leere Eingabe) | Meldung "Ungültige Eingabe", Menü erscheint erneut — **kein Absturz** | **Absturz:** `java.lang.StringIndexOutOfBoundsException: Range [0, 1) out of bounds for length 0`, Programm endet mit Exit-Code 1 | **Fehler** | **R-01** — `substring(0,1)` in `Counter.java:105` läuft vor jeder Prüfung |
| BB-19 | Bei der Löschbestätigung nur **Enter** drücken | Behandelt wie "nein", keine Löschung — **kein Absturz** | **Absturz:** dieselbe `StringIndexOutOfBoundsException`, Exit-Code 1 | **Fehler** | **R-01** — gleiche Stelle in `Counter.java:153` |
| BB-20 | Neues Konto erstellen mit Währung `chf` (Kleinschreibung) | Konto wird mit Währung CHF angelegt und angezeigt | Konto Nr. 6, Nachname Meier, "Kontostand: 0.00 CHF" | OK | – |
| BB-21 | Neues Konto erstellen mit unbekannter Währung `XYZ` | Eingabe wird abgewiesen und erneut abgefragt (nicht stillschweigend auf USD gesetzt) | "! Die eingegebene Währung ist nicht bekannt, es wird USD verwendet." Konto Nr. 6 wird **trotzdem** angelegt, mit 0.00 USD | **Fehler** | **F-05** — `default`-Zweig meldet nur und legt das Konto in der falschen Währung an |
| BB-22 | Wechselkurs abfragen mit `CHF USD` | Kurs wird ausgegeben ("1 CHF = … USD") | "1 CHF = 1.232202 USD" — API antwortet, Schlüssel ist gültig | OK | Der gelieferte Kurs weicht stark vom fest verdrahteten Wert ab — siehe V-05 |
| BB-23 | Wechselkurs abfragen ohne Internetverbindung | Verständliche Fehlermeldung, Anwendung läuft weiter | – | offen | Erfordert manuelles Abschalten der Netzverbindung, nicht ausgeführt |
| BB-24 | `q` im Hauptmenü | "Auf Wiedersehen!", Programm endet sauber | "Auf Wiedersehen!", Programm endet mit Exit-Code 0 | OK | – |

### Ergebnis der Testdurchführung

**15 × OK · 8 × Fehler · 1 × offen** (BB-23, nicht ausgeführt)

Alle acht Fehlschläge waren aus der Codeanalyse in Abschnitt 4 vorhergesagt worden und bestätigen sie mit Zahlen:

| Befund | Bestätigt durch | Kernbeleg |
|---|---|---|
| **F-01** — fehlende Prüfung auf positive Beträge | BB-05, BB-06 | Abheben von −100 erhöht den Saldo auf 1600.00 USD |
| **F-02** — unvollständige Umrechnungsmatrix | BB-10 | 100 EUR werden als 100 CHF gutgeschrieben statt umgerechnet |
| **F-04** — `NaN` wird akzeptiert | BB-08 | Saldo dauerhaft "NaN USD" |
| **F-05** — unbekannte Währung wird zu USD | BB-21 | Konto mit `XYZ` wird als USD-Konto angelegt |
| **R-01** — `substring(0,1)` vor der Prüfung | BB-18, BB-19 | `StringIndexOutOfBoundsException`, Programmabbruch bei blossem Enter |
| **R-02 / R-03** — `find()` statt `matches()`, verschluckte Exception | BB-17 | "hallo" erzeugt keinerlei Rückmeldung |

**Zusätzlicher Befund aus dem Testlauf:** Der Vergleich von BB-22 mit BB-09 zeigt die Inkonsistenz aus **V-05** in
Zahlen. Die Wechselkurs-API meldet `1 CHF = 1.232202 USD`, während `convertCurrency()` mit dem fest verdrahteten
`RATIO_CHF_TO_USD = 0.9` rechnet (`Counter.java:245`) — eine Abweichung von rund 27 %. Die Anwendung zeigt dem
Benutzer also einen Kurs an, mit dem sie selbst nicht bucht.

**Nicht als Fehler gewertet:** Wird die Eingabe über eine Datei statt über die Tastatur zugeführt, endet die
Anwendung am Dateiende mit einer `NoSuchElementException` aus `Scanner.nextLine()`. Das ist ein Artefakt der
automatisierten Ausführung und tritt im normalen interaktiven Betrieb nicht auf.

---

## 3  White-Box-Testfälle — geeignete Methoden

Priorisiert nach Verzweigungsdichte und Rechenlogik. Das sind die Methoden, die sich später (Kapitel Unit-Testing)
direkt als JUnit-Tests schreiben lassen — sie haben Parameter und Rückgabewerte und brauchen keine Konsole.

| Methode | Warum geeignet | Zu prüfende Pfade / Grenzwerte |
|---|---|---|
| `Account.withdraw(double)` <br> `Account.java:49` | Enthält die einzige echte Geschäftsregel im Datenmodell (`if (amount > balance)`), gibt `boolean` zurück, keine Konsolenabhängigkeit — der ideale erste Unit-Test | `amount < balance` (true, Saldo korrekt), `amount == balance` (**Grenzwert**, Saldo 0), `amount > balance` (false, Saldo **unverändert**), `amount == 0`, `amount < 0`, `Double.NaN` |
| `Counter.convertCurrency(double, Currency, Currency)` <br> `Counter.java:241` | Reine Berechnungsfunktion mit drei `if`-Zweigen und einem Default-Pfad; alle 9 Währungskombinationen sind vollständig durchtestbar | Die 3 abgedeckten Paare (USD→CHF, USD→EUR, CHF→USD) gegen den erwarteten Faktor; die 3 **nicht** abgedeckten Paare (CHF→EUR, EUR→USD, EUR→CHF); gleiche Währung; `amount == 0`; negativer Betrag |
| `Account.deposit(double)` <br> `Account.java:40` | Trivial, aber genau deshalb der Test, der die fehlende Validierung sichtbar macht | positiver Betrag, `0`, negativer Betrag, `Double.NaN`, sehr grosser Betrag (Überlaufverhalten) |
| `Bank.getAccount(int)` <br> `Bank.java:31` | Schleife mit Abbruchbedingung und `null`-Rückgabe — klassischer Pfadtest | vorhandene Id (erstes / letztes Element), nicht vorhandene Id, Id `0`, negative Id, leere Kontenliste |
| `Bank.createAccount(...)` / `deleteAccount(...)` / `getNumberOfAccounts()` <br> `Bank.java:13`, `:23`, `:73` | Zustandsänderungen der Liste, gut über `getNumberOfAccounts()` prüfbar | Anlegen erhöht die Anzahl um 1; Löschen verringert sie um 1; Löschen eines nicht enthaltenen Kontos; Löschen aus leerer Liste |
| `ExchangeRateOkhttp.getExchangeRate(String, String)` <br> `ExchangeRateOkhttp.java:23` | Testbar nur mit **Stub/Mock** des HTTP-Aufrufs — genau das Thema "Abhängigkeiten zu Schnittstellen" (Kapitel 4) | Erfolgsfall mit Beispiel-JSON; HTTP-Fehler; leerer Body; ungültiges JSON; kein Netz |

**Nicht sinnvoll direkt testbar (erst nach Refactoring):**
`Counter.chooseAccount()`, `editAccount()`, `deposit()`, `withdraw()`, `transfer()`, `createAccount()` und
`getExchangeRate()` lesen alle direkt von `Scanner(System.in)` und schreiben auf `System.out` (`Counter.java:18`
und durchgehend). Ein Unit-Test müsste die Konsole umleiten und Eingaben simulieren. Sauberer ist, die Fachlogik
aus diesen Methoden herauszulösen (siehe V-01), dann werden sie wie die Methoden oben testbar.

---

## 4  Verbesserungsvorschläge und Best Practices

Alle Punkte sind am vorliegenden Quelltext belegt, sortiert nach Schwere.

### 4.1  Fachliche Fehler

| ID | Befund | Fundstelle | Warum das ein Problem ist |
|---|---|---|---|
| F-01 | **Negative Beträge kehren die Buchungsrichtung um.** `withdraw(-100)` prüft `-100 > balance` → `false`, also `balance -= -100` → der Saldo **steigt**. `deposit(-100)` senkt ihn analog. Nirgends wird auf `amount > 0` geprüft. | `Account.java:40`, `Account.java:49` | Über "abheben" lässt sich Geld erzeugen. Bei einer Überweisung mit negativem Betrag fliesst das Geld in die **Gegenrichtung** (`Counter.java:212` und `:221`). Testfälle BB-05/BB-06. |
| F-02 | **Unvollständige Umrechnungsmatrix.** Abgedeckt sind nur USD→CHF, USD→EUR und CHF→USD. Für CHF→EUR, EUR→USD und EUR→CHF wird nur eine Meldung ausgegeben und der Betrag **unverändert** gutgeschrieben. | `Counter.java:241-261` | Die Hälfte aller Währungspaare bucht den falschen Betrag — und das mit blossem Hinweis statt Abbruch. Testfall BB-10. |
| F-03 | **Keine Atomarität bei der Überweisung.** Erst `accFrom.withdraw(amount)`, danach `accTo.deposit(amount)` — dazwischen liegt die Umrechnung. Ein Rollback existiert nicht. | `Counter.java:212-221` | Geld kann das Quellkonto verlassen, ohne anzukommen. Buchungen gehören in **eine** Operation mit definiertem Fehlerfall. |
| F-04 | **`NaN` / `Infinity` werden akzeptiert.** `Double.parseDouble("NaN")` wirft keine Exception; `NaN > balance` ist `false`, also wird gebucht und der Saldo dauerhaft `NaN`. | `Counter.java:211`, `:268`, `:282` | Das Konto ist danach unbrauchbar und lässt sich nicht mehr korrigieren. Testfall BB-08. |
| F-05 | **Unbekannte Währung wird stillschweigend zu USD.** Der `default`-Zweig meldet den Fehler nur und legt das Konto trotzdem an. | `Counter.java:325-328` | Ein Tippfehler bei der Kontoeröffnung erzeugt ein Konto in der falschen Währung. Testfall BB-21. |
| F-06 | **Anfangssaldo lässt sich nicht erfassen.** `startBalance` ist fest `0.0` und wird nie abgefragt, obwohl `createAccount` den Parameter durchreicht. | `Counter.java:302`, `:331` | Toter Parameterpfad — nur über `Bank.createAccount()` direkt erreichbar. |
| F-07 | **Löschmeldung ohne Prüfung.** "Konto … wurde gelöscht" wird ausgegeben, ohne den Rückgabewert von `accounts.remove(a)` auszuwerten. | `Bank.java:23-29` | Die Anwendung meldet Erfolg, auch wenn nichts gelöscht wurde. |
| F-08 | **Startmeldung wird nach dem Löschen falsch.** "Es gibt %d Konten mit den Nummern 1–%d" leitet den Nummernbereich aus der **Anzahl** ab statt aus den tatsächlichen Ids. | `Main.java:21` | Nach einer Löschung stimmen Anzahl und Nummernbereich nicht mehr überein. |

### 4.2  Robustheit und Eingabeprüfung

| ID | Befund | Fundstelle | Warum das ein Problem ist |
|---|---|---|---|
| R-01 | **Absturz bei leerer Eingabe.** `input.substring(0,1)` wird **vor** jeder Prüfung ausgeführt; ein blosses Enter liefert `""` → `StringIndexOutOfBoundsException`. Dieselbe Stelle gibt es in der Löschbestätigung. | `Counter.java:105`, `Counter.java:153` | Die Anwendung stürzt bei der harmlosesten Fehlbedienung ab. Testfälle BB-18/BB-19. Prüfen vor Zugreifen, z.B. `str.isEmpty()` abfangen. |
| R-02 | **Regex prüft mit `find()` statt `matches()`.** Das Muster gilt als erfüllt, sobald ein Zeichen **irgendwo** vorkommt: `hallo` enthält `a` und passiert die Prüfung. Danach greift kein `switch`-Zweig, `Integer.parseInt("hallo")` wirft, und der `catch`-Block gibt für diesen Fehlertyp nichts aus — die Schleife läuft kommentarlos weiter. | `Counter.java:40-46`, `:64-80` | Der Benutzer bekommt auf eine falsche Eingabe **gar keine** Rückmeldung. Testfall BB-17. |
| R-03 | **Menüpunkte fallen in die Zahlenauswertung durch.** Nach `case "a"`/`"e"`/`"w"` fehlt ein `continue`; anschliessend wird die Buchstabeneingabe zusätzlich durch `Integer.parseInt` geschickt und die entstehende Exception verschluckt. | `Counter.java:54-65` | Funktioniert nur zufällig, weil der Fehler unterdrückt wird. Steuerfluss explizit beenden. |
| R-04 | **`catch (Exception e)` mit `instanceof`-Abfragen** statt mehrerer typisierter `catch`-Blöcke — an vier Stellen. Nicht erwartete Ausnahmen (z.B. `NumberFormatException`) fallen durch alle Abfragen und verschwinden spurlos. | `Counter.java:72-80`, `:193-200`, `:224-230`, `:288-295` | Fehler werden versteckt statt behandelt. Typisierte `catch`-Blöcke sind präziser und vom Compiler geprüft. |
| R-05 | **Falscher Text in der Fehlermeldung:** angeboten werden `a`, `e`, `w`, `q`, die Meldung nennt aber `"a"`, `"e"`, `"u"`, `"q"`. | `Counter.java:44` | Führt den Benutzer in die Irre; typischer Copy-Paste-Rest. |
| R-06 | **Zeichenklasse mit überflüssigen Alternativstrichen** im Wechselkurs-Muster: innerhalb einer Zeichenklasse ist der Strich kein Alternativoperator, sondern ein normales Zeichen — akzeptiert wird dadurch auch `CHF\|USD`. | `Counter.java:359` | Unbeabsichtigt tolerantes Eingabeformat; korrekt wäre eine Klasse nur aus Leerzeichen, Komma und Grösserzeichen. |
| R-07 | **`sc.close()` schliesst `System.in` endgültig.** Danach ist keine Konsoleneingabe mehr möglich, auch für andere `Counter`-Instanzen nicht. | `Counter.java:129` | Funktioniert hier nur, weil das Programm direkt danach endet — bei jeder Erweiterung ein Fehler. |
| R-08 | **Reihenfolge der Prüfungen ist unlogisch:** `accTo == accFrom` wird vor `accTo == null` geprüft. | `Counter.java:181-187` | Führt aktuell zu keinem Fehler, ist aber fragil — die `null`-Prüfung gehört zuerst. |

### 4.3  Sicherheit

| ID | Befund | Fundstelle | Warum das ein Problem ist |
|---|---|---|---|
| S-01 | **API-Schlüssel im Klartext im Quelltext** und damit in der Versionsverwaltung. | `ExchangeRateOkhttp.java:28` | Der Schlüssel ist für alle einsehbar und lässt sich nicht wechseln, ohne den Code zu ändern. Gehört in eine Umgebungsvariable oder eine nicht eingecheckte Konfigurationsdatei. Da er bereits veröffentlicht ist, sollte er ersetzt werden. |
| S-02 | **Veraltete Abhängigkeiten.** Gson `2.8.2` liegt vor Version 2.8.9, mit der CVE-2022-25647 behoben wurde; OkHttp wird in einer **Alpha**-Version (`5.0.0-alpha.11`) eingesetzt. | `pom.xml` | Bekannte Schwachstelle und instabile API. Auf aktuelle stabile Versionen heben. |
| S-03 | **Ungeprüfte Antwortverarbeitung.** `response.body().string()` ohne `null`-Prüfung und ohne Auswertung des HTTP-Status; ein Fehler-JSON ohne Feld `result` ergibt `null` bzw. `0.0`. | `ExchangeRateOkhttp.java:33-41` | `0.0` ist zugleich der Rückgabewert für "Fehler" und ein theoretisch gültiger Kurs — der Aufrufer kann beides nicht unterscheiden. Besser eine Exception oder `Optional<Double>`. |

### 4.4  Testbarkeit und Struktur — der wichtigste Punkt für dieses Modul

| ID | Befund | Fundstelle | Warum das ein Problem ist |
|---|---|---|---|
| V-01 | **Keine Trennung von Benutzeroberfläche und Fachlogik.** `Counter` liest per `Scanner(System.in)`, schreibt per `System.out` und enthält gleichzeitig Umrechnung, Buchung und Validierung. | `Counter.java:12`, `:18` und durchgehend | Kein Unit-Test kommt an die Logik heran, ohne die Konsole zu simulieren. Fachlogik in eigene, parameterbasierte Methoden ziehen (z.B. `TransferService.transfer(from, to, amount)`), `Counter` nur noch für Ein- und Ausgabe. |
| V-02 | **Statische Zähler ohne Reset.** `Account.counter` und `Counter.counterId` sind `static` und werden nur hochgezählt. | `Account.java:25`, `Counter.java:13` | Zerstört die Testisolation: Konto-Ids hängen davon ab, wie viele Tests vorher liefen — Tests werden reihenfolgeabhängig und schlagen scheinbar zufällig fehl. Die Id-Vergabe gehört als Instanzzustand in `Bank`. |
| V-03 | **`double` für Geldbeträge.** | `Account.java:29` und alle Beträge | Binäre Gleitkommazahlen stellen 0.10 nicht exakt dar; Salden driften bei vielen Buchungen. Für Geld `BigDecimal` oder eine Ganzzahl in Rappen verwenden. |
| V-04 | **Harte Abhängigkeit auf den Webservice.** `new ExchangeRateOkhttp()` wird direkt in der Methode erzeugt. | `Counter.java:373` | Nicht austauschbar, also nicht mockbar; jeder Test bräuchte echtes Internet. Über ein Interface injizieren (Thema Kapitel 4/5). |
| V-05 | **Zwei konkurrierende Umrechnungswege.** `convertCurrency()` rechnet mit fest verdrahteten Konstanten, während `ExchangeRateOkhttp` echte Kurse liefert. | `Counter.java:243-245` gegenüber `ExchangeRateOkhttp.java:23` | Überweisung und Kursanzeige liefern unterschiedliche Werte. **Im Testlauf belegt:** die API meldete `1 CHF = 1.232202 USD`, gebucht wird aber mit `RATIO_CHF_TO_USD = 0.9` — rund 27 % Abweichung. Eine Quelle festlegen. |
| V-06 | **Innere, nicht-statische Klassen.** `Counter.AccountExeption` und `ExchangeRateOkhttp.Rate` sind an eine Instanz der äusseren Klasse gebunden; `Rate` wird zusätzlich von Gson instanziiert, das mit inneren Klassen nur eingeschränkt umgeht. | `Counter.java:339`, `ExchangeRateOkhttp.java:44` | Beide gehören als `static` deklariert oder in eigene Dateien. |
| V-07 | **Toter Code und Build-Artefakte.** `Account.pseudoDeleteAccount()` wird nur in einem auskommentierten Block verwendet; im ZIP liegen zusätzlich kompilierte `target/classes/*.class`. | `Account.java:67`, `Bank.java:26-28` | Beides gehört nicht ins Repository (`.gitignore`). |
| V-08 | **Typo im Klassennamen:** `AccountExeption` statt `AccountException`. Ebenso liegt das Enum `Currency` in `Main.java` statt in einer eigenen Datei. | `Counter.java:339`, `Main.java:37` | Erschwert das Auffinden im Projekt; eine öffentliche Klasse pro Datei ist Java-Konvention. |
| V-09 | **Kein einziger Test und keine Test-Infrastruktur.** Weder `src/test/java` noch eine JUnit-Dependency. | `pom.xml` | Jede Änderung ist ein Blindflug. Erster Schritt: JUnit 5 einbinden und mit `Account.withdraw()` beginnen — der Methode aus Abschnitt 3 mit dem besten Verhältnis von Aufwand zu Risiko. |

---

## 5  Fazit

Die Software ist als Lernbeispiel gut lesbar, in ihrer heutigen Form aber kaum automatisiert testbar: Die gesamte
Fachlogik steckt in der Klasse, die auch die Konsole bedient. Für den Einstieg ins Unit-Testing eignen sich
`Account.withdraw()`, `Account.deposit()`, `Counter.convertCurrency()` und `Bank.getAccount()` — sie haben
Parameter und Rückgabewerte und brauchen keine Benutzereingabe.

Die drei dringendsten Korrekturen unabhängig vom Testen: die fehlende Prüfung auf positive Beträge (**F-01**), die
unvollständige Umrechnungsmatrix (**F-02**) und der API-Schlüssel im Quelltext (**S-01**).

Der Testlauf zeigt zudem, wie wirksam schon eine kleine, systematisch entworfene Testsuite ist: 24 Fälle, in
wenigen Minuten ausgeführt, haben acht echte Fehler aufgedeckt — darunter zwei, mit denen sich der Kontostand
beliebig manipulieren lässt (BB-05/BB-06), und zwei, die die Anwendung zum Absturz bringen (BB-18/BB-19). Keiner
dieser Fehler hätte sich beim blossen Durchklicken des Normalablaufs gezeigt; alle acht liegen an den Rändern:
negative Werte, leere Eingaben, unbekannte Währungen, nicht abgedeckte Währungspaare.
