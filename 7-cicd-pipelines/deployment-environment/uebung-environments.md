# Übungen — Deployment Environments

> **Aufgabenstellung**
>
> Arbeiten Sie zu zweit an diesen Aufgaben.
>
> **Aufgabe 1** — Recherchieren Sie kurz zu den vorgestellten Softwarelösungen (Docker Compose,
> Kubernetes, Vagrant, Terraform) und entscheiden Sie, welche Softwarelösung für das Setup welcher
> Umgebung optimal geeignet ist. Dokumentieren Sie Ihre Überlegungen.
>
> **Aufgabe 2** — Entscheiden Sie sich für eine der Lösungen und überlegen Sie, welche Umgebung Sie
> damit automatisiert aufsetzen möchten und welche Software Sie darauf deployen möchten. Nehmen Sie
> sich ca. 1 Lektion Zeit. Ziel ist es, dass Sie schauen, wie weit Sie kommen und an welche Probleme
> Sie anstossen. Schreiben Sie Ihre Gedanken in einer kurzen Reflexion bzw. einem Fazit nieder.
>
> **Aufgabe 3** (optional — challenge): vollständig automatisiertes Setup. **Nicht bearbeitet.**

Theorie: [Deployment Environment](README.md) · Setup: [`docker-compose.yml`](docker-compose.yml)

