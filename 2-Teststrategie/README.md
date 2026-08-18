# Welche Elemente braucht es für eine Teststrategie?

> Kapitel 2 des Moduls 450 — Testen von Software (TBZ)
>
> **Lösungen zu den Übungen:**
> - [Übung 1 — Rabattregeln: abstrakte und konkrete Testfälle](uebung-1-rabattregeln.md)
> - [Übung 2 — Autovermietung: funktionale Black-Box-Testfälle](uebung-2-autovermietung-blackbox.md)
> - [Übung 3 — Bank-Software: Black-Box, White-Box und Code-Review](uebung-3-bank-software.md)

## Lernziele

* Ich kenne die Elemente einer Teststrategie
* Ich kann den Unterschied zwischen abstrakten und konkreten Testfällen erklären
* Ich kenne den Unterschied zwischen funktionalen und nicht-funktionalen Testfällen und kann Beispiele nennen
* Ich kann Testmethoden erklären

---

## Teststrategie und Planung

Um eine Software zu testen, braucht es eine klare Strategie. Das Testen erfolgt nicht ohne Plan. Bei der Entwicklung einer Software wird auch ein Ablauf oder ein Prozess festgelegt (z.Bsp. wird die Software nach einer agilen Methode wie SCRUM umgesetzt). Dasselbe gilt auch für das Testen.

Teststrategie und Testkonzept gehen Hand in Hand. Eine Strategie beschreibt, wie vorgegangen wird beim Testen.

Dabei wird ein Testkonzept erstellt, wo die wichtigsten Elemente für das Testen festgelegt werden. Im Moment beschränken wir uns hier auf die elementaren Faktoren für eine Strategie:

* Testobjekte mit einer kurzen Auflistung, aus welchen Teilen die Software besteht
* Auflistung, was genau getestet wird (welche Funktionen zu testen sind und welche nicht getestet werden)
* Testmethode auswählen
* Welche Testinfrastruktur verwendet wird

Es gibt noch weitere Kriterien für eine Strategie (wie z.Bsp. Testmetriken definieren oder wie die Tests dokumentiert werden), wir wollen uns aber hier auf diese vier Punkte konzentrieren.

In diesem Abschnitt erklären wir die Themen nur kurz. In den folgenden Kapiteln im Modul wird dann jedes Thema im Detail genauer erklärt.

### 1  Testobjekte

Hier handelt es sich um die Komponente oder den Teil der Applikation, welcher getestet wird. Typische Testobjekte sind Programmmodule oder Units (z.Bsp. eine Klasse). Aber auch Kommandozeilenskripte des Betriebssystems, DB-Skripte oder Datenkonvertierungsprozeduren können Testobjekte sein.

Z.Bsp. bei einer Software für eine Autovermietung wollen wir nur die Komponente testen, welche dem Kunden eine Rechnung nach der Vermietung erstellt.

### 2  Was wird genau getestet – die Testfälle

Sobald wir die Testobjekte haben, können wir Testfälle erstellen. Testfälle sind quasi das "Herzstück" des Testkonzepts. Nur mit Testfällen können wir überhaupt eine Applikation konkret testen.

Dabei wird grundsätzlich zwischen funktionalen und nicht-funktionalen Testfällen unterschieden:

**Funktionale Testfälle**

Beschreiben die Funktionen einer Software, welche vom Kunden ursprünglich als Anforderung definiert worden sind. Das *"was"* die Software tut, wird getestet.

**Nicht-funktionale Testfälle**

Hier listet man Testfälle auf, welche nicht generell mit der Applikation zu tun haben. Ein Beispiel wäre die Performance einer Applikation (z.Bsp. wie schnell eine Berechnung ausgeführt wird oder wie rasch auf Daten zugegriffen wird). Das *"wie"* eine Software es tut, wird getestet.

> **Wir werden uns in diesem Modul nur auf funktionales Testing beschränken.**

**Abstrakte Testfälle**

Hier haben wir keine konkreten Ein- und Ausgabewerte, sondern wir verwenden logische Operatoren (wie grösser oder kleiner, etc.) für die Beschreibung.

**Konkrete Testfälle**

Hier arbeiten wir mit konkreten Wertangaben für die Ein- und Ausgabe.

Sehr oft können wir die Testfälle aus den Anforderungen einer Software ableiten. Die Anforderungen werden zu Beginn eines Projekts definiert, damit wir als Programmierer wissen, was die Applikation alles umfassen soll.

### 3  Testmethoden auswählen

Mit Testmethoden ist das *WIE* beim Testen gemeint. Welche Art von Test führen wir durch? Hier legen wir fest, wie wir testen.

Sind es **Black-Box-** oder **White-Box-**Testfälle? Werden die Tests **automatisch** durchgeführt oder braucht es hier Benutzer, welche die Tests **manuell** durchführen?

**White-Box-Testfälle**

White-Box-Testfälle werden üblicherweise auf der Unit-Testebene ausgeführt. Wir werden dieses Thema (Unit-Testing) später noch genauer anschauen. Der Code ist sichtbar und der Code kann explizit durchgetestet werden. (Stichwort: Code-Pfad-Testing)

**Black-Box-Testfälle**

Hier kennen wir den Code nicht, bzw. der Code ist bereits kompiliert (und deployed) und somit nicht sichtbar. Der innere Aufbau des Testobjekts ist nicht bekannt. Üblicherweise werden diese Tests dann von einem Testteam (Benutzer etc.) durchgeführt und dokumentiert.

**Automatisierung von Testfällen**

Das Thema Automatisierung werden wir später in diesem Modul im Detail behandeln.

### 4  Testumgebung festlegen

Wenn wir unsere Testmethoden kennen, können wir auch besser bestimmen, mit welchen Tools wir arbeiten wollen. Auch dieses Thema werden wir später genauer anschauen.
