import { Component, input, output } from '@angular/core';
import { QuizSummary } from '../core/game-events';

/** Liste der Quiz, ein Klick startet eine neue Lobby. */
@Component({
  selector: 'app-quiz-picker',
  template: `
    <ul>
      @for (quiz of quizzes(); track quiz.id) {
        <li>
          <button type="button" (click)="picked.emit(quiz.id)">
            <strong>{{ quiz.title }}</strong>
            <span>{{ quiz.questions }} Fragen</span>
          </button>
        </li>
      } @empty {
        <li class="empty">Keine Quiz gefunden.</li>
      }
    </ul>
  `,
  styles: `
    ul { list-style: none; padding: 0; display: grid; gap: .75rem; }
    button { width: 100%; display: flex; justify-content: space-between; padding: 1rem; font-size: 1.1rem; }
  `,
})
export class QuizPicker {
  readonly quizzes = input.required<QuizSummary[]>();
  readonly picked = output<string>();
}
