import { TestBed } from '@angular/core/testing';
import { PlayerList } from './player-list';

describe('PlayerList', () => {
  function render(players: string[]): HTMLElement {
    const fixture = TestBed.createComponent(PlayerList);
    fixture.componentRef.setInput('players', players);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('zeigt alle Namen und die Anzahl', () => {
    const element = render(['Lena', 'Tim']);

    expect([...element.querySelectorAll('li')].map((li) => li.textContent)).toEqual(['Lena', 'Tim']);
    expect(element.querySelector('.count')?.textContent).toContain('2');
  });

  it('ermuntert zum Mitspielen, solange niemand da ist', () => {
    const element = render([]);

    expect(element.querySelector('li')).toBeNull();
    expect(element.querySelector('.empty')).not.toBeNull();
  });
});
