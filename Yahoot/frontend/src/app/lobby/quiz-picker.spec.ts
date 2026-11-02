import { TestBed } from '@angular/core/testing';
import { QuizPicker } from './quiz-picker';

describe('QuizPicker', () => {
  it('meldet das angeklickte Quiz', () => {
    const fixture = TestBed.createComponent(QuizPicker);
    fixture.componentRef.setInput('quizzes', [
      { id: 'mathe', title: 'Kopfrechnen', questions: 5 },
      { id: 'geo', title: 'Geografie', questions: 8 },
    ]);
    const picked: string[] = [];
    fixture.componentInstance.picked.subscribe((id) => picked.push(id));
    fixture.detectChanges();

    const buttons = (fixture.nativeElement as HTMLElement).querySelectorAll('button');
    buttons[1].click();

    expect(buttons.length).toBe(2);
    expect(buttons[0].textContent).toContain('5 Fragen');
    expect(picked).toEqual(['geo']);
  });
});
