import { Attack } from './game-events';
import { attackText, effectsFor, shuffledOrder } from './game-rules';

const attack = (card: Attack['card'], target: string, blocked = false): Attack => ({
  attacker: 'Lena',
  card,
  target,
  blocked,
});

describe('effectsFor', () => {
  it('findet die Angriffe auf den eigenen Namen', () => {
    expect(effectsFor([attack('FREEZE', 'Tim'), attack('SHUFFLE', 'Tim')], 'Tim')).toEqual({
      frozen: true,
      shuffled: true,
    });
  });

  it('ignoriert geblockte Angriffe und Angriffe auf andere', () => {
    expect(effectsFor([attack('FREEZE', 'Tim', true), attack('SHUFFLE', 'Mia')], 'Tim')).toEqual({
      frozen: false,
      shuffled: false,
    });
  });
});

describe('attackText', () => {
  it('beschreibt den Angriff fuer den Beamer', () => {
    expect(attackText(attack('FREEZE', 'Tim'))).toBe('Lena hat Tim eingefroren');
  });

  it('nennt ein Schild, das den Angriff geblockt hat', () => {
    expect(attackText(attack('STEAL', 'Tim', true))).toBe('Tim hat den Angriff von Lena geblockt');
  });
});

describe('shuffledOrder', () => {
  it('liefert alle Antworten in einer anderen Reihenfolge', () => {
    const order = shuffledOrder([0, 1, 2, 3]);

    expect([...order].sort()).toEqual([0, 1, 2, 3]);
    expect(order).not.toEqual([0, 1, 2, 3]);
  });
});
