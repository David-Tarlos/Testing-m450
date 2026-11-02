import { Component, input } from '@angular/core';

/** Zeigt, wer schon im Spiel ist. */
@Component({
  selector: 'app-player-list',
  template: `
    <p class="count">{{ players().length }} {{ players().length === 1 ? 'Spieler' : 'Spieler:innen' }}</p>
    @if (players().length === 0) {
      <p class="empty">Noch niemand da. Code eintippen und mitspielen!</p>
    } @else {
      <ul>
        @for (player of players(); track player) {
          <li>{{ player }}</li>
        }
      </ul>
    }
  `,
  styles: `
    ul { display: flex; flex-wrap: wrap; gap: .5rem; list-style: none; padding: 0; }
    li { background: var(--chip); border-radius: 999px; padding: .4rem .9rem; font-weight: 600; }
    .empty { color: var(--muted); }
  `,
})
export class PlayerList {
  readonly players = input.required<string[]>();
}
