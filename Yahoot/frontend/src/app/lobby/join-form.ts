import { Component, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

export interface JoinRequest {
  code: string;
  name: string;
}

/** Code und Name eingeben. Kennt keinen Server, meldet nur den Wunsch zum Beitritt. */
@Component({
  selector: 'app-join-form',
  imports: [FormsModule],
  template: `
    <form (ngSubmit)="submit()">
      <label>
        Spielcode
        <input name="code" [ngModel]="code()" (ngModelChange)="code.set($event)" maxlength="6"
               autocomplete="off" autocapitalize="characters" placeholder="ABC234" />
      </label>
      <label>
        Dein Name
        <input name="name" [ngModel]="name()" (ngModelChange)="name.set($event)" maxlength="20"
               autocomplete="nickname" />
      </label>
      @if (error()) {
        <p class="error" role="alert">{{ error() }}</p>
      }
      <button type="submit" [disabled]="!canSubmit()">Mitspielen</button>
    </form>
  `,
  styles: `
    form { display: grid; gap: 1rem; max-width: 22rem; }
    label { display: grid; gap: .3rem; font-weight: 600; }
    input { font-size: 1.3rem; padding: .6rem; }
    input[name=code] { letter-spacing: .3em; text-transform: uppercase; }
    .error { color: var(--danger); margin: 0; }
  `,
})
export class JoinForm implements OnInit {
  readonly initialCode = input('');
  readonly error = input<string | null>(null);
  readonly joinRequested = output<JoinRequest>();

  protected readonly code = signal('');
  protected readonly name = signal('');

  ngOnInit(): void {
    this.code.set(this.initialCode());
  }

  protected canSubmit(): boolean {
    return this.code().trim().length === 6 && this.name().trim().length > 0;
  }

  protected submit(): void {
    if (this.canSubmit()) {
      this.joinRequested.emit({ code: this.code().trim().toUpperCase(), name: this.name().trim() });
    }
  }
}