<!-- TOC -->
- [1 Aufgabe 1 — Welche Lösung für welche Umgebung?](#1--aufgabe-1--welche-lösung-für-welche-umgebung)
- [2 Aufgabe 2 — Testing Environment mit Docker Compose](#2--aufgabe-2--testing-environment-mit-docker-compose)
  - [2.1 Entscheidung](#21--entscheidung)
  - [2.2 Das Setup](#22--das-setup)
  - [2.3 Was tatsächlich passiert ist](#23--was-tatsächlich-passiert-ist)
  - [2.4 Ergebnis](#24--ergebnis)
- [3 Reflexion](#3--reflexion)
<!-- TOC -->

---

## 1  Aufgabe 1 — Welche Lösung für welche Umgebung?

### Die vier Werkzeuge

| | Arbeitet mit | Abstraktionsebene | Kernaufgabe |
|---|---|---|---|
| **Docker Compose** | Containern | eine Maschine | Mehrere Container zusammen starten, verbinden und konfigurieren |
| **Kubernetes** | Containern | ein Cluster aus vielen Maschinen | Container verteilen, skalieren, neu starten, ausrollen |
| **Vagrant** | virtuellen Maschinen | eine Maschine | Eine VM reproduzierbar erzeugen und provisionieren |
| **Terraform** | Infrastruktur beliebiger Art | Rechenzentrum / Cloud | Infrastruktur deklarativ beschreiben und beim Anbieter anlegen |

Der entscheidende Unterschied verläuft nicht zwischen den vier Werkzeugen gleichmässig, sondern
entlang **zweier** Achsen:

**Container oder VM?** Ein Container teilt sich den Kernel des Hosts und startet in Sekunden. Eine
VM bringt ein eigenes Betriebssystem mit, braucht Minuten und Gigabytes. Wer nur eine Anwendung
laufen lassen will, nimmt Container; wer ein vollständiges System mit eigenem Kernel, eigenen
Diensten und eigener Netzwerkkonfiguration braucht, nimmt eine VM.

**Was wird beschrieben — die Anwendung oder die Infrastruktur?** Compose und Kubernetes beschreiben,
*welche Dienste laufen sollen*. Terraform beschreibt, *worauf sie laufen sollen* — Server, Netze,
Datenbanken, Loadbalancer beim Cloud-Anbieter. Das sind komplementäre Aufgaben, keine konkurrierenden:
in der Praxis legt Terraform den Kubernetes-Cluster an, und Kubernetes betreibt darin die Container.

### Zuordnung zu den Umgebungen

| Umgebung | Empfehlung | Begründung |
|---|---|---|
| **Development** | **Vagrant** oder **Docker Compose** | Auf der Workstation zählt: schnell hochfahren, schnell wegwerfen, auf jedem Rechner im Team gleich. Compose reicht, wenn die Anwendung containerisiert ist. Vagrant lohnt, wenn das Team auf unterschiedlichen Betriebssystemen arbeitet oder die Software Kernel-Nähe braucht (Treiber, systemd, eigene Netzwerkstacks) |
| **Testing** | **Docker Compose** | Genau der Fall, für den Compose gebaut ist: mehrere zusammengehörige Dienste, eine Maschine, eine Datei, ein Befehl. Schnell genug, um pro Testlauf neu aufgesetzt zu werden — und weil der Zustand nach `down` weg ist, startet jeder Lauf identisch |
| **Staging** | **Kubernetes**, bereitgestellt durch **Terraform** | Staging soll der Produktion *möglichst genau entsprechen*. Läuft Produktion auf Kubernetes, muss Staging es auch — sonst testet man eine andere Architektur. Terraform legt die Umgebung an, Kubernetes betreibt sie |
| **Production** | **Terraform** + **Kubernetes** | Terraform, weil Infrastruktur versioniert, überprüfbar und wiederherstellbar sein muss. Kubernetes, weil dort Ausfallsicherheit, Skalierung und rollierende Updates gebraucht werden — genau das, was Compose nicht kann |

### Die zwei Sätze, auf die es hinausläuft

**Compose skaliert nicht über eine Maschine hinaus.** Es gibt keinen Scheduler, keine
Selbstheilung, kein rollierendes Update über mehrere Hosts. Das ist keine Schwäche, sondern der
Zuschnitt: für Dev und Test braucht man das alles nicht, und Kubernetes dort einzusetzen bedeutet,
sich Cluster-Komplexität für ein Problem einzuhandeln, das man nicht hat.

**Vagrant und Terraform lösen unterschiedliche Probleme**, auch wenn beide von HashiCorp kommen und
beide „Maschinen erzeugen". Der [offizielle Vergleich](https://developer.hashicorp.com/vagrant/intro/vs/terraform)
sagt es deutlich: Vagrant ist für **Entwicklungsumgebungen** gedacht, Terraform für
**Infrastruktur, die nicht auf dem eigenen Laptop steht**. Vagrant-VMs sind zum Wegwerfen da,
Terraform-Infrastruktur soll Bestand haben.

---

## 2  Aufgabe 2 — Testing Environment mit Docker Compose

### 2.1  Entscheidung

**Werkzeug:** Docker Compose.
**Umgebung:** Testing Environment.
**Software:** der [recipe-planner](../uebung-recipe-planner.md) aus dem CI/CD-Teil dieses Kapitels —
React-Frontend, Spring-Boot-Backend, H2-Datenbank.

Warum nicht Development: die Entwicklungsumgebung ist laut Theorie die Workstation des Entwicklers,
und dort laufen Backend und Frontend ohnehin direkt. Eine Stufe weiter wird es interessant — eine
Umgebung, die auf **jedem** Rechner identisch hochkommt, ohne dass vorher jemand JDK 17, Maven und
Node 18 von Hand installiert. Genau das ist der Zweck der Testumgebung aus der Theorie: neuen Code
prüfbar machen, unabhängig davon, was auf dem Rechner des Prüfenden installiert ist.

### 2.2  Das Setup

Drei Dateien:

| Datei | Inhalt |
|---|---|
| [`docker-compose.yml`](docker-compose.yml) | Die beiden Services, Netzwerk, Ports, Healthcheck |
| [`../recipe-planner/recipe-planner-backend/Dockerfile`](../recipe-planner/recipe-planner-backend/Dockerfile) | Mehrstufig: bauen mit Maven, ausliefern mit JRE |
| [`../recipe-planner/recipe-planner-fronend/Dockerfile`](../recipe-planner/recipe-planner-fronend/Dockerfile) | Node 18, React-Dev-Server |

```bash
cd 7-cicd-pipelines/deployment-environment
docker compose up --build        # hochfahren
docker compose down              # wieder abbauen
```

Drei Entscheidungen, die nicht offensichtlich waren:

**Mehrstufiger Backend-Build.** Stufe 1 baut mit `maven:3.9-eclipse-temurin-17`, Stufe 2 kopiert nur
das fertige JAR in ein `eclipse-temurin:17-jre`. Das Auslieferungs-Image enthält dadurch weder Maven
noch den Quellcode. Zusätzlich wird erst die `pom.xml` kopiert und `dependency:go-offline`
ausgeführt — solange sich die Abhängigkeiten nicht ändern, kommt dieser Layer aus dem Cache und ein
Codewechsel löst keinen erneuten Download aus.

**Healthcheck statt blindes `depends_on`.** `depends_on` allein wartet nur darauf, dass der Container
*gestartet* ist — nicht darauf, dass die Anwendung *antwortet*. Ein Spring-Boot-Start dauert aber
mehrere Sekunden. Mit `condition: service_healthy` und einem Healthcheck auf `/api/recipes` startet
das Frontend erst, wenn die API wirklich erreichbar ist.

**Port 8080 muss auf den Host veröffentlicht werden**, obwohl beide Container im selben Netzwerk
liegen. Grund ist Befund **F-01** aus dem CI/CD-Teil: das Frontend hat die Backend-URL fest im Code
(`Browse.js:7`, `const baseURL = "http://localhost:8080/api/recipes"`). Dieser Aufruf passiert im
**Browser des Benutzers**, nicht im Frontend-Container — ein Compose-interner Servicename wie
`http://backend:8080` würde dort ins Leere laufen. Ebenso ist Port 3000 nicht frei wählbar: der
Controller erlaubt per `@CrossOrigin(origins = "http://localhost:3000")` genau diese Origin.

### 2.3  Was tatsächlich passiert ist

Chronologisch, einschliesslich dessen, was nicht auf Anhieb ging:

**1. Der Docker-Daemon lief nicht.** Das CLI war installiert (Docker 28.4.0, Compose v2.39.4-desktop.1),
aber:

```text
error during connect: Get "http://%2F%2F.%2Fpipe%2FdockerDesktopLinuxEngine/v1.51/containers/json":
open //./pipe/dockerDesktopLinuxEngine: The system cannot find the file specified.
```

Ein installiertes CLI heisst nicht, dass die Engine läuft. Docker Desktop musste erst gestartet
werden; danach war der Daemon nach rund zehn Sekunden erreichbar.

> **Änderung gegenüber Kapitel 5:** Dort ist dokumentiert, dass Docker Desktop auf diesem Rechner
> *nicht* lief — das MSI-Paket verlangte Administratorrechte, weshalb k6 als portables ZIP
> installiert wurde. Inzwischen ist Docker Desktop 4.47.0 vorhanden und funktioniert. Die alte
> Einschränkung gilt nicht mehr.

**2. Der erste Build dauerte mehrere Minuten.** Beide Images laden ihre Abhängigkeiten im Container
neu: Maven zieht Spring Boot komplett, npm installiert React mit allem Drumherum. Das ist der Preis
der Reproduzierbarkeit — dafür braucht der Rechner selbst weder JDK noch Node.

**3. Der Healthcheck funktionierte auf Anhieb.** `curl` ist im `eclipse-temurin:17-jre` enthalten,
was nicht bei jedem Basis-Image selbstverständlich ist.

### 2.4  Ergebnis

```text
NAME                      STATUS                    PORTS
recipe-planner-backend    Up 27 seconds (healthy)   0.0.0.0:8080->8080/tcp
recipe-planner-frontend   Up 11 seconds             0.0.0.0:3000->3000/tcp
```

Geprüft wurde:

| Prüfung | Ergebnis |
|---|---|
| `GET http://localhost:8080/api/recipes` | **HTTP 200**, nach 0,39 s |
| Seed-Daten vorhanden | **15 Rezepte** (6× Spaghetti Bolognese, je 3× Lasagne, Fried Rice, Pommes Frites) mit je einer Zutat |
| `GET http://localhost:3000` | **HTTP 200**, nach 14,2 s (Dev-Server kompiliert beim ersten Aufruf) |
| CORS mit `Origin: http://localhost:3000` | **HTTP 200** |
| CORS mit `Origin: http://evil.example` | **HTTP 403** |
| Container-zu-Container (`wget http://backend:8080/api/recipes` aus dem Frontend-Container) | funktioniert — das Compose-Netzwerk löst den Servicenamen auf |

Die Umgebung steht damit vollständig und ist mit **einem Befehl** reproduzierbar. Die Aufgabe hat
gefragt, wie weit man in ungefähr einer Lektion kommt: bis hierhin.

---

## 3  Reflexion

### Was gut funktioniert hat

**Die Umgebung ist jetzt unabhängig vom Rechner.** Vorher brauchte es JDK 17, Maven und Node 18 in
passenden Versionen — im Modul 450 hat genau das schon zweimal Ärger gemacht: in Kapitel 5 brach
Lombok 1.18.28 auf JDK 21 ab, und der recipe-planner bringt nicht einmal einen `mvnw`-Wrapper mit.
Im Container ist die Java-Version Teil der Beschreibung und keine Eigenschaft des Laptops.

**Der Healthcheck war die wichtigste Zeile.** Ohne ihn startet das Frontend, während das Backend noch
hochfährt — und man sucht den Fehler in der Anwendung statt in der Startreihenfolge.

### Woran es gehakt hat

**Die Portwahl war nicht frei.** Zwei fest verdrahtete Stellen im Code — die Backend-URL im Frontend
und die CORS-Origin im Controller — bestimmen, wie das Compose-File aussehen muss. Wäre die URL über
eine Umgebungsvariable konfigurierbar (`REACT_APP_API_URL`), könnte man die Umgebung frei
zusammenstecken. **Lehre: Containerisierbarkeit entscheidet sich im Anwendungscode, nicht im
Compose-File.** Das ist dieselbe Erkenntnis wie bei der Testbarkeit in
[Kapitel 4](../../4-abhaengigkeiten-zu-schnittstellen/uebung-addressbook.md) — Constructor Injection
machte dort das Mocking erst möglich.

**Das Frontend-Image ist mit 1,4 GB unverhältnismässig gross** (Backend: 537 MB). Ursache ist der
React-Dev-Server mit dem vollständigen `node_modules`-Baum. Für eine Testumgebung ist das
vertretbar — man will ja den Dev-Server testen. Für Staging oder Produktion müsste man
`npm run build` machen und die statischen Dateien von einem nginx ausliefern; das Image wäre dann
im zweistelligen Megabyte-Bereich. Dass Test- und Produktions-Image sich unterscheiden, ist genau
der Punkt, an dem Staging gebraucht wird: dort fällt auf, was nur in der Testvariante funktioniert.

**Kein Datenbank-Container.** Die Anwendung benutzt H2 in-memory im Backend-Prozess. Für die
Testumgebung ist das sogar erwünscht — nach `down` ist der Stand weg, jeder Lauf startet gleich.
Es heisst aber auch, dass die Compose-Datei **nicht** zeigt, was sie eigentlich zeigen sollte: das
Zusammenspiel mehrerer unabhängiger Dienste inklusive Volume und Datenpersistenz. Ein Umstieg auf
PostgreSQL wäre dafür nötig gewesen — das wäre aber eine Änderung am Produktivcode, die die
Aufgabenstellung nicht verlangt.

### Wann ich Docker Compose einsetzen würde

**Ja, wenn:** mehrere Dienste zusammengehören und gemeinsam hochkommen müssen; die Umgebung auf jedem
Rechner gleich sein soll; ein Testlauf eine frische Umgebung braucht; jemand Neues im Team in fünf
Minuten statt einem halben Tag lauffähig sein soll.

**Nein, wenn:** es um Produktion mit mehreren Maschinen geht (dann Kubernetes); die Software
Kernel-Nähe braucht (dann Vagrant); Infrastruktur beim Cloud-Anbieter angelegt werden soll (dann
Terraform).

**Der konkrete nächste Schritt** wäre, die Compose-Umgebung an die Pipeline aus
[Aufgabe 3](../uebung-recipe-planner.md#3--aufgabe-3--pipeline) zu hängen: Integrationstests laufen
heute nicht, weil sie eine laufende Anwendung brauchen. Mit `docker compose up -d` als
Pipeline-Schritt liesse sich das automatisieren — und damit wäre der Bogen vom Testkonzept über die
Testautomatisierung bis zur Umgebung geschlossen.
