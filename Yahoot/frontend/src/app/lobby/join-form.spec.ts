import { ComponentFixture, TestBed } from '@angular/core/testing';
import { JoinForm, JoinRequest } from './join-form';

describe('JoinForm', () => {
  let fixture: ComponentFixture<JoinForm>;
  let element: HTMLElement;
  let requests: JoinRequest[];

  async function setUp(initialCode = '') {
    fixture = TestBed.createComponent(JoinForm);
    fixture.componentRef.setInput('initialCode', initialCode);
    requests = [];
    fixture.componentInstance.joinRequested.subscribe((request) => requests.push(request));
    fixture.detectChanges();
    await fixture.whenStable();
    element = fixture.nativeElement as HTMLElement;
  }

  async function type(name: 'code' | 'name', value: string) {
    const input = element.querySelector<HTMLInputElement>(`input[name=${name}]`)!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    await fixture.whenStable();
  }

  function button(): HTMLButtonElement {
    return element.querySelector('button')!;
  }

  it('meldet Code in Grossbuchstaben und Namen ohne Leerzeichen', async () => {
    await setUp();
    await type('code', 'abc234');
    await type('name', '  Lena ');

    button().click();

    expect(requests).toEqual([{ code: 'ABC234', name: 'Lena' }]);
  });

  it('laesst ohne vollstaendigen Code oder Namen nicht abschicken', async () => {
    await setUp();
    await type('code', 'ABC');
    await type('name', 'Lena');
    expect(button().disabled).toBe(true);

    await type('code', 'ABC234');
    await type('name', '   ');
    expect(button().disabled).toBe(true);
  });

  it('uebernimmt einen Code aus dem Link', async () => {
    await setUp('XYZ789');

    expect(element.querySelector<HTMLInputElement>('input[name=code]')!.value).toBe('XYZ789');
  });

  it('zeigt einen Fehler vom Server an', async () => {
    await setUp();
    fixture.componentRef.setInput('error', 'Name ist bereits vergeben: Lena');
    fixture.detectChanges();

    expect(element.querySelector('[role=alert]')?.textContent).toContain('bereits vergeben');
  });
});
