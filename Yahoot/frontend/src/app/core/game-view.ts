import { GameEvent, GamePhase, GameSnapshot, PlayerScore, QuestionStarted } from './game-events';

/** Was Beamer und Handy aus den Ereignissen eines Spiels zusammensetzen. */
export interface GameView {
  phase: GamePhase;
  players: string[];
  question: QuestionStarted | null;
  correctOption: number | null;
  ranking: PlayerScore[];
  sabotageSeconds: number;
}

export const EMPTY_VIEW: GameView = {
  phase: 'LOBBY',
  players: [],
  question: null,
  correctOption: null,
  ranking: [],
  sabotageSeconds: 0,
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
      return { ...view, phase: 'SABOTAGE', ranking: event.ranking, sabotageSeconds: event.seconds };
    case 'GameFinished':
      return { ...view, phase: 'FINISHED', ranking: event.ranking };
  }
}

/** Baut den Spielstand nach einem Neuladen oder Verbindungsabbruch wieder auf. */
export function applySnapshot(snapshot: GameSnapshot): GameView {
  const question: QuestionStarted | null =
    snapshot.text === null
      ? null
      : {
          type: 'QuestionStarted',
          code: snapshot.code,
          number: snapshot.round,
          total: snapshot.total,
          text: snapshot.text,
          options: snapshot.options,
          seconds: snapshot.secondsLeft,
          attacks: snapshot.attacks,
        };
  return {
    phase: snapshot.phase,
    players: snapshot.players,
    question,
    correctOption: snapshot.correctOption,
    ranking: snapshot.ranking,
    sabotageSeconds: 0,
  };
}
