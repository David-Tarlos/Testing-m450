import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { GameApiService } from '../core/game-api.service';
import { GameSocketService } from '../core/game-socket.service';
import { EMPTY_VIEW } from '../core/game-view';
import { fakeSocket } from './fake-socket';
import { HostPage } from './host-page';

describe('HostPage', () => {
  let socket: ReturnType<typeof fakeSocket>;
  let fixture: ComponentFixture<HostPage>;
  let element: HTMLElement;

  beforeEach(async () => {
    socket = fakeSocket();
    const api = {
      quizzes: () => of([{ id: 'mathe', title: 'Kopfrechnen', questions: 5 }]),
      createGame: () => of({ code: 'ABC234', hostToken: 'geheim' }),
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
});
