# Übung 1 — Rabattregeln: abstrakte und konkrete Testfälle

> **Aufgabenstellung**
>
> Wir haben folgende Beschreibung einer Verkaufssoftware:
>
> *Über die Verkaufssoftware kann das Autohaus seinen Verkäufern Rabattregeln vorgeben: Bei einem Kaufpreis von weniger
> als 15'000 CHF soll kein Rabatt gewährt werden. Bei einem Preis bis zu 20'000 CHF sind 5% Rabatt angemessen. Liegt der
> Kaufpreis unter 25'000 CHF sind 7% Rabatt möglich, darüber sind 8,5 % Rabatt zu gewähren.*
>
> Leiten Sie aus dieser Beschreibung Testfälle ab — eine Tabelle mit **abstrakten** Testfällen (logische Operatoren)
> und eine Tabelle mit **konkreten** Testfällen (konkrete Eingabewerte).

## Testobjekt

Die Komponente der Verkaufssoftware, welche aus einem Kaufpreis den Rabattsatz und daraus den Endpreis berechnet.
Alle Testfälle sind **funktionale** Testfälle: sie prüfen, *was* die Software rechnet, nicht *wie schnell* sie es tut.

---

## 1  Analyse der Anforderung — offene Punkte und getroffene Annahmen

Die Anforderung ist umgangssprachlich formuliert und an mehreren Stellen nicht eindeutig. Das ist der wichtigste
Teil der Aufgabe: Testfälle lassen sich erst schreiben, wenn die Lücken benannt und die Annahmen festgehalten sind.
In einem realen Projekt gehen diese Punkte als Rückfragen an den Kunden.

| # | Problem in der Anforderung | Getroffene Annahme für diese Testfälle |
|---|---|---|
| 1 | **Lücke bei genau 25'000.—** „unter 25'000 → 7%", „darüber → 8,5%". Der Wert 25'000 selbst ist weder „unter" noch „darüber" und fällt damit durch keine der beiden Regeln. | `preis >= 25'000` → 8,5% |
| 2 | **Überlappung der Regeln.** „Liegt der Kaufpreis unter 25'000, sind 7% möglich" deckt wörtlich gelesen auch 16'000 ab und widerspricht damit der 5%-Regel. | Die Regeln werden **kaskadierend** in der genannten Reihenfolge gelesen: die erste zutreffende Regel gewinnt. |
| 3 | **Genau 15'000.— / genau 20'000.—** „weniger als 15'000" schliesst 15'000 aus, „bis zu 20'000" schliesst 20'000 ein. | `15'000 <= preis <= 20'000` → 5% |
| 4 | **Kann- statt Muss-Formulierung.** „sind 5% angemessen", „sind 7% möglich" gegenüber „sind 8,5% zu gewähren". | Alle vier Sätze sind fix, kein Ermessensspielraum des Verkäufers. |
| 5 | **Rundung nicht spezifiziert.** 24'999.95 × 7% = 1'749.9965 CHF — ein nicht zahlbarer Betrag. | Kaufmännische Rundung auf **0.05 CHF** (kleinste CHF-Einheit). Alle Eingabewerte liegen ebenfalls auf dem 5-Rappen-Raster. |
| 6 | **Gültigkeitsbereich fehlt.** Zu Preis 0, negativen Preisen und nicht-numerischen Eingaben sagt die Anforderung nichts. | Ungültige Eingabe → Fehlermeldung, **kein** Rabatt und **keine** Berechnung. |

---

## 2  Tabelle A — Abstrakte Testfälle

Ohne konkrete Werte, nur mit logischen Operatoren. `preis` = Kaufpreis in CHF.

| ID | Äquivalenzklasse | Bedingung (abstrakt) | Erwarteter Rabattsatz | Erwarteter Endpreis |
|----|------------------|----------------------|-----------------------|---------------------|
| A1 | ÄK1 — kein Rabatt | `0 < preis < 15'000` | 0 % | `preis` |
| A2 | ÄK2 — kleiner Rabatt | `15'000 <= preis <= 20'000` | 5 % | `preis − preis × 0.05` |
| A3 | ÄK3 — mittlerer Rabatt | `20'000 < preis < 25'000` | 7 % | `preis − preis × 0.07` |
| A4 | ÄK4 — grosser Rabatt | `preis >= 25'000` | 8,5 % | `preis − preis × 0.085` |
| A5 | ÄK5 — ungültig | `preis <= 0` ODER `preis` nicht numerisch | — (Fehlermeldung) | keine Berechnung |

ÄK1–ÄK4 sind **gültige** Äquivalenzklassen, ÄK5 ist die **ungültige** Klasse. Die Klassen sind überschneidungsfrei
und decken zusammen den gesamten Wertebereich ab — genau das ist das Ziel der Äquivalenzklassenbildung.

