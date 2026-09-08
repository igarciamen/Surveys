import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Invitation } from '../model/invitation';
import { InvitationToken } from '../model/invitation-token';
import { API_BASE } from '../api';

@Injectable({ providedIn: 'root' })
export class InvitationService {
  private base = `${API_BASE}/api/invitations`;

  constructor(private http: HttpClient) {}

  create(surveyId: number, emails: string[]): Observable<Invitation[]> {
    return this.http.post<Invitation[]>(`${this.base}/survey/${surveyId}`, { emails });
  }

  list(surveyId: number): Observable<Invitation[]> {
    return this.http.get<Invitation[]>(`${this.base}/survey/${surveyId}`);
  }

  getByToken(token: string): Observable<InvitationToken> {
    return this.http.get<InvitationToken>(`${this.base}/token/${token}`);
  }

  accept(token: string): Observable<InvitationToken> {
    return this.http.post<InvitationToken>(`${this.base}/token/${token}/accept`, {});
  }

  delete(invitationId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${invitationId}`);
  }
}
