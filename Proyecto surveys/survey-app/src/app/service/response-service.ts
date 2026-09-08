import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

;
import { SurveyResults } from '../model/survey-results';
import { SubmissionRequest } from '../model/submission-request';
import { API_BASE } from '../api';

@Injectable({ providedIn: 'root' })
export class ResponseService {
 private base = `${API_BASE}/api/responses`;

  constructor(private http: HttpClient) {}

  submit(surveyId: number, submission: SubmissionRequest): Observable<unknown> {
    return this.http.post(`${this.base}/survey/${surveyId}`, submission);
  }

  getMine(surveyId: number): Observable<unknown> {
    return this.http.get(`${this.base}/survey/${surveyId}/mine`);
  }

  getResults(surveyId: number): Observable<SurveyResults> {
    return this.http.get<SurveyResults>(`${this.base}/survey/${surveyId}/results`);
  }

exportCsv(surveyId: number): Observable<Blob> {
  return this.http.get(`${this.base}/survey/${surveyId}/export/csv`, { responseType: 'blob' });
}
 
exportXlsx(surveyId: number): Observable<Blob> {
  return this.http.get(`${this.base}/survey/${surveyId}/export/xlsx`, { responseType: 'blob' });
}



}


