import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { GameApiService } from '../core/game-api.service';
import { CreatedGame } from '../core/game-events';
import { GameSocketService } from '../core/game-socket.service';
import { AnswerButtons } from '../game/answer-buttons';
import { AttackList } from '../game/attack-list';
import { Countdown } from '../game/countdown';
import { RankingList } from '../game/ranking-list';
import { JoinQr } from './join-qr';
import { PlayerList } from './player-list';
import { QuizPicker } from './quiz-picker';

const HOST_KEY = 'yahoot.host';

/** Container fuer den Beamer: Quiz waehlen, Code zeigen und das Spiel durch alle Phasen fuehren. */
@Component({
  selector: 'app-host-page',
  imports: [AnswerButtons, AttackList, Countdown, JoinQr, PlayerList, QuizPicker, RankingList],
  template: `
    @if (game(); as game) {
      @switch (view().phase) {
        @case ('LOBBY') {
          <p>Scanne den QR-Code oder geh auf <strong>{{ joinHost() }}</strong> und tippe den Code ein:</p>
          <p class="code" data-testid="game-code">{{ game.code }}</p>
          <app-join-qr [url]="joinUrl()" />
          <app-player-list [players]="view().players" />
          <button type="button" class="primary" [disabled]="view().players.length === 0" (click)="command('start')">
            Spiel starten
          </button>
        }
        @case ('QUESTION') {
          @if (view().question; as question) {
            <p class="progress">Frage {{ question.number }} von {{ question.total }}</p>
            <h2>{{ question.text }}</h2>
            <app-countdown [seconds]="question.seconds" [runKey]="question" />
            <app-answer-buttons [options]="question.options" [order]="order()" [disabled]="true" />
            <button type="button" class="primary" (click)="command('close')">Frage schliessen</button>
          }
        }
        @case ('REVEAL') {
          @if (view().question; as question) {
            <p class="progress">Frage {{ question.number }} von {{ question.total }}</p>
            <h2>{{ question.text }}</h2>
            <app-answer-buttons
              [options]="question.options"
              [order]="order()"
              [correctOption]="view().correctOption"
              [disabled]="true"
            />
            <app-attack-list [attacks]="question.attacks" />
          }
          <app-ranking-list [ranking]="view().ranking" />
          <button type="button" class="primary" (click)="command('sabotage')">Sabotage starten</button>
        }
        @case ('SABOTAGE') {
          <h2>Sabotage!</h2>
          <p>Schaut auf eure Handys und spielt eure Karten.</p>
          @if (view().sabotageSeconds > 0) {
            <app-countdown [seconds]="view().sabotageSeconds" />
          }
          <app-ranking-list [ranking]="view().ranking" />
          <button type="button" class="primary" (click)="command('next')">Nächste Frage</button>
        }
        @case ('FINISHED') {
          <h2>{{ winner() }} gewinnt!</h2>
          @if (view().question; as question) {
            <app-attack-list [attacks]="question.attacks" />
          }
          <app-ranking-list [ranking]="view().ranking" />
          <a class="button" href="/host" (click)="reset()">Neues Spiel</a>
        }
      }
    } @else {
      <h2>Welches Quiz?</h2>
      <app-quiz-picker [quizzes]="quizzes()" (picked)="create($event)" />
    }
    @if (socket.error(); as error) {
      <p class="error" role="alert">{{ error }}</p>
    }
  `,
  styles: `
    .code { font-size: clamp(3rem, 12vw, 7rem); font-weight: 800; letter-spacing: .15em; margin: .5rem 0; }
    .progress { color: var(--muted); margin-bottom: 0; }
    h2 { margin-top: .3rem; overflow-wrap: anywhere; }
    .error { color: var(--danger); }
  `,
})
export class HostPage {
  protected readonly socket = inject(GameSocketService);
  private readonly api = inject(GameApiService);

  protected readonly quizzes = toSignal(this.api.quizzes(), { initialValue: [] });
  protected readonly game = signal<CreatedGame | null>(null);
  protected readonly view = this.socket.view;
  protected readonly joinHost = computed(() => `${location.host}/join`);
  protected readonly joinUrl = computed(() => `${location.origin}/join?code=${this.game()?.code ?? ''}`);
  protected readonly order = computed(() => (this.view().question?.options ?? []).map((_, index) => index));
  protected readonly winner = computed(() => this.view().ranking[0]?.name ?? 'Niemand');

  constructor() {
    this.restore();
  }

  protected create(quizId: string): void {
    this.api.createGame(quizId).subscribe((game) => {
      this.game.set(game);
      this.remember(game);
      this.socket.watch(game.code);
    });
  }

  protected command(command: 'start' | 'close' | 'sabotage' | 'next'): void {
    const game = this.game();
    if (game) {
      this.socket.host(command, game.hostToken);
    }
  }

  protected reset(): void {
    this.remember(null);
  }

  /** Nach dem Neuladen zum eigenen Spiel zurueck, solange es das noch gibt. */
  private restore(): void {
    const stored = this.recall();
    if (!stored) {
      return;
    }
    this.api.state(stored.code).subscribe({
      next: () => {
        this.game.set(stored);
        this.socket.watch(stored.code);
      },
      error: () => this.remember(null),
    });
  }

  private recall(): CreatedGame | null {
    try {
      const stored = sessionStorage.getItem(HOST_KEY);
      return stored ? (JSON.parse(stored) as CreatedGame) : null;
    } catch {
      return null;
    }
  }

  private remember(game: CreatedGame | null): void {
    try {
      if (game) {
        sessionStorage.setItem(HOST_KEY, JSON.stringify(game));
      } else {
        sessionStorage.removeItem(HOST_KEY);
      }
    } catch {
      // Ohne Speicher klappt nur das Neuladen nicht, das Spiel laeuft weiter.
    }
  }
}
