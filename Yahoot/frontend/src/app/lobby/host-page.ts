import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { GameApiService } from '../core/game-api.service';
import { CreatedGame } from '../core/game-events';
import { GameSocketService } from '../core/game-socket.service';
import { PlayerList } from './player-list';
import { QuizPicker } from './quiz-picker';

/** Container fuer den Beamer: Quiz waehlen, Code zeigen, Spiel starten. */
@Component({
  selector: 'app-host-page',
  imports: [PlayerList, QuizPicker],
  template: `
    @if (game(); as game) {
      @if (view().phase === 'LOBBY') {
        <p>Geh auf <strong>{{ joinUrl() }}</strong> und tippe den Code ein:</p>
        <p class="code" data-testid="game-code">{{ game.code }}</p>
        <app-player-list [players]="view().players" />
        <button type="button" class="primary" [disabled]="view().players.length === 0" (click)="start()">
          Spiel starten
        </button>
      } @else if (view().question; as question) {
        <p class="progress">Frage {{ question.number }} von {{ question.total }}</p>
        <h2>{{ question.text }}</h2>
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
    .progress { color: var(--muted); }
    .error { color: var(--danger); }
  `,
})
export class HostPage {
  protected readonly socket = inject(GameSocketService);
  private readonly api = inject(GameApiService);

  protected readonly quizzes = toSignal(this.api.quizzes(), { initialValue: [] });
  protected readonly game = signal<CreatedGame | null>(null);
  protected readonly view = this.socket.view;
  protected readonly joinUrl = computed(() => `${location.host}/join`);

  protected create(quizId: string): void {
    this.api.createGame(quizId).subscribe((game) => {
      this.game.set(game);
      this.socket.watch(game.code);
    });
  }

  protected start(): void {
    const game = this.game();
    if (game) {
      this.socket.host('start', game.hostToken);
    }
  }
}
