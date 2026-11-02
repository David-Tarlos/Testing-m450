# Ablauf der Präsentation (10 Minuten)

| Zeit | Teil | Inhalt |
|---|---|---|
| 1 min | Idee | Kahoot, aber man kann sich gegenseitig sabotieren. Vier Karten kurz zeigen |
| 3 min | Demo | Die Klasse spielt mit: QR-Code scannen, zwei Fragen, eine Sabotage-Runde, Auflösung auf dem Beamer |
| 2 min | Testing | Testpyramide mit den Zahlen, Container und Dumb Components, was gemockt ist. Playwright mit Beamer und Handys |
| 1 min | Reports | Pipeline auf GitHub, JaCoCo-Report und Playwright-Report aus den Artefakten |
| 2 min | Reflexion | TDD und Code Reviews, siehe [Reflexion](reflexion.md) |
| 1 min | Fazit | Was gut lief, was wir anders machen würden |

## Gute Beispiele fürs Testing

- **Grün heisst nicht richtig:** Der Playwright-Test fürs Schild blieb bei wirkungslosem Schild grün, weil
  Playwright bis zu 5 Sekunden wartet und der Freeze nur 3 dauert.
- **Zeit im Griff:** Die Uhr ist gemockt, darum lässt sich «noch 15 Sekunden» ohne Warten prüfen.
- **Abbruch testen:** Playwright kappt den WebSocket eines Handys gezielt. Das Handy holt das Verpasste nach und
  spielt weiter.

## Vorher prüfen

- [ ] Eine Runde auf echten Handys gespielt, auch auf einem iPhone
- [ ] Adresse steht fest und ist vom Handy erreichbar, Beamer nicht über `localhost` geöffnet
- [ ] Plan B bereit, falls das WLAN streikt: Hotspot oder Video der Demo
- [ ] Pipeline-Lauf und Reports im Browser offen
