import { Component, input } from '@angular/core';
import { PlayerScore } from '../core/game-events';

/** Die Rangliste. Auf dem Handy ist der eigene Name hervorgehoben. */
@Component({
  selector: 'app-ranking-list',
  template: `
    <ol>
      @for (player of ranking(); track player.name; let place = $index) {
        <li [class.me]="player.name === me()">
          <span class="place">{{ place + 1 }}.</span>
          <span class="name">{{ player.name }}</span>
          <strong>{{ player.score }}</strong>
        </li>
      }
    </ol>
  `,
  styles: `
    ol { list-style: none; padding: 0; margin: 1rem 0; display: grid; gap: .4rem; }
    li { display: flex; gap: .6rem; align-items: center; background: var(--chip); border-radius: .6rem; padding: .5rem .8rem; }
    .name { flex: 1; overflow-wrap: anywhere; }
    .me { outline: 2px solid var(--accent); }
  `,
})
export class RankingList {
  readonly ranking = input.required<PlayerScore[]>();
  readonly me = input<string | null>(null);
}
