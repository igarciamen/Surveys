import { of, throwError } from 'rxjs';
import { ActivatedRoute } from '@angular/router';

import { SurveyResultsComponent } from './survey-results-component';
import { ResponseService } from '../../service/response-service';
import { SurveyResults } from '../../model/survey-results';
import { QuestionResult } from '../../model/question-result';

describe('SurveyResultsComponent', () => {
  let responseService: jasmine.SpyObj<ResponseService>;
  let route: ActivatedRoute;

  const sampleResults: SurveyResults = {
    surveyId: 35,
    title: 'Survey about your home',
    totalResponses: 4,
    questions: [
      { questionId: 70, label: 'colour', type: 'SINGLE_CHOICE', totalAnswered: 4,
        options: [ { optionId: 1, label: 'Red', count: 1 }, { optionId: 2, label: 'Blue', count: 3 } ] },
    ],
  };

  function build(): SurveyResultsComponent {
    return new SurveyResultsComponent(route, responseService);
  }

  beforeEach(() => {
    responseService = jasmine.createSpyObj('ResponseService', ['getResults']);
    route = { snapshot: { paramMap: { get: () => '35' } } } as unknown as ActivatedRoute;
  });

  it('loads the results on init', () => {
    responseService.getResults.and.returnValue(of(sampleResults));

    const c = build();
    c.ngOnInit();

    expect(c.results).toBe(sampleResults);
    expect(c.forbidden).toBeFalse();
    expect(c.loading).toBeFalse();
  });

  it('sets forbidden when the backend returns 403', () => {
    responseService.getResults.and.returnValue(throwError(() => ({ status: 403 })));

    const c = build();
    c.ngOnInit();

    expect(c.forbidden).toBeTrue();
    expect(c.results).toBeUndefined();
  });

  it('percent computes a rounded percentage', () => {
    const c = build();
    const q = { totalAnswered: 4 } as QuestionResult;
    expect(c.percent(3, q)).toBe(75);
    expect(c.percent(1, q)).toBe(25);
  });

  it('percent returns 0 when nothing was answered', () => {
    const c = build();
    const q = { totalAnswered: 0 } as QuestionResult;
    expect(c.percent(2, q)).toBe(0);
  });

  it('isOption recognises choice-based types', () => {
    const c = build();
    expect(c.isOption('SINGLE_CHOICE')).toBeTrue();
    expect(c.isOption('RATING')).toBeTrue();
    expect(c.isOption('TEXT')).toBeFalse();
  });
});
