# Übung 2 — Autovermietung: funktionale Black-Box-Testfälle

> **Aufgabenstellung**
>
> Suchen Sie sich eine Webseite zum Thema **Autovermietung**. Definieren Sie *funktionale Black-Box-Tests*, die Sie
> brauchen, um diese Plattform zu betreiben. *Listen Sie die 5 wichtigsten Testfälle auf* und erstellen Sie eine
> Tabelle mit diesen Testfällen als Markdown.

## Testobjekt

**Plattform:** [europcar.ch](https://www.europcar.ch) — Online-Buchungsstrecke für Mietfahrzeuge.

Gewählt, weil die Plattform den kompletten Miet-Ablauf ohne Kundenkonto durchlaufen lässt: Station und Zeitraum
wählen → Fahrzeugkategorie wählen → Zusatzoptionen → Buchungsformular. Damit lassen sich Suche, Validierung,
Preisberechnung und Formularprüfung an einer einzigen Anwendung testen.

**Warum sind das Black-Box-Testfälle?**
Uns liegt kein Quelltext vor — die Anwendung ist kompiliert, deployed und läuft auf fremden Servern. Der innere
Aufbau ist unbekannt. Getestet wird deshalb ausschliesslich über die Benutzeroberfläche: definierte Eingabe →
beobachtbare Ausgabe. Alle fünf Fälle sind ausserdem **funktional** (das *was*), nicht nicht-funktional — Ladezeiten
oder Serverlast prüfen wir hier bewusst nicht.

## Testdurchführung

Alle fünf Testfälle wurden am **18.08.2026** tatsächlich ausgeführt; die eingetragenen Resultate sind beobachtet,
nicht angenommen.

**Testumgebung:** Google Chrome unter Windows 11 · Sprachversion `europcar.ch/de-ch` · Cookie-Banner mit
„Weiter ohne Zustimmung" geschlossen (keine optionalen Cookies) · nicht eingeloggt, ohne Kundenkonto.

**Testdaten:** Station Zürich Kloten Flughafen ZRH (`ZRHT01`), Abholung 19.08.2026 10:15,
Rückgabe 22.08.2026 10:00 (3 Miettage), Fahreralter 26+, Wohnsitz Schweiz.
Referenzfahrzeug für alle Preisvergleiche: **Volkswagen Golf, CHF 57.08/Tag, Gesamtpreis CHF 171.23.**

**Abgrenzung:** Es wurde **keine** Buchung abgeschlossen und keine Zahlung ausgelöst. Im Buchungsformular kamen
nur offensichtliche Testdaten zum Einsatz (Max Mustermann, 01.01.1990, Telefon 079 123 45 67); die Häkchen für
Datenschutzerklärung und AVB blieben bewusst ungesetzt.

---

## Die 5 wichtigsten Testfälle

### Vorbedingungen und Testschritte

**TF-01 — Verfügbarkeitssuche (Gutfall)**
*Vorbedingung:* Startseite geöffnet, keine Buchung im Warenkorb.
1. Station „Zürich Flughafen" als Abhol- und Rückgabeort wählen.
2. Abholung: morgen, 10:00 Uhr. Rückgabe: in 3 Tagen, 10:00 Uhr.
3. Suche auslösen.

**TF-02 — Rückgabedatum vor Abholdatum (Fehlerfall)**
*Vorbedingung:* Startseite geöffnet.
1. Station „Zürich Flughafen" wählen.
2. Abholung: in 5 Tagen, 10:00 Uhr. Rückgabe: in 2 Tagen, 10:00 Uhr.
3. Suche auslösen.

**TF-03 — Preisberechnung mit Zusatzoption**
*Vorbedingung:* TF-01 erfolgreich, eine Fahrzeugkategorie ist gewählt, Gesamtpreis notiert.
1. Auf der Optionsseite den ausgewiesenen Preis einer Zusatzoption (z.B. Kindersitz oder Zusatzfahrer) notieren.
2. Option hinzufügen.
3. Neuen Gesamtpreis mit dem notierten Ausgangspreis vergleichen.

**TF-04 — Fahreralter unter Mindestalter**
*Vorbedingung:* Startseite geöffnet.
1. Station und gültigen Zeitraum wie in TF-01 wählen.
2. Als Fahreralter einen Wert unter dem Mindestalter angeben (z.B. 19).
3. Suche auslösen.

**TF-05 — Pflichtfeldprüfung im Buchungsformular**
*Vorbedingung:* Fahrzeug und Optionen gewählt, Buchungsformular geöffnet.
1. Alle Felder ausser der E-Mail-Adresse korrekt ausfüllen.
2. Formular absenden.

### Testfalltabelle

| ID | Beschreibung | Erwartetes Resultat | Effektives Resultat | Status | Mögliche Ursache |
|----|--------------|---------------------|---------------------|--------|------------------|
| TF-01 | Verfügbarkeitssuche mit gültiger Station und gültigem Zeitraum (19.08. 10:15 → 22.08. 10:00) | Ergebnisseite zeigt eine Liste verfügbarer Fahrzeugkategorien mit Preis pro Kategorie; gewählte Station und Zeitraum werden korrekt wiederholt angezeigt | Trefferliste erscheint mit mehreren Kategorien (VW Golf CHF 57.08/Tag, Gesamt CHF 171.23; Seat Cupra Born CHF 48.65/Tag, Gesamt CHF 145.94). Kopfzeile wiederholt „Zürich Kloten Flughafen…", 2026-08-19 10:15 und 2026-08-22 10:00 korrekt | OK | – |
| TF-02 | Suche mit Rückgabedatum **vor** dem Abholdatum (Abholung 25.08., Rückgabe 21.08.) | Eingabe wird abgewiesen: Validierungsmeldung direkt beim Datumsfeld, es wird **keine** Ergebnisliste geladen | Der ungültige Zeitraum lässt sich nicht herstellen und **keine** Ergebnisliste wird geladen — aber ohne jede Meldung: Beim Klick auf den 21.08. übernimmt die Seite diesen als Rückgabedatum und **löscht stillschweigend das bereits gesetzte Abholdatum**. Ein anschliessender Klick auf „Suche" bleibt wirkungslos, der Kalender öffnet sich nur erneut | Abweichung | Die Schutzwirkung ist da, die geforderte Rückmeldung fehlt: kein Hinweistext, und die zuvor getätigte Eingabe geht kommentarlos verloren |
| TF-03 | Zusatzoption Kindersitz 9–18 kg (CHF 69.50) zur Buchung hinzufügen | Gesamtpreis erhöht sich **exakt** um den für die Option ausgewiesenen Betrag; die Option erscheint in der Buchungsübersicht | Gesamtpreis stieg von CHF 171.23 auf **CHF 240.73**, also exakt +69.50 ohne Rundungsabweichung. Kopfzeile zeigt „1 Extra CHF 69.50", die Karte wird als „HINZUGEFÜGT" markiert, die Buchungsübersicht listet „Kindersitz 9-18 kg, für 3 Tag(e), 1, CHF 69.50" | OK | – |
| TF-04 | Fahreralter unter dem Mindestalter angeben (19 Jahre) | Anwendung reagiert nachvollziehbar: Hinweis auf das Mindestalter, Ausschluss der nicht zulässigen Fahrzeugkategorien **oder** Ausweis eines Jungfahrer-Zuschlags — in keinem Fall stiller Normalpreis | Hinweisbanner „Fahrzeugverfügbarkeit und Aufpreis abhängig vom Alter" erscheint über der Trefferliste; alle Preise steigen um einen konstanten Zuschlag von CHF 59.70 auf die Mietdauer (Golf CHF 76.98/Tag, Gesamt CHF 230.93; Cupra CHF 68.55/Tag, Gesamt CHF 205.64) | OK | – |
| TF-05 | Buchungsformular ohne E-Mail-Adresse absenden (Vorname, Nachname, Geburtsdatum, Telefon gefüllt) | Absenden wird verhindert, Fehlermeldung direkt beim E-Mail-Feld; bereits eingegebene Daten bleiben erhalten | Klick auf „Zur Zahlung" führt **nicht** weiter (URL unverändert). Das E-Mail-Feld wird rot umrandet, darunter erscheint „Bitte geben Sie Ihre Mail-Adresse ein", die Seite scrollt automatisch zum Fehler. Max / Mustermann / 01/01/1990 bleiben vollständig erhalten | OK | – |

**Status-Werte:** `OK` · `Fehler` · `Abweichung` (verhindert das Problem, aber nicht wie spezifiziert) · `offen`

### Ergebnis

**4 × OK · 1 × Abweichung · 0 × Fehler**

Die Plattform verhält sich in den Kernfunktionen korrekt: Suche, Preisberechnung, Altersregel und Pflichtfeldprüfung
arbeiten sauber und melden Fehler dort, wo sie entstehen. TF-03 und TF-05 sind besonders sauber gelöst — der
Optionspreis wird auf den Rappen genau addiert, und die Formularvalidierung erhält die bereits eingegebenen Daten.

Einzig **TF-02** weicht ab. Sicherheitskritisch ist das nicht — ein ungültiger Mietzeitraum lässt sich nicht
abschicken. Aus Benutzersicht ist es trotzdem ein Mangel: Wer ein früheres Rückgabedatum anklickt, verliert sein
Abholdatum ohne Vorwarnung und erfährt nicht, warum die Suche nicht startet. Eine Meldung wie „Das Rückgabedatum
muss nach dem Abholdatum liegen" würde genügen.

**Nebenbefund aus TF-01/TF-03/TF-04:** Tagespreis × Miettage ergibt durchgängig einen Rappen mehr als der
ausgewiesene Gesamtpreis (57.08 × 3 = 171.24 gegenüber CHF 171.23; ebenso 48.65 × 3 und 76.98 × 3). Der
Tagespreis ist offensichtlich der aufgerundete Gesamtpreis geteilt durch die Miettage. Verrechnet wird der
Gesamtpreis, ein echter Fehler ist es also nicht — für einen aufmerksamen Kunden wirkt es dennoch inkonsistent.

**Methodischer Hinweis zu TF-04:** Das Alters-Dropdown der Startseite ist ein natives Auswahlfeld, dessen Liste
sich im Testwerkzeug nicht bedienen liess. Der Testfall wurde deshalb über den Suchparameter `driverAge=19`
in der Ergebnis-URL ausgeführt — bei sonst identischen Suchdaten, sodass der Preisvergleich mit TF-01 gültig bleibt.

---

## Begründung der Auswahl

Die fünf Fälle decken bewusst unterschiedliche Risikoarten ab statt fünfmal dieselbe Funktion:

| Testfall | Abgedecktes Risiko |
|----------|--------------------|
| TF-01 | **Kernfunktion.** Ohne funktionierende Suche ist die Plattform wertlos — der wichtigste Gutfall. |
| TF-02 | **Eingabevalidierung / Grenzwert.** Klassische Fehlerquelle bei Datumslogik (Zeitraum-Umkehr). |
| TF-03 | **Berechnungslogik.** Preisfehler haben direkte finanzielle Folgen für Kunde und Betreiber. |
| TF-04 | **Geschäftsregel.** Mindestalter ist eine vertragliche Vorgabe, kein technisches Detail. |
| TF-05 | **Datenqualität.** Ohne E-Mail-Adresse ist die Buchung nicht bestätigbar — der Fehler fällt sonst erst nach Vertragsabschluss auf. |

Jeweils ein Gutfall (TF-01, TF-03) und vier Fehler- bzw. Sonderfälle: Fehler zeigen sich in der Praxis fast immer
an den Rändern, nicht im Normalablauf.
