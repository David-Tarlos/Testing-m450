import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CreatedGame, GameSnapshot, QuizSummary } from './game-events';

/** Die REST-Aufrufe: Quiz auflisten, Spiel erstellen und den Spielstand abfragen. */
@Injectable({ providedIn: 'root' })
export class GameApiService {
  private readonly http = inject(HttpClient);

  quizzes(): Observable<QuizSummary[]> {
    return this.http.get<QuizSummary[]>('/api/quizzes');
  }

  createGame(quizId: string): Observable<CreatedGame> {
    return this.http.post<CreatedGame>('/api/games', { quizId });
  }

  state(code: string): Observable<GameSnapshot> {
    return this.http.get<GameSnapshot>(`/api/games/${code}`);
  }
}
