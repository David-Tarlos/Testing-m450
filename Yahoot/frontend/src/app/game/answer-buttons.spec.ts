import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AnswerButtons } from './answer-buttons';

describe('AnswerButtons', () => {
  let fixture: ComponentFixture<AnswerButtons>;

  beforeEach(() => {
    fixture = TestBed.createComponent(AnswerButtons);
    fixture.componentRef.setInput('options', ['A', 'B', 'C']);
    fixture.componentRef.setInput('order', [0, 1, 2]);
  });

  function buttons(): HTMLButtonElement[] {
    fixture.detectChanges();
    return [...(fixture.nativeElement as HTMLElement).querySelectorAll('button')];
  }

  it('meldet auch bei vertauschter Reihenfolge die richtige Antwort', () => {
    const picked: number[] = [];
    fixture.componentInstance.picked.subscribe((index) => picked.push(index));
    fixture.componentRef.setInput('order', [2, 0, 1]);

    const shown = buttons();
    expect(shown.map((button) => button.textContent?.trim())).toEqual(['C', 'A', 'B']);

    shown[0].click();
    expect(picked).toEqual([2]);
  });

  it('sperrt alle Buttons, wenn nicht geantwortet werden darf', () => {
    fixture.componentRef.setInput('disabled', true);

    expect(buttons().every((button) => button.disabled)).toBe(true);
  });

  it('markiert in der Aufloesung die richtige und die falsche eigene Antwort', () => {
    fixture.componentRef.setInput('correctOption', 1);
    fixture.componentRef.setInput('chosen', 2);

    const shown = buttons();
    expect(shown[1].classList).toContain('correct');
    expect(shown[2].classList).toContain('wrong');
    expect(shown[0].classList).not.toContain('correct');
  });
});
