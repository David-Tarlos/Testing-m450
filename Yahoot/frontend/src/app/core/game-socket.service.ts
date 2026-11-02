import { inject, Injectable, OnDestroy, signal } from '@angular/core';
import { Client, StompSubscription } from '@stomp/stompjs';
import { GameApiService } from './game-api.service';
import { AnswerResult, Card, CardPlayed, GameEvent } from './game-events';
import { applyEvent, applySnapshot, EMPTY_VIEW, GameView } from './game-view';

export interface JoinedPlayer {
  code: string;
  name: string;
  playerToken: string;
}

const PLAYER_KEY = 'yahoot.player';

/**
 * Die einzige Stelle, die den WebSocket kennt. Komponenten lesen nur die Signale und rufen die
 * Methoden auf, darum lassen sie sich im Test mit einem einfachen Fake pruefen.
 *
 * Reisst die Verbindung ab, etwa bei einem gesperrten Handy, verbindet sich der Client von selbst neu.
 * Danach meldet sich das Handy mit seinem Token zurueck und holt den verpassten Spielstand.
 */
@Injectable({ providedIn: 'root' })
export class GameSocketService implements OnDestroy {
  readonly connected = signal(false);
  readonly view = signal<GameView>(EMPTY_VIEW);
  readonly joined = signal<JoinedPlayer | null>(null);
  readonly lastAnswer = signal<AnswerResult | null>(null);
  readonly lastCard = signal<CardPlayed | null>(null);
  readonly error = signal<string | null>(null);

  private readonly api = inject(GameApiService);
  private readonly client: Client;
  private gameSubscription: StompSubscription | null = null;
  private watchedCode: string | null = null;
  private rejoining = false;

  constructor() {
    const stored = readPlayer();
    if (stored) {
      this.joined.set(stored);
      this.watchedCode = stored.code;
    }
    this.client = new Client({
      brokerURL: socketUrl(),
      reconnectDelay: 2000,
      onConnect: () => this.onConnect(),
      onWebSocketClose: () => this.connected.set(false),
    });
    this.client.activate();
  }

  /** Ab jetzt die Ereignisse dieses Spiels verfolgen. */
  watch(code: string): void {
    this.watchedCode = code.toUpperCase();
    this.view.set(EMPTY_VIEW);
    if (this.connected()) {
      this.subscribeToGame();
    }
  }

  join(code: string, name: string): void {
    this.error.set(null);
    this.watch(code);
    this.send(`/app/games/${this.watchedCode}/join`, { name });
  }

  /** Das aktuelle Spiel verlassen, damit dieses Handy einem neuen beitreten kann. */
  leave(): void {
    this.rejoining = false;
    this.joined.set(null);
    writePlayer(null);
    this.watchedCode = null;
    if (this.connected()) {
      this.gameSubscription?.unsubscribe();
    }
    this.gameSubscription = null;
    this.view.set(EMPTY_VIEW);
    this.lastAnswer.set(null);
    this.lastCard.set(null);
    this.error.set(null);
  }

  answer(option: number): void {
    this.error.set(null);
    this.lastAnswer.set(null);
    this.send(`/app/games/${this.watchedCode}/answer`, { option });
  }

  playCard(card: Card, target: string): void {
    this.error.set(null);
    this.send(`/app/games/${this.watchedCode}/card`, { card, target });
  }

  host(command: 'start' | 'close' | 'sabotage' | 'next', hostToken: string): void {
    this.error.set(null);
    this.send(`/app/games/${this.watchedCode}/${command}`, { hostToken });
  }

  ngOnDestroy(): void {
    void this.client.deactivate();
  }

  private onConnect(): void {
    this.connected.set(true);
    this.gameSubscription = null;
    this.client.subscribe('/user/queue/joined', (frame) => this.onJoined(JSON.parse(frame.body)));
    this.client.subscribe('/user/queue/answers', (frame) =>
      this.lastAnswer.set(JSON.parse(frame.body).result),
    );
    this.client.subscribe('/user/queue/cards', (frame) => this.lastCard.set(JSON.parse(frame.body)));
    this.client.subscribe('/user/queue/errors', (frame) =>
      this.onError(JSON.parse(frame.body).message),
    );
    if (!this.watchedCode) {
      return;
    }
    this.subscribeToGame();
    const player = this.joined();
    if (player) {
      this.rejoining = true;
      this.send(`/app/games/${player.code}/rejoin`, { playerToken: player.playerToken });
    }
    // Nach einem Abbruch fehlen die Ereignisse dazwischen, darum einmal den ganzen Stand holen.
    this.api.state(this.watchedCode).subscribe({
      next: (snapshot) => this.view.set(applySnapshot(snapshot)),
      error: () => undefined,
    });
  }

  private onJoined(player: JoinedPlayer): void {
    this.rejoining = false;
    this.joined.set(player);
    writePlayer(player);
  }

  private onError(message: string): void {
    if (this.rejoining) {
      // Das Spiel gibt es nicht mehr: zurueck zum Formular.
      this.rejoining = false;
      this.joined.set(null);
      writePlayer(null);
      this.view.set(EMPTY_VIEW);
    }
    this.error.set(message);
  }

  private subscribeToGame(): void {
    this.gameSubscription?.unsubscribe();
    this.gameSubscription = this.client.subscribe(`/topic/games/${this.watchedCode}`, (frame) => {
      const event = JSON.parse(frame.body) as GameEvent;
      if (event.type === 'QuestionStarted') {
        this.lastAnswer.set(null);
      }
      if (event.type === 'SabotageOpened') {
        this.lastCard.set(null);
      }
      this.view.update((view) => applyEvent(view, event));
    });
  }

  /** Ohne Verbindung wird nichts verschickt, stattdessen erscheint ein Hinweis. */
  private send(destination: string, body: unknown): void {
    if (!this.connected()) {
      this.error.set('Keine Verbindung zum Server, bitte kurz warten');
      return;
    }
    this.client.publish({ destination, body: JSON.stringify(body) });
  }
}

function socketUrl(): string {
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${location.host}/ws`;
}

function readPlayer(): JoinedPlayer | null {
  try {
    const stored = sessionStorage.getItem(PLAYER_KEY);
    return stored ? (JSON.parse(stored) as JoinedPlayer) : null;
  } catch {
    return null;
  }
}

function writePlayer(player: JoinedPlayer | null): void {
  try {
    if (player) {
      sessionStorage.setItem(PLAYER_KEY, JSON.stringify(player));
    } else {
      sessionStorage.removeItem(PLAYER_KEY);
    }
  } catch {
    // Ohne Speicher klappt nur der Rejoin nach dem Neuladen nicht, das Spiel laeuft weiter.
  }
}
