/** Die Nachrichten des Servers, siehe docs/api.md. */

export type Card = 'FREEZE' | 'SHUFFLE' | 'STEAL' | 'SHIELD';
export type GamePhase = 'LOBBY' | 'QUESTION' | 'REVEAL' | 'SABOTAGE' | 'FINISHED';
export type AnswerResult = 'ACCEPTED' | 'ALREADY_ANSWERED' | 'FROZEN' | 'TOO_LATE' | 'INVALID_OPTION';

export interface PlayerScore {
  name: string;
  score: number;
  energy: number;
}

export interface Attack {
  attacker: string;
  card: Card;
  target: string;
  blocked: boolean;
}

export interface PlayerJoined {
  type: 'PlayerJoined';
  code: string;
  name: string;
  players: string[];
}

export interface QuestionStarted {
  type: 'QuestionStarted';
  code: string;
  number: number;
  total: number;
  text: string;
  options: string[];
  seconds: number;
  attacks: Attack[];
}

export interface QuestionClosed {
  type: 'QuestionClosed';
  code: string;
  number: number;
  correctOption: number;
  ranking: PlayerScore[];
}

export interface SabotageOpened {
  type: 'SabotageOpened';
  code: string;
  number: number;
  seconds: number;
  ranking: PlayerScore[];
}

export interface GameFinished {
  type: 'GameFinished';
  code: string;
  ranking: PlayerScore[];
}

export type GameEvent = PlayerJoined | QuestionStarted | QuestionClosed | SabotageOpened | GameFinished;

/** Der ganze Spielstand fuer alle, die neu laden oder die Verbindung verloren haben. */
export interface GameSnapshot {
  code: string;
  phase: GamePhase;
  round: number;
  total: number;
  text: string | null;
  options: string[];
  secondsLeft: number;
  correctOption: number | null;
  attacks: Attack[];
  players: string[];
  ranking: PlayerScore[];
}

export interface CardPlayed {
  card: Card;
  target: string;
}

export interface QuizSummary {
  id: string;
  title: string;
  questions: number;
}

export interface CreatedGame {
  code: string;
  hostToken: string;
}
