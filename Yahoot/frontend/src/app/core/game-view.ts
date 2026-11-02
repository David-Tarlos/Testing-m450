import { GameEvent, GamePhase, PlayerScore, QuestionStarted } from './game-events';

/** Was Beamer und Handy aus den Ereignissen eines Spiels zusammensetzen. */
export interface GameView {
  phase: GamePhase;
  players: string[];
  question: QuestionStarted | null;
  correctOption: number | null;
  ranking: PlayerScore[];
}

export const EMPTY_VIEW: GameView = {
  phase: 'LOBBY',
  players: [],
  question: null,
  correctOption: null,
  ranking: [],
};

/** Reine Funktion, damit sich der Spielstand ohne Socket testen laesst. */
export function applyEvent(view: GameView, event: GameEvent): GameView {
  switch (event.type) {
    case 'PlayerJoined':
      return { ...view, players: event.players };
    case 'QuestionStarted':
      return { ...view, phase: 'QUESTION', question: event, correctOption: null };
    case 'QuestionClosed':
      return { ...view, phase: 'REVEAL', correctOption: event.correctOption, ranking: event.ranking };
    case 'SabotageOpened':
      return { ...view, phase: 'SABOTAGE', ranking: event.ranking };
    case 'GameFinished':
      return { ...view, phase: 'FINISHED', ranking: event.ranking };
  }
}
