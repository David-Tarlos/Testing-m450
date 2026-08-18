# Modul 450 – 1 Grundlagen

## Aufgabe 1 – Formen von Tests

Bekannte Testformen: Unit-Test, Integrationstest, System-/End-to-End-Test, Smoke-Test,
Regressionstest, Abnahmetest, Lasttest.

Drei Beispiele aus der Praxis und wie sie durchgeführt werden:

**Unit-Test** – Eine einzelne Methode wird isoliert getestet, z.B. `calculatePrice()` aus Aufgabe 3.
Man ruft sie mit festen Eingabewerten auf und vergleicht das Resultat mit dem erwarteten Wert.
Durchführung: automatisiert mit JUnit, läuft lokal in der IDE und bei jedem Build in der Pipeline.

**Integrationstest** – Getestet wird das Zusammenspiel mehrerer Komponenten, z.B. ob der Bestell-Service
die Bestellung korrekt in die Datenbank schreibt. Fehler treten hier meist an den Schnittstellen auf.
Durchführung: automatisiert, aber mit echter Test-Datenbank (z.B. Docker-Container), nach den Unit-Tests.

**End-to-End-Test** – Der ganze Ablauf wird über die Oberfläche getestet: einloggen → Artikel in den
Warenkorb → bestellen → Bestätigung erhalten.
Durchführung: automatisiert mit Selenium oder Playwright, die einen echten Browser fernsteuern.
Läuft auf einer Testumgebung, meist nachts, weil es langsam ist.

## Aufgabe 2 – SW-Fehler und SW-Mangel

**SW-Fehler:** Das Ist-Verhalten weicht vom spezifizierten Soll-Verhalten ab.
Beispiel: In Aufgabe 3 wird bei 5 Zusatzausstattungen laut Spezifikation 15% Rabatt verlangt,
die Software rechnet aber nur 10%.

**SW-Mangel:** Die Funktion läuft korrekt durch, aber ein Qualitätsmerkmal ist ungenügend.
Beispiel: Der Preis wird als `20899.999999998` statt `20'900.00` angezeigt – rechnerisch richtig,
aber unformatiert und für den Kunden unprofessionell.

Kurz: Fehler = falsches Ergebnis. Mangel = richtiges, aber schlechtes Ergebnis.

**Hoher Schaden:** In einem Banking-System schlagen Transaktionen fehl oder werden doppelt gebucht.
Folge: direkter finanzieller Schaden, tausende manuelle Korrekturen, Meldepflicht und Reputationsverlust.
Ein bekannter realer Fall ist Knight Capital (2012): ein fehlerhaftes Deployment kostete in 45 Minuten
440 Mio. USD, die Firma war danach faktisch pleite.

## Aufgabe 3 – Testtreiber für die Preisberechnung

Dateien: [`aufgabe3/Preisberechnung.java`](aufgabe3/Preisberechnung.java) (vorgegebener Code, unverändert)
und [`aufgabe3/TestTreiber.java`](aufgabe3/TestTreiber.java).

Ausführen:

```bash
cd 1-grundlagen/aufgabe3
javac *.java
java TestTreiber
```

Der Testtreiber ruft `calculatePrice()` mit sieben verschiedenen Eingabewerten auf und vergleicht das
Resultat mit dem Wert, den wir von Hand aus der Aufgabenstellung berechnet haben. Wichtig: das Soll kommt
aus der Spezifikation, nicht aus dem Code – sonst würde man nur bestätigen, dass der Code macht, was er macht.

Testergebnis:

```
[OK  ] nur Grundpreis                   Soll= 20000.00  Ist= 20000.00
[OK  ] Grundpreis, 10% Haendlerrabatt   Soll= 18000.00  Ist= 18000.00
[OK  ] Grundpreis + Sondermodell        Soll= 22500.00  Ist= 22500.00
[OK  ] 2 Extras -> 0% Rabatt            Soll= 21000.00  Ist= 21000.00
[OK  ] 3 Extras -> 10% Rabatt           Soll= 20900.00  Ist= 20900.00
[FAIL] 5 Extras -> 15% Rabatt           Soll= 20850.00  Ist= 20900.00
[FAIL] 3 Extras + 20% Haendlerrabatt    Soll= 16900.00  Ist= 16800.00
```

### Gefundene Fehler

**1. Der 15%-Rabatt wird nie gewährt.**

```java
if (extras >= 3)
    addon_discount = 10;
else if (extras >= 5)   // wird nie erreicht
    addon_discount = 15;
```

Bei 5 Extras ist schon `extras >= 3` wahr, der zweite Zweig wird gar nie ausgewertet. Der Kunde bekommt
immer nur 10%. Korrektur: die grössere Bedingung zuerst prüfen (`>= 5`, dann `>= 3`).

**2. Der Händlerrabatt wird auf das Zubehör übertragen.**

```java
if (discount > addon_discount)
    addon_discount = discount;
```

Laut Aufgabenstellung gilt der Händlerrabatt nur auf den Grundpreis. Diese Zeile überschreibt aber den
Zubehörrabatt, sobald der Händlerrabatt höher ist. Ob das ein Code-Fehler oder eine unklare Spezifikation
ist, müsste man mit dem Auftraggeber klären.
