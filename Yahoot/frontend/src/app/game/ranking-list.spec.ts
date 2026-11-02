import { TestBed } from '@angular/core/testing';
import { RankingList } from './ranking-list';

describe('RankingList', () => {
  it('zeigt Platz, Name und Punkte und hebt den eigenen Namen hervor', () => {
    const fixture = TestBed.createComponent(RankingList);
    fixture.componentRef.setInput('ranking', [
      { name: 'Lena', score: 1500, energy: 2 },
      { name: 'Tim', score: 700, energy: 1 },
    ]);
    fixture.componentRef.setInput('me', 'Tim');
    fixture.detectChanges();

    const rows = (fixture.nativeElement as HTMLElement).querySelectorAll('li');
    expect(rows[0].textContent).toContain('1.');
    expect(rows[0].textContent).toContain('Lena');
    expect(rows[0].textContent).toContain('1500');
    expect(rows[1].classList).toContain('me');
  });
});
