import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { CardPlayed } from '../core/game-events';
import { effectsFor, FREEZE_SECONDS, shuffledOrder } from '../core/game-rules';
import { GameSocketService } from '../core/game-socket.service';
import { AnswerButtons } from './answer-buttons';
import { AttackList } from './attack-list';
import { CardPicker } from './card-picker';
import { Countdown } from './countdown';
import { RankingList } from './ranking-list';

/** Container fuers Handy waehrend des Spiels. Holt alles vom Socket und reicht es an die Dumb Components. */
@Component({
  selector: 'app-player-game',
  imports: [AnswerButtons, AttackList, CardPicker, Countdown, RankingList],
  template: `
    @switch (view().phase) {
      @case ('QUESTION') {
        @if (question(); as question) {
          <p class="progress">Frage {{ question.number }} von {{ question.total }}</p>
          <h2>{{ question.text }}</h2>
          <app-countdown [seconds]="question.seconds" [runKey]="question" />
          @if (frozen()) {
            <p class="status frozen" role="status">Du bist eingefroren!</p>
          }
          <app-answer-buttons
            [options]="question.options"
            [order]="order()"
            [chosen]="chosen()"
            [disabled]="frozen() || locked()"
            [dimmed]="frozen() || locked()"
            (picked)="answer($event)"
          />
          @if (locked()) {
            <p class="status" role="status">Antwort gespeichert</p>
          } @else if (socket.lastAnswer() === 'FROZEN') {
            <p class="status" role="status">Noch eingefroren, versuch es gleich nochmal</p>
          } @else if (socket.lastAnswer() === 'TOO_LATE') {
            <p class="status" role="status">Zu spät</p>
          }
        }
      }
      @case ('REVEAL') {
        @if (question(); as question) {
          <h2>{{ question.text }}</h2>
          <app-answer-buttons
            [options]="question.options"
            [order]="baseOrder()"
            [chosen]="chosen()"
            [correctOption]="view().correctOption"
            [disabled]="true"
          />
          <app-attack-list [attacks]="question.attacks" />
        }
        <app-ranking-list [ranking]="view().ranking" [me]="name()" />
      }
      @case ('SABOTAGE') {
        <h2>Sabotage!</h2>
        @if (view().sabotageSeconds > 0) {
          <app-countdown [seconds]="view().sabotageSeconds" />
        }
        <app-card-picker
          [me]="name()"
          [targets]="targets()"
          [energy]="energy()"
          [played]="socket.lastCard()"
          (play)="play($event)"
        />
      }
      @case ('FINISHED') {
        <h2>Du bist Platz {{ place() }}!</h2>
        <app-ranking-list [ranking]="view().ranking" [me]="name()" />
        <button type="button" (click)="socket.leave()">Neues Spiel</button>
      }
    }
    @if (socket.error(); as error) {
      <p class="error" role="alert">{{ error }}</p>
    }
    @if (!socket.connected()) {
      <p class="status" role="status">Verbindung verloren, ich verbinde neu …</p>
    }
  `,
  styles: `
    .progress { color: var(--muted); margin-bottom: 0; }
    h2 { margin-top: .3rem; overflow-wrap: anywhere; }
    .status { font-weight: 700; text-align: center; }
    .frozen { color: var(--accent); }
    .error { color: var(--danger); }
  `,
})
export class PlayerGame {
  protected readonly socket = inject(GameSocketService);
  readonly name = input.required<string>();

  protected readonly view = this.socket.view;
  protected readonly question = computed(() => this.view().question);
  private readonly questionOpen = computed(() => this.view().phase === 'QUESTION');
  protected readonly chosen = signal<number | null>(null);
  protected readonly frozen = signal(false);
  protected readonly order = signal<number[]>([]);

  protected readonly baseOrder = computed(() => (this.question()?.options ?? []).map((_, index) => index));
  protected readonly locked = computed(() => {
    const result = this.socket.lastAnswer();
    return this.chosen() !== null && (result === null || result === 'ACCEPTED' || result === 'ALREADY_ANSWERED');
  });
  protected readonly targets = computed(() =>
    this.view().ranking.map((player) => player.name).filter((player) => player !== this.name()),
  );
  protected readonly energy = computed(
    () => this.view().ranking.find((player) => player.name === this.name())?.energy ?? 0,
  );
  protected readonly place = computed(
    () => this.view().ranking.findIndex((player) => player.name === this.name()) + 1,
  );

  constructor() {
    // Die eigene Wahl gilt bis zur naechsten Frage, sie wird auch in der Aufloesung noch gebraucht.
    effect(() => {
      this.question();
      this.chosen.set(null);
    });
    effect((onCleanup) => {
      const question = this.question();
      this.order.set(this.baseOrder());
      this.frozen.set(false);
      if (!question || !this.questionOpen()) {
        return;
      }
      const effects = effectsFor(question.attacks, this.name());
      const timers: ReturnType<typeof setTimeout>[] = [];
      if (effects.frozen) {
        this.frozen.set(true);
        timers.push(setTimeout(() => this.frozen.set(false), FREEZE_SECONDS * 1000));
      }
      if (effects.shuffled) {
        this.order.set(shuffledOrder(this.baseOrder()));
        timers.push(setInterval(() => this.order.update(shuffledOrder), 1000));
      }
      onCleanup(() => timers.forEach((timer) => clearTimeout(timer)));
    });
  }

  protected answer(option: number): void {
    this.chosen.set(option);
    this.socket.answer(option);
  }

  protected play(card: CardPlayed): void {
    this.socket.playCard(card.card, card.target);
  }
}
