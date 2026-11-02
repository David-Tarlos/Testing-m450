import { signal } from '@angular/core';
import { vi } from 'vitest';
import { AnswerResult, CardPlayed } from '../core/game-events';
import { JoinedPlayer } from '../core/game-socket.service';
import { EMPTY_VIEW, GameView } from '../core/game-view';

/** Ersetzt den GameSocketService im Test: gleiche Signale, Methoden als Spione. */
export function fakeSocket() {
  return {
    connected: signal(true),
    view: signal<GameView>(EMPTY_VIEW),
    joined: signal<JoinedPlayer | null>(null),
    lastAnswer: signal<AnswerResult | null>(null),
    lastCard: signal<CardPlayed | null>(null),
    error: signal<string | null>(null),
    watch: vi.fn(),
    join: vi.fn(),
    answer: vi.fn(),
    playCard: vi.fn(),
    host: vi.fn(),
  };
}
