import { Injectable, OnDestroy, signal } from '@angular/core';
import { Client, StompSubscription } from '@stomp/stompjs';
import { AnswerResult, Card, GameEvent } from './game-events';
import { applyEvent, EMPTY_VIEW, GameView } from './game-view';

export interface JoinedPlayer {
  code: string;
  name: string;
}

/**
 * Die einzige Stelle, die den WebSocket kennt. Komponenten lesen nur die Signale und rufen die
 * Methoden auf, darum lassen sie sich im Test mit einem einfachen Fake pruefen.
 */
@Injectable({ providedIn: 'root' })
export class GameSocketService implements OnDestroy {
  readonly connected = signal(false);
  readonly view = signal<GameView>(EMPTY_VIEW);
  readonly joined = signal<JoinedPlayer | null>(null);
  readonly lastAnswer = signal<AnswerResult | null>(null);
  readonly error = signal<string | null>(null);

  private readonly client: Client;
  private gameSubscription: StompSubscription | null = null;
  private watchedCode: string | null = null;

  constructor() {
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

  answer(option: number): void {
    this.send(`/app/games/${this.watchedCode}/answer`, { option });
  }

  playCard(card: Card, target: string): void {
    this.send(`/app/games/${this.watchedCode}/card`, { card, target });
  }

  host(command: 'start' | 'close' | 'sabotage' | 'next', hostToken: string): void {
    this.send(`/app/games/${this.watchedCode}/${command}`, { hostToken });
  }

  ngOnDestroy(): void {
    void this.client.deactivate();
  }

  private onConnect(): void {
    this.connected.set(true);
    this.client.subscribe('/user/queue/joined', (frame) => this.joined.set(JSON.parse(frame.body)));
    this.client.subscribe('/user/queue/answers', (frame) =>
      this.lastAnswer.set(JSON.parse(frame.body).result),
    );
    this.client.subscribe('/user/queue/errors', (frame) =>
      this.error.set(JSON.parse(frame.body).message),
    );
    if (this.watchedCode) {
      this.subscribeToGame();
    }
  }

  private subscribeToGame(): void {
    this.gameSubscription?.unsubscribe();
    this.gameSubscription = this.client.subscribe(`/topic/games/${this.watchedCode}`, (frame) => {
      const event = JSON.parse(frame.body) as GameEvent;
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
