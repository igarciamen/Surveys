import { of } from 'rxjs';
import { SurveyStatisticsComponent } from './survey-statistics-component';

describe('SurveyStatisticsComponent (new responses indicator)', () => {

  function build(): SurveyStatisticsComponent {
    const routeStub: any = { snapshot: { paramMap: { get: () => '999' } } };
    const serviceSpy = jasmine.createSpyObj('StatisticsService', ['getOverview']);
    serviceSpy.getOverview.and.returnValue(of({
      surveyId: 999, title: 'Test', totalResponses: 0,
      responsesByDay: [], questions: [],
    }));
    return new SurveyStatisticsComponent(routeStub, serviceSpy);
  }

  it('shows nothing new on the first visit (establishes a baseline)', () => {
    const c = build();
    c.surveyId = 999;
    localStorage.removeItem('stats-seen:999');

    expect(c.markSeenAndCountNew(5)).toBe(0); // first visit: baseline, nothing is "new"
  });

  it('counts only the responses added since the last visit', () => {
    const c = build();
    c.surveyId = 999;
    localStorage.removeItem('stats-seen:999');

    c.markSeenAndCountNew(5);                 // baseline = 5
    expect(c.markSeenAndCountNew(5)).toBe(0); // no change
    expect(c.markSeenAndCountNew(8)).toBe(3); // 3 new since last time
    expect(c.markSeenAndCountNew(8)).toBe(0); // seen again, back to 0
  });

  it('never shows a negative number if the total goes down', () => {
    const c = build();
    c.surveyId = 999;
    localStorage.removeItem('stats-seen:999');

    c.markSeenAndCountNew(10);
    expect(c.markSeenAndCountNew(4)).toBe(0); // max(0, 4 - 10)
  });
});
