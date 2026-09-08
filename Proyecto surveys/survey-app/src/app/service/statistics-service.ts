import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Statistics } from '../model/statistics';
import { API_BASE } from '../api';

@Injectable({ providedIn: 'root' })
export class StatisticsService {
  private base = `${API_BASE}/api/statistics`;

  constructor(private http: HttpClient) {}

  getOverview(surveyId: number): Observable<Statistics> {
    return this.http.get<Statistics>(`${this.base}/survey/${surveyId}`);
  }
}
