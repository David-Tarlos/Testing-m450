import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { Countdown } from './countdown';

describe('Countdown', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it('zaehlt jede Sekunde herunter und bleibt bei null stehen', () => {
    const fixture = TestBed.createComponent(Countdown);
    fixture.componentRef.setInput('seconds', 2);
    fixture.componentRef.setInput('runKey', 1);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('2');

    vi.advanceTimersByTime(1000);
    fixture.detectChanges();
    expect(element.textContent).toContain('1');

    vi.advanceTimersByTime(5000);
    fixture.detectChanges();
    expect(element.textContent).toContain('0');
  });
});
