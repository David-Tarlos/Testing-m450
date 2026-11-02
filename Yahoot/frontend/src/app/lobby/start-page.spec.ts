import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { StartPage } from './start-page';

describe('StartPage', () => {
  it('fuehrt zum Hosten und zum Mitspielen', () => {
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(StartPage);
    fixture.detectChanges();

    const links = [...(fixture.nativeElement as HTMLElement).querySelectorAll('a')];

    expect(links.map((a) => a.getAttribute('href'))).toEqual(['/host', '/join']);
  });
});
