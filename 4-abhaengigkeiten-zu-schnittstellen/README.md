# Schnittstellen

> Kapitel 4 des Moduls 450 — Testen von Software (TBZ)
>
> **Lösung zu den Übungen:** [Aufgabe 1 und 2 — addressbook-backend](uebung-addressbook.md)
>
> *Hinweis: Die Diagramme des Original-Kapitels (`x_gitres/*.png`) sind hier nicht
> eingebunden, damit keine kaputten Bildverweise entstehen.*

<!-- TOC -->
* [Schnittstellen](#schnittstellen)
  * [Glossar](#glossar)
  * [Lernziel](#lernziel)
  * [Einführung](#einführung)
  * [Test Doubles](#test-doubles)
    * [Lifecycle](#lifecycle)
  * [Mock, Spy, Stub, Fake, Dummy](#mock-spy-stub-fake-dummy)
    * [Mock](#mock)
    * [Spy](#spy)
    * [Dummy](#dummy)
    * [Stub](#stub)
    * [Fake](#fake)
  * [Mockito — alles in einem](#mockito--alles-in-einem)
  * [Sonstige Use Cases](#sonstige-use-cases)
  * [Source](#source)
  * [Checkpoint](#checkpoint)
<!-- TOC -->

---

## Glossar

| Abkürzung | Erklärung         |
|-----------|-------------------|
| SUT       | System under test |

## Lernziel

* Ich kenne Test Doubles
* Ich weiss, wo Test Doubles zum Einsatz kommen
* Ich kenne ein Test Framework, welches Test Doubles verwendet

---

## Einführung

Früher oder später trifft man bei Unit-Tests (hier würde man dann eher den Begriff Component-Tests verwenden) auf
folgendes Problem: Man würde gerne eine Methode Unit-Testen, merkt dann aber, dass diese Methode viele Abhängigkeiten
zu anderen Klassen / Methoden besitzt, welche einem eigentlich gar nicht interessieren in diesem Kontext. Sprich man
möchte diesen Teil, der einem nicht interessiert, irgendwie aushebeln.

Hier kommen [Test Doubles](https://en.wikipedia.org/wiki/Test_double) zum Zuge, welche man mit einem Stunt Double
vergleichen kann. Fakes, Stubs, Dummys, Spys und Mocks gehören alle zur Kategorie der Testdoubles. Ein Testdouble ist
ein Objekt oder System, das Sie in einem Test anstelle von etwas anderem verwenden. Die meisten automatisierten
Softwaretests beinhalten die Verwendung von Testdoubles der einen oder anderen Art.

---

## Test Doubles

Test Doubles werden in zwei Hauptkategorien aufgeteilt, welche jeweils nochmals unterkategorisiert werden:

* Mock (mock, spy)
* Stub (stub, dummy, fake)

**Mocks vs. Stubs = Behavioral Testing vs. State Testing**

Der Zweck von beidem ist es, das Testen aller Abhängigkeiten einer Klasse oder Funktion zu eliminieren, so dass Ihre
Tests konzentrierter und einfacher sind in dem, was sie zu beweisen versuchen.

* Ein **Mock** wartet darauf, von der zu testenden Klasse (A) aufgerufen zu werden.
    * Vielleicht hat sie mehrere Methoden, die sie erwartet und die A aufrufen sollte.
* Sie stellt sicher, dass sie auf genau die richtige Weise kontaktiert wurde.
    * Wenn A mit B so interagiert, wie es sein sollte, ist der Test erfolgreich.

* Ein **Stub** gibt Daten aus, die an die zu testende Klasse / das zu testende Objekt gehen.
* Der Unit-Test überprüft direkt die zu testende Klasse, um sicherzustellen, dass er das richtige Ergebnis liefert,
  wenn er mit diesen Daten gefüttert wird.

### Lifecycle

| Stubs | Mocks |
|-------|-------|
| 1. Setup — Bereiten Sie das zu testende Objekt und seine Stubs-Kollaborateure vor. | 1. Daten einrichten — Bereiten Sie das zu testende Objekt vor. |
| 2. Anwenden — Testen Sie die Funktionalität. | 2. **Setup expectations** — Bereiten Sie die Erwartungen in der Attrappe vor, die vom primären Objekt verwendet wird. |
| 3. Verify state — Verwenden Sie Asserts, um den Zustand des Objekts zu überprüfen. | 3. Anwenden — Testen Sie die Funktionalität. |
| 4. Teardown — Aufräumen der Ressourcen. | 4. **Verify expectations** — Überprüfen Sie, ob die richtigen Methoden im Mock aufgerufen wurden. |
| | 5. Verify state — Verwenden Sie Asserts, um den Zustand des Objekts zu überprüfen. |
| | 6. Teardown — Aufräumen der Ressourcen. |

---

## Mock, Spy, Stub, Fake, Dummy

### Mock

Mocks sind Objekte, denen man sagen kann, was sie zu empfangen erwarten. Sie werden verwendet, um das Verhalten
zwischen dem zu testenden System und seinen Kollaborateuren zu verifizieren.

Sie setzen Ihre Erwartungen, rufen die Methode des SUT auf und überprüfen, ob die Methode am Ende aufgerufen wurde.

### Spy

Stellen Sie sich einen Spy wie jemanden vor, der sich in Ihren SUT eingeschleust hat und jede seiner Bewegungen
aufzeichnet, genau wie ein echter Spion. Im Gegensatz zu Mocks schweigt der Spion und es liegt an Ihnen, anhand der
Daten, die er liefert, Aussagen zu treffen.

Sie verwenden Spione, wenn Sie sich nicht sicher sind, was Ihr SUT von Ihrem Mitarbeiter abrufen wird, also zeichnen
Sie alles auf und stellen fest, ob der Spion die gewünschten Daten abgerufen hat.

### Dummy

Dummy-Objekte sind sehr simpel. Ein Dummy ist z.B. nur ein Objekt, das Sie übergeben, um einen Konstruktor zu
initialisieren. Das kann etwas Einfaches sein wie z.B. einen `null`-Wert als Parameter mitgegeben. Es wird keine
Methode implementiert.

### Stub

Ein Stub ist ein Objekt, das vordefinierte Daten enthält und diese zur Beantwortung von Aufrufen während der Tests
verwendet. Es wird verwendet, wenn wir keine Objekte einbeziehen können oder wollen, die mit echten Daten antworten
würden oder unerwünschte Nebeneffekte hätten.

Ein Beispiel ist ein Objekt, das Daten aus der Datenbank abrufen muss, um auf einen Methodenaufruf zu reagieren.
Anstelle des echten Objekts haben wir einen Stub eingeführt und definiert, welche Daten zurückgegeben werden sollen.
Die Daten sind meistens sehr simpel und hardcoded.

### Fake

Fakes sind Objekte, die funktionierende Implementierungen haben, die aber nicht mit dem Produktionscode
übereinstimmen. Stub und Fakes sind sehr ähnlich. Fakes sind jedoch näher an einer realen Implementierung als ein
Stub. Normalerweise nehmen sie eine Abkürzung und haben eine vereinfachte Version des Produktionscodes.

Ein Beispiel für eine solche Abkürzung ist eine In-Memory-Implementierung von Data Access Object oder Repository.
Diese Fake-Implementierung greift nicht auf eine Datenbank zu, sondern verwendet eine einfache Sammlung zum Speichern
von Daten (zum Beispiel gefeedet aus einem CSV-File). So können wir Integrationstests von Diensten durchführen, ohne
eine Datenbank zu starten und zeitaufwändige Anfragen durchzuführen.

---

## Mockito — alles in einem

Nicht nur Mockito, sondern die meisten Mocking-Frameworks haben dieses Enten-Syndrom, bei dem sie mehrere Dinge tun
können: eine Ente kann schwimmen, fliegen und laufen. Diese Frameworks arbeiten mit Dummies, Mocks, Spys und Stubs.

* Lesen Sie das [Mockito Tutorial](https://www.vogella.com/tutorials/Mockito/article.html) bis und mit dem 9. Kapitel

## Sonstige Use Cases

Zusätzlich gibt es auch Frameworks, welche einen HTTP-Mock-Server initialisieren und so einem ermöglichen APIs zu
mocken und HTTP-Responses zu stubben. Eine Variante dafür wäre z.B. [WireMock](https://wiremock.org/)

---

## Source

* https://stackoverflow.com/questions/3459287/whats-the-difference-between-a-mock-stub
* https://stackoverflow.com/questions/346372/whats-the-difference-between-faking-mocking-and-stubbing
* https://methodpoet.com/stub-vs-mock/
* https://blog.pragmatists.com/test-doubles-fakes-mocks-and-stubs-1a7491dfa3da
* https://www.javacodegeeks.com/2019/04/introduction-to-test-doubles.html

## Checkpoint

* Ich kann die zwei Hauptkategorien Mocks und Stubs unterscheiden
* Ich kenne die Unterschiede des Lifecycles
* Ich kenne die jeweiligen Test Doubles und weiss, was für eine Aufgabe sie übernehmen
  * Mock, Spy, Stub, Fake, Dummy
* Ich habe erste Erfahrungen mit Mockito gemacht