---

## 3  Tabelle B — Konkrete Testfälle

Pro Äquivalenzklasse wird der untere Rand, ein Wert aus der Mitte und der obere Rand geprüft
(**Grenzwertanalyse**), dazu die ungültigen Fälle. Beträge in CHF, Rundung auf 0.05 CHF.

| ID | Eingabe (Kaufpreis) | Erwarteter Rabattsatz | Erwarteter Rabattbetrag | Erwarteter Endpreis | ÄK | Zweck des Testfalls |
|----|---------------------|-----------------------|-------------------------|---------------------|-----|---------------------|
| K1 | 1.00 | 0 % | 0.00 | 1.00 | ÄK1 | unterer Rand gültiger Bereich |
| K2 | 9'000.00 | 0 % | 0.00 | 9'000.00 | ÄK1 | Normalfall Mitte |
| K3 | 14'999.95 | 0 % | 0.00 | 14'999.95 | ÄK1 | **Grenzwert:** letzter Preis ohne Rabatt |
| K4 | 15'000.00 | 5 % | 750.00 | 14'250.00 | ÄK2 | **Grenzwert:** erster Preis mit 5% |
| K5 | 15'000.05 | 5 % | 750.00 | 14'250.05 | ÄK2 | direkt nach der Grenze |
| K6 | 18'000.00 | 5 % | 900.00 | 17'100.00 | ÄK2 | Normalfall Mitte |
| K7 | 19'999.95 | 5 % | 1'000.00 | 18'999.95 | ÄK2 | direkt vor der Grenze |
| K8 | 20'000.00 | 5 % | 1'000.00 | 19'000.00 | ÄK2 | **Grenzwert:** „bis zu 20'000" ist inklusiv |
| K9 | 20'000.05 | 7 % | 1'400.00 | 18'600.05 | ÄK3 | **Grenzwert:** erster Preis mit 7% |
| K10 | 22'500.00 | 7 % | 1'575.00 | 20'925.00 | ÄK3 | Normalfall Mitte |
| K11 | 24'999.95 | 7 % | 1'750.00 | 23'249.95 | ÄK3 | **Grenzwert:** letzter Preis mit 7% |
| K12 | 25'000.00 | 8,5 % | 2'125.00 | 22'875.00 | ÄK4 | **Grenzwert + Annahme 1:** strittiger Wert |
| K13 | 25'000.05 | 8,5 % | 2'125.00 | 22'875.05 | ÄK4 | direkt nach der Grenze |
| K14 | 40'000.00 | 8,5 % | 3'400.00 | 36'600.00 | ÄK4 | Normalfall, offener oberer Bereich |
| K15 | 0.00 | — | — | Fehlermeldung | ÄK5 | ungültig: Nullpreis |
| K16 | −100.00 | — | — | Fehlermeldung | ÄK5 | ungültig: negativer Preis |
| K17 | `abc` | — | — | Fehlermeldung | ÄK5 | ungültig: nicht numerisch |
| K18 | *(leere Eingabe)* | — | — | Fehlermeldung | ÄK5 | ungültig: Pflichtfeld leer |

Alle fünf Äquivalenzklassen sind abgedeckt: ÄK1 in K1–K3, ÄK2 in K4–K8, ÄK3 in K9–K11, ÄK4 in K12–K14, ÄK5 in K15–K18.

### Kontrollrechnung für zwei Werte

* **K8:** 20'000.00 × 5 % = 1'000.00 → 20'000.00 − 1'000.00 = **19'000.00**
* **K11:** 24'999.95 × 7 % = 1'749.9965 → gerundet auf 0.05 = 1'750.00 → 24'999.95 − 1'750.00 = **23'249.95**

---

## 4  Warum gerade diese Werte?

Die drei Grenzen 15'000 / 20'000 / 25'000 sind die fehlerträchtigsten Stellen der ganzen Komponente. Fast jeder
Fehler in solcher Preislogik ist eine Verwechslung von `<` und `<=` bzw. ein um eine Einheit verschobener Vergleich
(*off-by-one*). Ein Testfall mitten in der Klasse (z.B. 18'000) findet so einen Fehler **nie** — er fällt nur auf,
wenn genau der Wert direkt vor der Grenze, die Grenze selbst und der Wert direkt danach geprüft werden.
Deshalb steht um jede Grenze ein Tripel: K3/K4/K5, K7/K8/K9 und K11/K12/K13.

Testfall **K12** ist zusätzlich der Nachweis für Annahme 1: schlägt er fehl, ist nicht der Code falsch, sondern die
Anforderung war an dieser Stelle nie entschieden — und muss vom Kunden geklärt werden.
