import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Survey, SurveyStatus } from '../model/survey';
import { PageResponse } from '../model/page-response';
import { API_BASE } from '../api';


@Injectable({ providedIn: 'root' })
export class SurveyService {
  private base = `${API_BASE}/api/surveys`;

  constructor(private http: HttpClient) {}

  // ----- public catalog -----
  listPublished(q = '', page = 0, size = 9): Observable<PageResponse<Survey>> {
    let params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size));
    if (q && q.trim()) {
      params = params.set('q', q.trim());
    }
    return this.http.get<PageResponse<Survey>>(this.base, { params });
  }

  getPublished(id: number): Observable<Survey> {
    return this.http.get<Survey>(`${this.base}/${id}`);
  }

  // ----- designer area -----
  listMine(page = 0, size = 9): Observable<PageResponse<Survey>> {
    const params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size));
    return this.http.get<PageResponse<Survey>>(`${this.base}/mine`, { params });
  }

  getOwned(id: number): Observable<Survey> {
    return this.http.get<Survey>(`${this.base}/${id}/manage`);
  }

  create(survey: Survey): Observable<Survey> {
    return this.http.post<Survey>(this.base, survey);
  }

  update(id: number, survey: Survey): Observable<Survey> {
    return this.http.put<Survey>(`${this.base}/${id}`, survey);
  }

  changeStatus(id: number, status: SurveyStatus): Observable<Survey> {
    return this.http.patch<Survey>(`${this.base}/${id}/status`, { status });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
