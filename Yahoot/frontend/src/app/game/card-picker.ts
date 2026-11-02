import { Component, computed, input, linkedSignal, output } from '@angular/core';
import { CardPlayed } from '../core/game-events';
import { CARDS, CardInfo, cardLabel } from '../core/game-rules';

/** Die Sabotage-Karten: Ziel waehlen und Karte spielen. Ob es erlaubt ist, entscheidet der Server. */
@Component({
  selector: 'app-card-picker',
  template: `
    @if (played(); as played) {
      <p class="played">Gespielt: {{ playedText() }}</p>
    } @else {
      <p>Deine Energie: <strong data-testid="energy">{{ energy() }}</strong></p>
      <label>
        Ziel
        <select [value]="target()" (change)="target.set($any($event.target).value)">
          @for (name of targets(); track name) {
            <option [value]="name" [selected]="name === target()">{{ name }}</option>
          }
        </select>
      </label>
      <div class="cards">
        @for (info of cards; track info.card) {
          <button type="button" [disabled]="!canPlay(info)" (click)="playCard(info)">
            {{ info.label }} ({{ info.cost }})
            <small>{{ info.hint }}</small>
          </button>
        }
      </div>
    }
  `,
  styles: `
    label { display: grid; gap: .3rem; margin: .8rem 0; font-weight: 600; }
    select { font: inherit; padding: .6rem; border-radius: .5rem; background: var(--bg); color: var(--text); }
    .cards { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: .6rem; }
    small { display: block; font-weight: 400; }
    .played { font-weight: 700; }
  `,
})
export class CardPicker {
  readonly me = input.required<string>();
  readonly targets = input.required<string[]>();
  readonly energy = input.required<number>();
  readonly played = input<CardPlayed | null>(null);
  readonly play = output<CardPlayed>();

  protected readonly cards = CARDS;
  protected readonly target = linkedSignal(() => this.targets()[0] ?? '');
  protected readonly playedText = computed(() => {
    const played = this.played();
    if (!played) {
      return '';
    }
    return played.card === 'SHIELD'
      ? cardLabel(played.card)
      : `${cardLabel(played.card)} auf ${played.target}`;
  });

  protected canPlay(info: CardInfo): boolean {
    const hasTarget = info.card === 'SHIELD' || this.target() !== '';
    return hasTarget && this.energy() >= info.cost;
  }

  protected playCard(info: CardInfo): void {
    const target = info.card === 'SHIELD' ? this.me() : this.target();
    this.play.emit({ card: info.card, target });
  }
}
