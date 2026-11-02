import { Component, inject, input } from '@angular/core';
import { GameSocketService } from '../core/game-socket.service';
import { JoinForm, JoinRequest } from './join-form';
import { PlayerList } from './player-list';

/** Container fuers Handy: beitreten und auf den Start warten. Der Code kann im Link stehen (?code=). */
@Component({
  selector: 'app-join-page',
  imports: [JoinForm, PlayerList],
  template: `
    @if (socket.joined(); as player) {
      <h2>Hallo {{ player.name }}!</h2>
      @if (socket.view().phase === 'LOBBY') {
        <p class="waiting">Du bist drin. Gleich geht's los, schau auf den Beamer.</p>
        <app-player-list [players]="socket.view().players" />
      } @else if (socket.view().question; as question) {
        <p>Frage {{ question.number }} von {{ question.total }} läuft.</p>
      }
    } @else {
      <h2>Mitspielen</h2>
      <app-join-form [initialCode]="code() ?? ''" [error]="socket.error()" (joinRequested)="join($event)" />
    }
  `,
})
export class JoinPage {
  protected readonly socket = inject(GameSocketService);
  /** Kommt ueber withComponentInputBinding aus dem Query-Parameter. */
  readonly code = input<string>();

  protected join(request: JoinRequest): void {
    this.socket.join(request.code, request.name);
  }
}
