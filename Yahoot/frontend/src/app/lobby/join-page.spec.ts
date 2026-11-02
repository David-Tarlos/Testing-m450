import { ComponentFixture, TestBed } from '@angular/core/testing';
import { GameSocketService } from '../core/game-socket.service';
import { EMPTY_VIEW } from '../core/game-view';
import { fakeSocket } from './fake-socket';
import { JoinForm } from './join-form';
import { JoinPage } from './join-page';

describe('JoinPage', () => {
  let socket: ReturnType<typeof fakeSocket>;
  let fixture: ComponentFixture<JoinPage>;
  let element: HTMLElement;

  beforeEach(async () => {
    socket = fakeSocket();
    TestBed.configureTestingModule({ providers: [{ provide: GameSocketService, useValue: socket }] });
    fixture = TestBed.createComponent(JoinPage);
    element = fixture.nativeElement as HTMLElement;
    await render();
  });

  async function render() {
    fixture.detectChanges();
    await fixture.whenStable();
  }

  it('tritt mit Code und Namen aus dem Formular bei', () => {
    const form = fixture.debugElement.query((node) => node.componentInstance instanceof JoinForm);

    (form.componentInstance as JoinForm).joinRequested.emit({ code: 'ABC234', name: 'Lena' });

    expect(socket.join).toHaveBeenCalledWith('ABC234', 'Lena');
  });

  it('zeigt Fehler des Servers im Formular', async () => {
    socket.error.set('Kein Spiel mit dem Code ABC234');
    await render();

    expect(element.querySelector('[role=alert]')?.textContent).toContain('Kein Spiel');
  });

  describe('mit einem gespeicherten Spieler', () => {
    function open(code: string) {
      socket.joined.set({ code: 'ABC234', name: 'Lena', playerToken: 'token' });
      const page = TestBed.createComponent(JoinPage);
      page.componentRef.setInput('code', code);
      page.detectChanges();
    }

    it('verlaesst das alte Spiel, wenn der Link zu einem neuen fuehrt', () => {
      open('xyz789');

      expect(socket.leave).toHaveBeenCalled();
    });

    it('bleibt im Spiel, wenn der Link zum selben Spiel fuehrt', () => {
      open('abc234');

      expect(socket.leave).not.toHaveBeenCalled();
    });
  });

  it('wartet nach dem Beitritt mit den anderen auf den Start', async () => {
    socket.joined.set({ code: 'ABC234', name: 'Lena', playerToken: 'token' });
    socket.view.set({ ...EMPTY_VIEW, players: ['Lena', 'Tim'] });
    await render();

    expect(element.querySelector('h2')?.textContent).toContain('Lena');
    expect(element.querySelector('.waiting')).not.toBeNull();
    expect(element.querySelectorAll('app-player-list li').length).toBe(2);
  });
});
