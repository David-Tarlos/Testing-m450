import { Component, effect, input, signal } from '@angular/core';

/** Zaehlt ab, sobald sich seconds oder runKey aendern. Die echte Zeit misst der Server. */
@Component({
  selector: 'app-countdown',
  template: `
    <div class="bar" role="timer" aria-label="Verbleibende Zeit">
      <span class="fill" [style.width.%]="seconds() > 0 ? (left() / seconds()) * 100 : 0"></span>
      <strong>{{ left() }}</strong>
    </div>
  `,
  styles: `
    .bar { position: relative; height: 2rem; border-radius: 999px; background: var(--chip); overflow: hidden; }
    .fill { position: absolute; inset: 0 auto 0 0; background: var(--accent); opacity: .45; transition: width 1s linear; }
    strong { position: relative; display: block; text-align: center; line-height: 2rem; }
  `,
})
export class Countdown {
  readonly seconds = input.required<number>();
  readonly runKey = input<unknown>(null);
  protected readonly left = signal(0);

  constructor() {
    effect((onCleanup) => {
      this.runKey();
      this.left.set(this.seconds());
      const timer = setInterval(() => this.left.update((left) => Math.max(0, left - 1)), 1000);
      onCleanup(() => clearInterval(timer));
    });
  }
}
