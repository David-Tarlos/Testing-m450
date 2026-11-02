import { ComponentFixture, TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { Attack, QuestionStarted } from '../core/game-events';
import { GameSocketService } from '../core/game-socket.service';
import { EMPTY_VIEW } from '../core/game-view';
import { fakeSocket } from '../lobby/fake-socket';
import { PlayerGame } from './player-game';

function question(attacks: Attack[] = []): QuestionStarted {
  return {
    type: 'QuestionStarted', code: 'ABC234', number: 2, total: 5,
    text: '2 + 2?', options: ['3', '4', '5', '6'], seconds: 20, attacks,
  };
}

const ranking = [
  { name: 'Lena', score: 1000, energy: 2 },
  { name: 'Tim', score: 0, energy: 1 },
];

describe('PlayerGame', () => {
  let socket: ReturnType<typeof fakeSocket>;
  let fixture: ComponentFixture<PlayerGame>;
  let element: HTMLElement;

  beforeEach(() => {
    vi.useFakeTimers();
    socket = fakeSocket();
    TestBed.configureTestingModule({ providers: [{ provide: GameSocketService, useValue: socket }] });
    fixture = TestBed.createComponent(PlayerGame);
    fixture.componentRef.setInput('name', 'Tim');
    element = fixture.nativeElement as HTMLElement;
  });

  afterEach(() => vi.useRealTimers());

  function answers(): HTMLButtonElement[] {
    fixture.detectChanges();
    return [...element.querySelectorAll<HTMLButtonElement>('app-answer-buttons button')];
  }

  function showQuestion(attacks: Attack[] = []) {
    socket.view.set({ ...EMPTY_VIEW, phase: 'QUESTION', question: question(attacks) });
  }

  it('schickt die gewaehlte Antwort und sperrt danach die Buttons', () => {
    showQuestion();

    answers()[1].click();
    socket.lastAnswer.set('ACCEPTED');

    expect(socket.answer).toHaveBeenCalledWith(1);
    expect(answers().every((button) => button.disabled)).toBe(true);
    expect(element.textContent).toContain('Antwort gespeichert');
  });

  it('bleibt offen, wenn der Server die Antwort ablehnt', () => {
    showQuestion();
    answers()[1].click();
    socket.lastAnswer.set('FROZEN');

    expect(answers().every((button) => !button.disabled)).toBe(true);
  });

  it('sperrt ein eingefrorenes Handy fuer 3 Sekunden', () => {
    showQuestion([{ attacker: 'Lena', card: 'FREEZE', target: 'Tim', blocked: false }]);

    expect(answers().every((button) => button.disabled)).toBe(true);
    expect(element.textContent).toContain('eingefroren');

    vi.advanceTimersByTime(3000);

    expect(answers().every((button) => !button.disabled)).toBe(true);
    expect(element.textContent).not.toContain('eingefroren');
  });

  it('laesst die Buttons bei Shuffle herumspringen', () => {
    showQuestion([{ attacker: 'Lena', card: 'SHUFFLE', target: 'Tim', blocked: false }]);
    const before = answers().map((button) => button.textContent);

    vi.advanceTimersByTime(1000);

    expect(answers().map((button) => button.textContent)).not.toEqual(before);
  });

  it('bleibt ruhig, wenn ein Angriff jemand anderen trifft', () => {
    showQuestion([{ attacker: 'Tim', card: 'FREEZE', target: 'Lena', blocked: false }]);

    expect(answers().every((button) => !button.disabled)).toBe(true);
  });

  it('zeigt in der Aufloesung Rangliste und Angriffe', () => {
    socket.view.set({
      ...EMPTY_VIEW,
      phase: 'REVEAL',
      question: question([{ attacker: 'Lena', card: 'FREEZE', target: 'Tim', blocked: false }]),
      correctOption: 1,
      ranking,
    });
    fixture.detectChanges();

    expect(element.textContent).toContain('Lena hat Tim eingefroren');
    expect(element.querySelector('li.me')?.textContent).toContain('Tim');
  });

  it('zeigt in der Aufloesung die eigene falsche Antwort', () => {
    showQuestion();
    answers()[0].click();
    socket.lastAnswer.set('ACCEPTED');
    fixture.detectChanges();

    socket.view.update((view) => ({ ...view, phase: 'REVEAL', correctOption: 1, ranking }));
    fixture.detectChanges();

    expect(element.querySelector('.wrong')?.textContent).toContain('3');
    expect(element.querySelector('.correct')?.textContent).toContain('4');
  });

  it('bietet in der Sabotage-Phase die anderen Spieler als Ziel an und spielt die Karte', () => {
    socket.view.set({ ...EMPTY_VIEW, phase: 'SABOTAGE', ranking, sabotageSeconds: 10 });
    fixture.detectChanges();

    const options = [...element.querySelectorAll('select option')].map((option) => option.textContent);
    expect(options).toEqual(['Lena']);

    [...element.querySelectorAll('button')].find((button) => button.textContent?.includes('Freeze'))!.click();
    expect(socket.playCard).toHaveBeenCalledWith('FREEZE', 'Lena');
  });

  it('zeigt am Schluss den eigenen Platz', () => {
    socket.view.set({ ...EMPTY_VIEW, phase: 'FINISHED', ranking });
    fixture.detectChanges();

    expect(element.textContent).toContain('Platz 2');
  });
});
