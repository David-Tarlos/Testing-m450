# Welche Elemente braucht es für ein Testkonzept?

> Kapitel 6 des Moduls 450 — Testen von Software (TBZ)
>
> **Lösung zur Übung:** [Testkonzept — Student-Verwaltung](testkonzept.md)
>
> *Hinweis: Die Bilder des Original-Kapitels (`x_gitres/*.png`) sind hier nicht
> eingebunden, damit keine kaputten Bildverweise entstehen. Der Abschnitt
> „Elemente eines Testplans" ersetzt die entfallene Grafik durch eine eigene
> Auflistung der im Text erklärten Abschnitte.*

<!-- TOC -->
- [Welche Elemente braucht es für ein Testkonzept?](#welche-elemente-braucht-es-für-ein-testkonzept)
  - [Lernziele](#lernziele)
  - [Teststrategie und Testkonzept](#teststrategie-und-testkonzept)
  - [Testkonzept nach IEEE 829](#testkonzept-nach-ieee-829)
    - [Introduction](#introduction)
    - [Test Items](#test-items)
    - [Features to be tested](#features-to-be-tested)
    - [Features not to be tested](#features-not-to-be-tested)
    - [Approach](#approach)
    - [Item pass / fail criteria](#item-pass--fail-criteria)
    - [Test Deliverables](#test-deliverables)
    - [Testing Tasks](#testing-tasks)
    - [Environmental Needs](#environmental-needs)
    - [Schedule](#schedule)
    - [Weitere Elemente](#weitere-elemente)
  - [Quellen](#quellen)
<!-- TOC -->

## Lernziele

* Ich kenne die wichtigsten Elemente eines Testkonzepts
* Ich kann Test Items für mein Projekt identifizieren
* Ich kann aus Test Items die Test Features ableiten
* Ich kann geeignete Testmethoden auswählen für mein Projekt

---

## Teststrategie und Testkonzept

Wie wir bereits gesehen haben, ist eine [Teststrategie](../2-Teststrategie/README.md) in einem Projekt eine wichtige
Voraussetzung für erfolgreiches Testen.

Teststrategie und ein Testkonzept gehen Hand in Hand. Eine Strategie beschreibt, wie vorgegangen wird beim Testen
(**Approach**). Dies wird in einem Testkonzept definiert. Dieses Konzept bildet quasi den „Masterplan" für das Testen.
Ein Testkonzept wird häufig auch *Testplan* genannt.

Wir werden uns im Folgenden auf den **Standard IEEE 829** beziehen, der eine detaillierte Auflistung aller
notwendigen Elemente für ein Konzept enthält.

### Elemente eines Testplans

> *Eigene Zusammenstellung anstelle der entfallenen Grafik — sie listet genau die
> Abschnitte auf, die weiter unten im Kapitel erklärt werden.*

| # | Element | Beantwortet die Frage |
|---|---|---|
| 1 | Introduction | Um welche Applikation geht es? |
| 2 | Test Items | Welche Bestandteile hat sie? |
| 3 | Features to be tested | Was wird getestet? |
| 4 | Features not to be tested | Was wird bewusst nicht getestet? |
| 5 | Approach | Wie wird getestet? |
| 6 | Item pass / fail criteria | Wann gilt ein Test als bestanden? |
| 7 | Test Deliverables | Welche Dokumente und Werkzeuge entstehen? |
| 8 | Testing Tasks | Welche Teststufen gibt es? |
| 9 | Environmental Needs | Welche Umgebung braucht es dafür? |
| 10 | Schedule | Wann läuft was? |
| 11 | Responsibilities, Staffing & Training, Approvals | Wer macht es und wer nimmt ab? |

## Testkonzept nach IEEE 829

Der IEEE 829 Standard ist eine **Empfehlung** für ein Testkonzept. Es mag etwas verstaubt und bürokratisch
daherkommen, ist aber immer noch eine sehr nützliche Richtlinie, wenn es darum geht, ein Testkonzept zu schreiben.
Man kann sich den IEEE 829 auch als **Checkliste** für die Elemente eines Konzepts vorstellen. Dabei müssen nicht
zwingend alle Elemente verwendet werden. Wir erklären hier die wichtigsten Elemente des Konzepts.
*Einen Teil dieser Elemente werden Sie dann direkt für Ihr Projekt anwenden.*

### Introduction

Hier wird die Applikation kurz beschrieben, damit alle wissen, um was für eine Applikation es sich handelt.

### Test Items

Hier werden die zu testenden Elemente (Items) aufgelistet und beschrieben. Dabei wird in einem *Big Picture* die
Applikation aufgezeigt. Sehr hilfreich ist es, wenn man eine **Skizze der Architektur** hat (ein Diagramm, welches
die einzelnen Komponenten darstellt). Somit wird klar, welche Elemente getestet werden und welche nicht.

### Features to be tested

Nun können die Test Items in einzelne Features (Funktionalitäten) heruntergebrochen werden. Hier wird eine
detaillierte Liste der Funktionalitäten erstellt, die wir testen.

### Features not to be tested

Hier beschreiben wir, welche Funktionalitäten nicht getestet werden. Dies können z.Bsp. nicht-funktionale Aspekte
der Software sein (Performance, etc.).

### Approach

Hier wird beschrieben, wie getestet wird. Wir beschreiben unsere Testmethode.

Ein Beispiel:
*Im Entwicklerteam (Dev Team) werden Komponententests ausgeführt durch Unit Tests. Das Test Vorgehen wird durch TDD
umgesetzt (Test Driven Development).*

### Item pass / fail criteria

Hier werden die Bedingungen festgelegt für erfolgreiche und nicht-erfolgreiche Tests.
Wann gilt ein Test als erfolgreich? Wann gilt ein Test als fehlgeschlagen?

Hier werden die Fehler detailliert klassifiziert und beschrieben. So gibt es z.Bsp.

* Geringfügige Fehler (die Applikation läuft, hat aber gewisse Mängel)
* Mittelschwere Fehler (die Applikation hat offensichtliche Fehler)
* Schwerwiegende Fehler (die Applikation stürzt ab)

### Test Deliverables

Hier werden die **Test-Artefakte** beschrieben. Unter Test-Artefakte versteht man Dokumente oder Tools, die für die
Tests bereitgestellt und verwendet werden. Ein Test-Artefakt wäre z.Bsp. das Testkonzept selber. Ein Test-Werkzeug
wäre z.Bsp. Postman, um die REST-Schnittstelle zu testen.

### Testing Tasks

Hier werden die Stufen der Tests beschrieben. Welche Stufen werden getestet? Sind es nur Unit-Tests oder gibt es
weitere Stufen, wie z.Bsp. Integrationstests?

### Environmental Needs

Hier werden die Testumgebungen (Hardware und Software) beschrieben, welche es für das Testing braucht.

### Schedule

Für jedes Vorgehen braucht es einen entsprechenden *Zeitplan*. Hier wird aufgelistet, wann welche Tests erfolgen.
Wenn z.Bsp. nach dem TDD-Ansatz entwickelt wird, erfolgen zuerst die Unit-Tests. Die Integrationstests werden erst
durchgeführt, wenn alle Komponenten einen gewissen Entwicklungsstand haben.

### Weitere Elemente

Der IEEE 829 beschreibt noch weitere Elemente für das Testkonzept. Diese beziehen sich aber mehr auf
organisatorische Aspekte im Projekt. So gibt es eine Beschreibung zu den Verantwortlichkeiten
(**Responsibilities**), zum Personal (**Staffing & Training**) und zu den Abnahmekriterien (**Approvals**).

---

## Quellen

* https://en.wikipedia.org/wiki/Test_plan
* https://www.informatik-aktuell.de/entwicklung/methoden/das-perfekte-testkonzept-in-6-schritten.html
