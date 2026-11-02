import { Component, computed, input } from '@angular/core';
import { Attack } from '../core/game-events';
import { attackText } from '../core/game-rules';

/** Die Aufloesung: wer wen in dieser Runde sabotiert hat. */
@Component({
  selector: 'app-attack-list',
  template: `
    @if (texts().length > 0) {
      <h3>Sabotage dieser Runde</h3>
      <ul>
        @for (text of texts(); track text) {
          <li>{{ text }}</li>
        }
      </ul>
    }
  `,
  styles: `
    h3 { margin-bottom: .3rem; }
    ul { margin: 0; padding-left: 1.2rem; }
  `,
})
export class AttackList {
  readonly attacks = input.required<Attack[]>();
  protected readonly texts = computed(() => this.attacks().map(attackText));
}
