import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-start-page',
  imports: [RouterLink],
  template: `
    <p class="lead">Quiz auf dem Beamer, Antworten auf dem Handy. Wer richtig liegt, darf sabotieren.</p>
    <nav>
      <a routerLink="/host" class="button primary">Spiel hosten</a>
      <a routerLink="/join" class="button">Mitspielen</a>
    </nav>
  `,
  styles: `
    nav { display: flex; flex-wrap: wrap; gap: 1rem; }
  `,
})
export class StartPage {}
