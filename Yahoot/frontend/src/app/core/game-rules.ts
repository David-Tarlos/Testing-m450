import { Attack, Card } from './game-events';

/** Muss zur Regel im Backend passen. Der Server entscheidet, das Handy zeigt es nur an. */
export const FREEZE_SECONDS = 3;

export interface CardInfo {
  card: Card;
  label: string;
  cost: number;
  hint: string;
}

export const CARDS: CardInfo[] = [
  { card: 'FREEZE', label: 'Freeze', cost: 1, hint: 'Sperrt das Handy 3 Sekunden lang' },
  { card: 'SHUFFLE', label: 'Shuffle', cost: 1, hint: 'Die Antworten springen herum' },
  { card: 'STEAL', label: 'Klau', cost: 2, hint: 'Holt die Hälfte der nächsten Punkte' },
  { card: 'SHIELD', label: 'Schild', cost: 1, hint: 'Blockt den nächsten Angriff auf dich' },
];

export function cardLabel(card: Card): string {
  return CARDS.find((info) => info.card === card)?.label ?? card;
}

/** Was die Angriffe der letzten Sabotage-Phase auf diesem Handy auslösen. */
export function effectsFor(attacks: Attack[], me: string): { frozen: boolean; shuffled: boolean } {
  const hits = attacks.filter((attack) => attack.target === me && !attack.blocked);
  return {
    frozen: hits.some((attack) => attack.card === 'FREEZE'),
    shuffled: hits.some((attack) => attack.card === 'SHUFFLE'),
  };
}

export function attackText(attack: Attack): string {
  if (attack.blocked) {
    return `${attack.target} hat den Angriff von ${attack.attacker} geblockt`;
  }
  switch (attack.card) {
    case 'FREEZE':
      return `${attack.attacker} hat ${attack.target} eingefroren`;
    case 'SHUFFLE':
      return `${attack.attacker} hat bei ${attack.target} die Antworten vertauscht`;
    case 'STEAL':
      return `${attack.attacker} hat bei ${attack.target} geklaut`;
    case 'SHIELD':
      return `${attack.attacker} hat ein Schild aktiviert`;
  }
}

/** Eine neue Reihenfolge, die sich sicher von der aktuellen unterscheidet. */
export function shuffledOrder(current: number[]): number[] {
  if (current.length < 2) {
    return current;
  }
  const next = [...current];
  while (next.every((value, index) => value === current[index])) {
    for (let i = next.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [next[i], next[j]] = [next[j], next[i]];
    }
  }
  return next;
}
