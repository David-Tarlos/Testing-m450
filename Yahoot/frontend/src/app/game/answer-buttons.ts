import { Component, input, output } from '@angular/core';

/** Die Antwort-Buttons. Die Farbe gehoert zur Antwort, die Position kommt von aussen (Shuffle). */
@Component({
  selector: 'app-answer-buttons',
  template: `
    @for (index of order(); track index) {
      <button
        type="button"
        [class]="'answer answer-' + (index % 4)"
        [class.correct]="correctOption() === index"
        [class.wrong]="correctOption() !== null && chosen() === index && correctOption() !== index"
        [class.chosen]="chosen() === index"
        [class.dimmed]="dimmed() || (correctOption() !== null && correctOption() !== index)"
        [disabled]="disabled()"
        (click)="picked.emit(index)"
      >
        {{ options()[index] }}
      </button>
    }
  `,
  styles: `
    :host { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: .6rem; margin: 1rem 0; }
    .answer { min-height: 5rem; border: 3px solid transparent; color: #fff; overflow-wrap: anywhere; }
    .answer:disabled { opacity: 1; cursor: default; }
    .answer.dimmed:disabled { opacity: .45; }
    .answer-0 { background: var(--answer-0); }
    .answer-1 { background: var(--answer-1); }
    .answer-2 { background: var(--answer-2); }
    .answer-3 { background: var(--answer-3); }
    .chosen { border-color: var(--text); }
    .correct { border-color: var(--success); box-shadow: 0 0 0 3px var(--success); }
    .wrong { text-decoration: line-through; }
  `,
})
export class AnswerButtons {
  readonly options = input.required<string[]>();
  readonly order = input.required<number[]>();
  readonly disabled = input(false);
  /** Blass auf dem Handy, wenn man gerade nicht antworten kann. Auf dem Beamer bleibt alles gut lesbar. */
  readonly dimmed = input(false);
  readonly chosen = input<number | null>(null);
  readonly correctOption = input<number | null>(null);
  readonly picked = output<number>();
}
