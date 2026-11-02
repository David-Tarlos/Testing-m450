import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { GameApiService } from '../core/game-api.service';
import { QuestionStarted } from '../core/game-events';
import { GameSocketService } from '../core/game-socket.service';
import { EMPTY_VIEW } from '../core/game-view';
import { fakeSocket } from './fake-socket';
import { HostPage } from './host-page';

describe('HostPage', () => {
  let socket: ReturnType<typeof fakeSocket>;
  let fixture: ComponentFixture<HostPage>;
  let element: HTMLElement;

  beforeEach(async () => {
    sessionStorage.clear();
    socket = fakeSocket();
    const api = {
      quizzes: () => of([{ id: 'mathe', title: 'Kopfrechnen', questions: 5 }]),
      createGame: () => of({ code: 'ABC234', hostToken: 'geheim' }),
      state: () => of({}),
    };
    TestBed.configureTestingModule({
      providers: [
        { provide: GameSocketService, useValue: socket },
        { provide: GameApiService, useValue: api },
      ],
    });
    fixture = TestBed.createComponent(HostPage);
    element = fixture.nativeElement as HTMLElement;
    await render();
  });

  async function render() {
    fixture.detectChanges();
    await fixture.whenStable();
  }

  async function createGame() {
    element.querySelector<HTMLButtonElement>('app-quiz-picker button')!.click();
    await render();
  }

  function startButton(): HTMLButtonElement {
    return element.querySelector<HTMLButtonElement>('button.primary')!;
  }

  it('erstellt nach der Quizwahl ein Spiel und zeigt den Code', async () => {
    await createGame();

    expect(element.querySelector('[data-testid=game-code]')?.textContent).toBe('ABC234');
    expect(socket.watch).toHaveBeenCalledWith('ABC234');
  });

  it('startet erst, wenn jemand beigetreten ist', async () => {
    await createGame();
    expect(startButton().disabled).toBe(true);

    socket.view.set({ ...EMPTY_VIEW, players: ['Lena'] });
    await render();
    startButton().click();

    expect(socket.host).toHaveBeenCalledWith('start', 'geheim');
  });

  it('zeigt nach dem Start die laufende Frage', async () => {
    await createGame();

    socket.view.set({
      ...EMPTY_VIEW,
      phase: 'QUESTION',
      question: {
        type: 'QuestionStarted', code: 'ABC234', number: 1, total: 5,
        text: '2 + 2?', options: ['3', '4'], seconds: 20, attacks: [],
      },
    });
    await render();

    expect(element.querySelector('h2')?.textContent).toBe('2 + 2?');
    expect(element.textContent).toContain('Frage 1 von 5');
  });

  const question: QuestionStarted = {
    type: 'QuestionStarted', code: 'ABC234', number: 2, total: 5, text: '2 + 2?', options: ['3', '4'],
    seconds: 20, attacks: [{ attacker: 'Lena', card: 'FREEZE', target: 'Tim', blocked: false }],
  };
  const ranking = [
    { name: 'Lena', score: 1500, energy: 2 },
    { name: 'Tim', score: 500, energy: 1 },
  ];

  it('schliesst die Frage auf Knopfdruck und verraet die Angriffe noch nicht', async () => {
    await createGame();
    socket.view.set({ ...EMPTY_VIEW, phase: 'QUESTION', question });
    await render();

    expect(element.textContent).not.toContain('eingefroren');
    startButton().click();

    expect(socket.host).toHaveBeenCalledWith('close', 'geheim');
  });

  it('zeigt in der Aufloesung Loesung, Angriffe und Rangliste und startet die Sabotage', async () => {
    await createGame();
    socket.view.set({ ...EMPTY_VIEW, phase: 'REVEAL', question, correctOption: 1, ranking });
    await render();

    expect(element.querySelector('.correct')?.textContent).toContain('4');
    expect(element.textContent).toContain('Lena hat Tim eingefroren');
    expect(element.querySelectorAll('app-ranking-list li').length).toBe(2);

    startButton().click();
    expect(socket.host).toHaveBeenCalledWith('sabotage', 'geheim');
  });

  it('geht aus der Sabotage-Phase zur naechsten Frage', async () => {
    await createGame();
    socket.view.set({ ...EMPTY_VIEW, phase: 'SABOTAGE', question, ranking, sabotageSeconds: 10 });
    await render();

    startButton().click();

    expect(socket.host).toHaveBeenCalledWith('next', 'geheim');
  });

  it('zeigt am Schluss den Endstand mit dem Sieger', async () => {
    await createGame();
    socket.view.set({ ...EMPTY_VIEW, phase: 'FINISHED', question, ranking });
    await render();

    expect(element.textContent).toContain('Lena gewinnt');
    expect(element.querySelector('button.primary')).toBeNull();
  });

  it('zeigt in der Lobby einen QR-Code mit dem Link zum Beitreten', async () => {
    await createGame();

    await vi.waitFor(() => {
      fixture.detectChanges();
      const image = element.querySelector<HTMLImageElement>('app-join-qr img');
      expect(image?.src).toContain('data:image/svg+xml');
    });
  });

  it('findet nach dem Neuladen zum eigenen Spiel zurueck', async () => {
    sessionStorage.setItem('yahoot.host', JSON.stringify({ code: 'XYZ789', hostToken: 'alt' }));

    const reloaded = TestBed.createComponent(HostPage);
    reloaded.detectChanges();
    await reloaded.whenStable();

    expect(socket.watch).toHaveBeenCalledWith('XYZ789');
  });
});
