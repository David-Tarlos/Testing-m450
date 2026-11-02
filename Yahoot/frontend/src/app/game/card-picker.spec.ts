import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CardPicker } from './card-picker';

describe('CardPicker', () => {
  let fixture: ComponentFixture<CardPicker>;

  beforeEach(() => {
    fixture = TestBed.createComponent(CardPicker);
    fixture.componentRef.setInput('me', 'Lena');
    fixture.componentRef.setInput('targets', ['Tim', 'Mia']);
    fixture.componentRef.setInput('energy', 1);
  });

  function button(name: string): HTMLButtonElement {
    fixture.detectChanges();
    const buttons = [...(fixture.nativeElement as HTMLElement).querySelectorAll('button')];
    return buttons.find((candidate) => candidate.textContent?.includes(name))!;
  }

  it('sperrt Karten, fuer die die Energie nicht reicht', () => {
    expect(button('Freeze').disabled).toBe(false);
    expect(button('Klau').disabled).toBe(true);
  });

  it('spielt einen Angriff auf das gewaehlte Ziel', () => {
    const played: unknown[] = [];
    fixture.componentInstance.play.subscribe((card) => played.push(card));
    fixture.detectChanges();
    const select = (fixture.nativeElement as HTMLElement).querySelector('select')!;
    select.value = 'Mia';
    select.dispatchEvent(new Event('change'));

    button('Freeze').click();

    expect(played).toEqual([{ card: 'FREEZE', target: 'Mia' }]);
  });

  it('das Schild gilt immer fuer einen selbst', () => {
    const played: unknown[] = [];
    fixture.componentInstance.play.subscribe((card) => played.push(card));

    button('Schild').click();

    expect(played).toEqual([{ card: 'SHIELD', target: 'Lena' }]);
  });

  it('zeigt nach dem Spielen nur noch die gespielte Karte', () => {
    fixture.componentRef.setInput('played', { card: 'FREEZE', target: 'Tim' });
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('button')).toBeNull();
    expect(element.textContent).toContain('Freeze auf Tim');
  });
});
