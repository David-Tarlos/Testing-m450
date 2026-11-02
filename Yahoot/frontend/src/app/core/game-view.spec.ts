import { applyEvent, EMPTY_VIEW } from './game-view';

describe('applyEvent', () => {
  it('uebernimmt die Spielerliste beim Beitritt', () => {
    const view = applyEvent(EMPTY_VIEW, {
      type: 'PlayerJoined',
      code: 'ABCDEF',
      name: 'Tim',
      players: ['Lena', 'Tim'],
    });

    expect(view.players).toEqual(['Lena', 'Tim']);
    expect(view.phase).toBe('LOBBY');
  });

  it('zeigt eine neue Frage und vergisst die alte Loesung', () => {
    const revealed = { ...EMPTY_VIEW, phase: 'REVEAL' as const, correctOption: 1 };

    const view = applyEvent(revealed, {
      type: 'QuestionStarted',
      code: 'ABCDEF',
      number: 2,
      total: 5,
      text: '3 + 3?',
      options: ['5', '6'],
      seconds: 20,
      attacks: [],
    });

    expect(view.phase).toBe('QUESTION');
    expect(view.question?.text).toBe('3 + 3?');
    expect(view.correctOption).toBeNull();
  });

  it('merkt sich Loesung und Rangliste bei der Aufloesung', () => {
    const ranking = [{ name: 'Lena', score: 900, energy: 2 }];

    const view = applyEvent(EMPTY_VIEW, {
      type: 'QuestionClosed',
      code: 'ABCDEF',
      number: 1,
      correctOption: 1,
      ranking,
    });

    expect(view.phase).toBe('REVEAL');
    expect(view.correctOption).toBe(1);
    expect(view.ranking).toEqual(ranking);
  });

  it('kennt Sabotage und Spielende', () => {
    const ranking = [{ name: 'Lena', score: 900, energy: 2 }];

    expect(applyEvent(EMPTY_VIEW, { type: 'SabotageOpened', code: 'X', ranking }).phase).toBe('SABOTAGE');
    expect(applyEvent(EMPTY_VIEW, { type: 'GameFinished', code: 'X', ranking }).phase).toBe('FINISHED');
  });
});
