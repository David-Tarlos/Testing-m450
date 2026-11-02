import { Component, inject, input, OnInit } from '@angular/core';
import { GameSocketService } from '../core/game-socket.service';
import { PlayerGame } from '../game/player-game';
import { JoinForm, JoinRequest } from './join-form';
import { PlayerList } from './player-list';

/** Container fuers Handy: beitreten, warten und dann spielen. Der Code kann im Link stehen (?code=). */
@Component({
  selector: 'app-join-page',
  imports: [JoinForm, PlayerGame, PlayerList],
  template: `
    @if (socket.joined(); as player) {
      @if (socket.view().phase === 'LOBBY') {
        <h2>Hallo {{ player.name }}!</h2>
        <p class="waiting">Du bist drin. Gleich geht's los, schau auf den Beamer.</p>
        <app-player-list [players]="socket.view().players" />
      } @else {
        <app-player-game [name]="player.name" />
      }
    } @else {
      <h2>Mitspielen</h2>
      <app-join-form [initialCode]="code() ?? ''" [error]="socket.error()" (joinRequested)="join($event)" />
    }
  `,
})
export class JoinPage implements OnInit {
  protected readonly socket = inject(GameSocketService);
  /** Kommt ueber withComponentInputBinding aus dem Query-Parameter. */
  readonly code = input<string>();

  /** Nur beim Oeffnen pruefen: Wer danach im Formular einen anderen Code eintippt, bleibt drin. */
  ngOnInit(): void {
    const code = this.code()?.trim().toUpperCase();
    const player = this.socket.joined();
    if (code && player && code !== player.code) {
      this.socket.leave();
    }
  }

  protected join(request: JoinRequest): void {
    this.socket.join(request.code, request.name);
  }
}
