# Automatisiertes Testen und Deployen

> Kapitel 7 des Moduls 450 — Testen von Software (TBZ)
>
> **Zum Vorzeigen:** [VORZEIGEN.md](VORZEIGEN.md) — alles in einer Datei, von oben nach unten
>
> **Lösung zur Übung:** [Aufgabe 1–3 — recipe-planner](uebung-recipe-planner.md)
>
> **Zweites Kapitel in diesem Ordner:** [Deployment Environment](deployment-environment/README.md)
>
> *Hinweis: Die Bilder des Original-Kapitels (`x_gitres/*.png`) sind hier nicht
> eingebunden, damit keine kaputten Bildverweise entstehen.*

<!-- TOC -->
- [Automatisiertes Testen und Deployen](#automatisiertes-testen-und-deployen)
  - [Lernziele](#lernziele)
  - [Begriffe](#begriffe)
  - [Werkzeuge](#werkzeuge)
  - [Aufbau und Erstellen der Pipeline](#aufbau-und-erstellen-der-pipeline)
    - [Schritt 1 (Voraussetzungen)](#schritt-1-voraussetzungen)
    - [Schritt 2](#schritt-2)
    - [Schritt 3](#schritt-3)
  - [Anpassen der Pipeline auf Ihr Projekt](#anpassen-der-pipeline-auf-ihr-projekt)
    - [Reports mit Pages ausgeben](#reports-mit-pages-ausgeben)
  - [Auftrag](#auftrag)
  - [Quellen](#quellen)
<!-- TOC -->

---

## Lernziele

- Sie verstehen das Konzept der Pipeline mit Stages und Jobs
- Sie können in Ihrem Projekt die einzelnen Schritte einer Pipeline erstellen und konfigurieren
- Sie können Ihr Projekt automatisiert builden, testen und deployen

## Begriffe

**Pipeline:**
Die Top-Level Komponente, wo wir «Stages» und «Jobs» deklarieren können.

**Stage:**
Beschreibt die einzelne Phase in der Pipeline. Ein Stage besteht aus einem oder mehreren Jobs.

**Job:**
Ist ein einzelner Prozess innerhalb eines Stage. Ein Job könnte z.Bsp. das Kompilieren von Code sein.

**Runner:**
Runner ist eine open source Applikation, welche die einzelnen Jobs ausführt. Die App kann lokal
installiert werden oder in einer Cloud-Umgebung (Shared Runners werden auf GitLab gehostet).

## Werkzeuge

Wir verwenden GitLab, um unseren Code automatisiert zu testen und zu deployen. Sie können
alternativ auch mit GitHub arbeiten. Das Prinzip der Pipeline ist ungefähr identisch mit GitLab.

## Aufbau und Erstellen der Pipeline

Im Folgenden beschreiben wir die einzelnen Schritte zur Erstellung der Pipeline.

### Schritt 1 (Voraussetzungen)

Das Projekt auf GitLab existiert. Ein SSH Key ist erstellt.

### Schritt 2

Mittels *set up CI/CD* wird ein yaml File erstellt. Dieses File beschreibt die entsprechenden
Schritte für die Pipeline.
**Wichtig: das File hat den Namen .gitlab-ci.yml.** (das File kann auch lokal erstellt und dann
gepusht werden, deshalb auf die Namensgebung achten).
Das File wird im *Hauptverzeichnis des Repositories* abgelegt.
Syntax und Struktur des yml-Files, hier direkt aus dem generierten Template:

```yaml
stages:          # List of stages for jobs, and their order of execution
  - build
  - test
  - deploy

build-job:       # This job runs in the build stage, which runs first.
  stage: build
  script:
    - echo "Compiling the code..."
    - echo "Compile complete."

unit-test-job:   # This job runs in the test stage.
  stage: test    # It only starts when the job in the build stage completes successfully.
  script:
    - echo "Running unit tests... This will take about 60 seconds."
    - sleep 60
    - echo "Code coverage is 90%"

# Optional:
lint-test-job:   # This job also runs in the test stage.
  stage: test    # It can run at the same time as unit-test-job (in parallel).
  script:
    - echo "Linting code... This will take about 10 seconds."
    - sleep 10
    - echo "No lint issues found."

deploy-job:      # This job runs in the deploy stage.
  stage: deploy  # It only runs when *both* jobs in the test stage complete successfully.
  environment: production
  script:
    - echo "Deploying application..."
    - echo "Application successfully deployed."
```

### Schritt 3

Nun pushen Sie Ihren Code ins Repository. Achten Sie darauf, dass Sie ein Build-Tool in Ihrem
Projekt verwenden (Gradle oder Maven).
Am besten verwenden Sie den Pipeline Editor auf GitLab, um Ihre Pipeline anzupassen. GitLab prüft
dabei laufend, ob die Syntax korrekt ist.

---

## Anpassen der Pipeline auf Ihr Projekt

Falls Sie die Vorlage generiert haben, müssen Sie diese auf Ihr Projekt anpassen. Dazu noch eine
kurze Erläuterung der notwendigen Keywords:

**Image**

mit Image definieren Sie, welches Docker Image der Runner verwenden soll (z.Bsp. Maven oder
Gradle). Dieses Image wird im Build-Job verwendet.
Beispiel:

```yaml
image: maven:latest
```

**Variables**

mit Variables definieren Sie Variablen, die Sie in der Pipeline verwenden. Typischerweise sind das
Variablen, um z.Bsp. die Art der Ausführung des Build-Tools zu beschreiben.
Beispiel:

```yaml
variables:
  MAVEN_CLI_OPTS: " --batch-mode"
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"
```

Hier definieren wir, dass das CLI von Maven im batch-mode ausgeführt wird. Wir definieren auch, wo
das Repository mit dem Code zu finden ist.

Achtung: GitLab hat einige vordefinierte Variablen, die Sie gleich verwenden können. Eine
Auflistung dazu finden Sie hier:
https://docs.gitlab.com/ee/ci/variables/predefined_variables.html

**Script**

Im Script Element führen Sie die eigentlichen Befehle aus, damit der Job ausgeführt wird. Im obigen
Beispiel werden nur Prompts ausgegeben (echo).
Beispiel:

```yaml
script:
    - mvn $MAVEN_CLI_OPTS test
```

Hier rufen wir das Build-Programm Maven auf, im batch-mode. Wir rufen den Befehl «test» auf, um die
Unit Tests auszuführen.

**Artifacts**

mit Artifacts definieren Sie, welche Resultate oder «Produkte» Sie wollen und wo diese abgelegt
werden. Typischerweise wären das XML-Files mit den Unit-Testresultate.
Beispiel:

```yaml
java:
  stage: test
  script:
    - mvn $MAVEN_CLI_OPTS test
  artifacts:
    when: always
    reports:
      junit:
        - target/surefire-reports/*Test*.xml
        - target/failsafe-reports/TEST-*.xml
```

Hier beschreiben wir den Job *java* (im Stage «test»). Maven wird mit dem Befehl «test» ausgeführt.
Die Artefakte werden im target-Verzeichnis abgelegt.

### Reports mit Pages ausgeben

GitLab bietet sog. **Pages** an, um Artefakte in einem bestimmtem Format auszugeben.
Siehe dazu folgende Informationen: https://docs.gitlab.com/ee/user/project/pages/
Beispiel:

```yaml
pages:
  stage: deploy
  script:
    - mvn site
    - mv target/site public
  artifacts:
    paths:
    - public
```

Hier beschreiben wir den Job *pages* (im Stage "deploy"). Mit dem Maven Befehl "site" wird der
Report generiert.

---

## Auftrag

Erstellen Sie ein entsprechendes *Yaml-File* mit den nötigen Script Befehlen für Ihr Projekt.

> Umgesetzt in [`.github/workflows/recipe-planner.yml`](../.github/workflows/recipe-planner.yml).
> Die Begründung für GitHub Actions statt GitLab CI und die Übersetzung der Kapitelbegriffe
> stehen in der [Lösung](uebung-recipe-planner.md#3--aufgabe-3--pipeline).

---

## Quellen

Erklärt das Konzept von CI/CD:
https://www.youtube.com/watch?v=OPwU3UWCxhw

Detaillierte Anleitung für CI/CD auf GitLab mit Maven als Build:
https://www.youtube.com/watch?v=9llCMADxvzI

Pipeline Syntax:
https://docs.gitlab.com/ee/ci/yaml/

Predefined Variables in GitLab:
https://docs.gitlab.com/ee/ci/variables/predefined_variables.html

Anleitung zu Pipelines auf GitLab:
https://docs.gitlab.com/ee/topics/build_your_application.html
