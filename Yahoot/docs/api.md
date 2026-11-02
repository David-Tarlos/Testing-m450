# Schnittstelle Yahoot

Beamer und Handy reden mit dem Server über REST für alles Einmalige und über STOMP auf `/ws` für das
Live-Spiel. Alle Nachrichten sind JSON.

## REST

| Methode | Pfad | Antwort |
|---|---|---|
| GET | `/api/quizzes` | `[{id, title, questions}]` |
| POST | `/api/games` mit `{quizId}` | 201 `{code, hostToken}`, 404 bei unbekanntem Quiz |
| GET | `/api/games/{code}` | `{code, phase, round, ranking}`, 404 bei unbekanntem Code |

Das `hostToken` bleibt beim Beamer. Wer es nicht kennt, kann das Spiel nicht steuern.

## STOMP: senden

| Ziel | Wer | Inhalt |
|---|---|---|
| `/app/games/{code}/join` | Handy | `{name}` |
| `/app/games/{code}/answer` | Handy | `{option}` |
| `/app/games/{code}/card` | Handy | `{card, target}`, Karte ist `FREEZE`, `SHUFFLE`, `STEAL` oder `SHIELD` |
| `/app/games/{code}/start` | Host | `{hostToken}` |
| `/app/games/{code}/close` | Host | `{hostToken}`, schliesst die Frage vor Ablauf der Zeit |
| `/app/games/{code}/sabotage` | Host | `{hostToken}` |
| `/app/games/{code}/next` | Host | `{hostToken}` |

Der Server merkt sich beim Beitritt, welcher Name zu welcher Sitzung gehört. Antworten und Karten
brauchen darum keinen Namen.

## STOMP: abonnieren

`/topic/games/{code}` bekommt alle Ereignisse des Spiels. Das Feld `type` sagt, welches:

| type | Felder |
|---|---|
| `PlayerJoined` | `name`, `players` |
| `QuestionStarted` | `number`, `total`, `text`, `options`, `seconds`, `attacks` aus der letzten Sabotage-Phase |
| `QuestionClosed` | `number`, `correctOption`, `ranking` |
| `SabotageOpened` | `ranking` |
| `GameFinished` | `ranking` |

Persönlich, nur für die eigene Sitzung:

| Ziel | Inhalt |
|---|---|
| `/user/queue/joined` | `{code, name}` nach erfolgreichem Beitritt |
| `/user/queue/answers` | `{result}`: `ACCEPTED`, `ALREADY_ANSWERED`, `FROZEN`, `TOO_LATE`, `INVALID_OPTION` |
| `/user/queue/errors` | `{message}` bei Regelverstoss oder unbekanntem Code |

Ist die Zeit einer Frage um, schliesst der Server sie selbst. Fertige Spiele verschwinden nach 30
Minuten aus dem Speicher.
