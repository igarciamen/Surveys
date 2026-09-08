import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { ResponseService } from './response-service';
import { SubmissionRequest } from '../model/submission-request';
import { API_BASE } from '../api';


describe('ResponseService', () => {
  let service: ResponseService;
  let httpMock: HttpTestingController;
const base = `${API_BASE}/api/responses`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        ResponseService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(ResponseService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('submit() posts the submission to the survey endpoint', () => {
    const payload: SubmissionRequest = { answers: [{ questionId: 67, textValue: 'Calle Mayor 12' }] };

    service.submit(35, payload).subscribe();

    const req = httpMock.expectOne(`${base}/survey/35`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({});
  });

  it('getMine() requests the "mine" endpoint with GET', () => {
    service.getMine(35).subscribe();

    const req = httpMock.expectOne(`${base}/survey/35/mine`);
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('getResults() requests the "results" endpoint with GET', () => {
    service.getResults(35).subscribe();

    const req = httpMock.expectOne(`${base}/survey/35/results`);
    expect(req.request.method).toBe('GET');
    req.flush({ surveyId: 35, title: 'x', totalResponses: 0, questions: [] });
  });
});
