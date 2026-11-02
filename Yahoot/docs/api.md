# Schnittstelle Yahoot

Beamer und Handy reden mit dem Server über REST für alles Einmalige und über STOMP auf `/ws` für das
Live-Spiel. Alle Nachrichten sind JSON.

## REST

| Methode | Pfad | Antwort |
|---|---|---|
| GET | `/api/quizzes` | `[{id, title, questions}]` |
| POST | `/api/games` mit `{quizId}` | 201 `{code, hostToken}`, 404 bei unbekanntem Quiz |
| GET | `/api/games/{code}` | ganzer Spielstand: `{code, phase, round, total, text, options, secondsLeft, correctOption, attacks, players, ranking}`, 404 bei unbekanntem Code |

Das `hostToken` bleibt beim Beamer. Wer es nicht kennt, kann das Spiel nicht steuern. Den Spielstand holen
Beamer und Handy nach dem Neuladen oder einem Verbindungsabbruch. `correctOption` ist `null`, solange die
Frage offen ist.

## STOMP: senden

| Ziel | Wer | Inhalt |
|---|---|---|
| `/app/games/{code}/join` | Handy | `{name}` |
| `/app/games/{code}/rejoin` | Handy | `{playerToken}`, nach einem Verbindungsabbruch |
| `/app/games/{code}/answer` | Handy | `{option}` |
| `/app/games/{code}/card` | Handy | `{card, target}`, Karte ist `FREEZE`, `SHUFFLE`, `STEAL` oder `SHIELD` |
| `/app/games/{code}/start` | Host | `{hostToken}` |
| `/app/games/{code}/close` | Host | `{hostToken}`, schliesst die Frage vor Ablauf der Zeit |
| `/app/games/{code}/sabotage` | Host | `{hostToken}` |
| `/app/games/{code}/next` | Host | `{hostToken}` |

Der Server merkt sich beim Beitritt, welcher Name zu welcher Sitzung gehört. Antworten und Karten
brauchen darum keinen Namen. Bei einem Abbruch wechselt die Sitzung, mit dem `playerToken` aus der
Beitrittsantwort findet das Handy zurück zu seinem Spieler.

## STOMP: abonnieren

`/topic/games/{code}` bekommt alle Ereignisse des Spiels. Das Feld `type` sagt, welches:

| type | Felder |
|---|---|
| `PlayerJoined` | `name`, `players` |
| `QuestionStarted` | `number`, `total`, `text`, `options`, `seconds`, `attacks` aus der letzten Sabotage-Phase |
| `QuestionClosed` | `number`, `correctOption`, `ranking` |
| `SabotageOpened` | `number`, `seconds`, `ranking` |
| `GameFinished` | `ranking` |

Persönlich, nur für die eigene Sitzung:

| Ziel | Inhalt |
|---|---|
| `/user/queue/joined` | `{code, name, playerToken}` nach erfolgreichem Beitritt oder Rejoin |
| `/user/queue/cards` | `{card, target}`, wenn die Karte angenommen wurde |
| `/user/queue/answers` | `{result}`: `ACCEPTED`, `ALREADY_ANSWERED`, `FROZEN`, `TOO_LATE`, `INVALID_OPTION` |
| `/user/queue/errors` | `{message}` bei Regelverstoss oder unbekanntem Code |

Ist die Zeit einer Frage um, schliesst der Server sie selbst. Auch die Sabotage-Phase endet nach 10
Sekunden von allein, der Host kann früher weiterschalten. Die Auflösung startet er selbst. Fertige Spiele
verschwinden nach 30 Minuten aus dem Speicher.
