import { of, throwError } from 'rxjs';
import { ActivatedRoute } from '@angular/router';

import { SurveyAnswerComponent } from './survey-answer-component';
import { SurveyService } from '../../service/survey-service';
import { ResponseService } from '../../service/response-service';
import { AuthService } from '../../service/auth-service';
import { InvitationService } from '../../service/invitation-service';
import { Survey, SurveyAccess } from '../../model/survey';

describe('SurveyAnswerComponent', () => {
  let surveyService: jasmine.SpyObj<SurveyService>;
  let responseService: jasmine.SpyObj<ResponseService>;
  let auth: jasmine.SpyObj<AuthService>;
  let invitationService: jasmine.SpyObj<InvitationService>;
  let route: ActivatedRoute;

  function surveyWith(access: SurveyAccess): Survey {
    return {
      id: 35,
      title: 'Survey about your home',
      access,
      questions: [
        { id: 67, type: 'TEXT', label: 'address', required: true, options: [] },
        { id: 70, type: 'SINGLE_CHOICE', label: 'colour', required: false,
          options: [ { id: 1, label: 'Red' }, { id: 2, label: 'Blue' } ] },
        { id: 71, type: 'MULTIPLE_CHOICE', label: 'languages', required: false,
          options: [ { id: 3, label: 'ES' }, { id: 4, label: 'EN' } ] },
      ],
    };
  }

  function build(): SurveyAnswerComponent {
    return new SurveyAnswerComponent(route, surveyService, responseService, auth, invitationService);
  }

  beforeEach(() => {
    surveyService = jasmine.createSpyObj('SurveyService', ['getPublished']);
    responseService = jasmine.createSpyObj('ResponseService', ['getMine', 'submit']);
    auth = jasmine.createSpyObj('AuthService', ['isAuthenticated']);
    invitationService = jasmine.createSpyObj('InvitationService', ['accept']);
    invitationService.accept.and.returnValue(of({
        surveyId: 35,
        surveyTitle: 'Survey about your home',
        email: 'test@example.com',
        status: 'RESPONDED',
}));
    // paramMap provides :id (35); queryParamMap provides ?invite (null = no invitation).
    route = {
      snapshot: {
        paramMap: { get: () => '35' },
        queryParamMap: { get: () => null },
      },
    } as unknown as ActivatedRoute;
  });

  it('marks alreadyAnswered when the logged-in user already answered', () => {
    surveyService.getPublished.and.returnValue(of(surveyWith('OPEN')));
    auth.isAuthenticated.and.returnValue(true);
    responseService.getMine.and.returnValue(of({}));

    const c = build();
    c.ngOnInit();

    expect(c.alreadyAnswered).toBeTrue();
    expect(c.loading).toBeFalse();
  });

  it('builds the form when the logged-in user has not answered (404)', () => {
    surveyService.getPublished.and.returnValue(of(surveyWith('OPEN')));
    auth.isAuthenticated.and.returnValue(true);
    responseService.getMine.and.returnValue(throwError(() => ({ status: 404 })));

    const c = build();
    c.ngOnInit();

    expect(c.working.length).toBe(3);
    expect(c.working[0].textValue).toBe('');
  });

  it('lets an anonymous user answer an OPEN survey without checking getMine', () => {
    surveyService.getPublished.and.returnValue(of(surveyWith('OPEN')));
    auth.isAuthenticated.and.returnValue(false);

    const c = build();
    c.ngOnInit();

    expect(c.needsLogin).toBeFalse();
    expect(c.working.length).toBe(3);
    expect(responseService.getMine).not.toHaveBeenCalled();
  });

  it('requires login for a RESTRICTED survey when anonymous', () => {
    surveyService.getPublished.and.returnValue(of(surveyWith('RESTRICTED')));
    auth.isAuthenticated.and.returnValue(false);

    const c = build();
    c.ngOnInit();

    expect(c.needsLogin).toBeTrue();
    expect(c.working.length).toBe(0);
    expect(responseService.getMine).not.toHaveBeenCalled();
  });

  it('classifies question types correctly', () => {
    const c = build();
    expect(c.isSingle('RATING')).toBeTrue();
    expect(c.isSingle('MULTIPLE_CHOICE')).toBeFalse();
    expect(c.isMulti('MULTIPLE_CHOICE')).toBeTrue();
  });

  it('toggleMulti adds and removes option ids', () => {
    const c = build();
    const w = { question: surveyWith('OPEN').questions[2], textValue: '', single: null, multi: [] as number[] };

    c.toggleMulti(w as never, 3, true);
    expect(w.multi).toEqual([3]);
    c.toggleMulti(w as never, 3, false);
    expect(w.multi).toEqual([]);
  });

  it('submit builds the correct payload and marks submitted', () => {
    surveyService.getPublished.and.returnValue(of(surveyWith('OPEN')));
    auth.isAuthenticated.and.returnValue(true);
    responseService.getMine.and.returnValue(throwError(() => ({ status: 404 })));
    responseService.submit.and.returnValue(of({}));

    const c = build();
    c.ngOnInit();

    c.working[0].textValue = 'Calle Mayor 12';
    c.working[1].single = 2;
    c.working[2].multi = [3, 4];

    c.submit();

    const [surveyId, payload] = responseService.submit.calls.mostRecent().args;
    expect(surveyId).toBe(35);
    expect(payload.answers).toEqual([
      { questionId: 67, textValue: 'Calle Mayor 12' },
      { questionId: 70, selectedOptionIds: [2] },
      { questionId: 71, selectedOptionIds: [3, 4] },
    ]);
    expect(c.submitted).toBeTrue();
  });

  it('requires login for a PASSWORD survey when anonymous', () => {
    surveyService.getPublished.and.returnValue(of(surveyWith('PASSWORD')));
    auth.isAuthenticated.and.returnValue(false);

    const c = build();
    c.ngOnInit();

    expect(c.needsLogin).toBeTrue();
    expect(responseService.getMine).not.toHaveBeenCalled();
  });

  it('blocks submit on a PASSWORD survey (logged in) until a password is typed', () => {
    surveyService.getPublished.and.returnValue(of(surveyWith('PASSWORD')));
    auth.isAuthenticated.and.returnValue(true);
    responseService.getMine.and.returnValue(throwError(() => ({ status: 404 })));

    const c = build();
    c.ngOnInit();

    c.submit();  // no password yet
    expect(responseService.submit).not.toHaveBeenCalled();
    expect(c.errorMessage).toContain('password');

    responseService.submit.and.returnValue(of({}));
    c.accessPassword = 'secret';
    c.submit();

    const [, payload] = responseService.submit.calls.mostRecent().args;
    expect(payload.accessPassword).toBe('secret');
    expect(c.submitted).toBeTrue();
  });

  it('submit sets alreadyAnswered when the backend returns 409', () => {
    surveyService.getPublished.and.returnValue(of(surveyWith('OPEN')));
    auth.isAuthenticated.and.returnValue(true);
    responseService.getMine.and.returnValue(throwError(() => ({ status: 404 })));
    responseService.submit.and.returnValue(throwError(() => ({ status: 409 })));

    const c = build();
    c.ngOnInit();
    c.submit();

    expect(c.alreadyAnswered).toBeTrue();
  });

  it('accepts the invitation after a successful submit when ?invite is present', () => {
    // Override the route so queryParamMap returns an invitation token.
    route = {
      snapshot: {
        paramMap: { get: () => '35' },
        queryParamMap: { get: () => 'TOKEN123' },
      },
    } as unknown as ActivatedRoute;

    surveyService.getPublished.and.returnValue(of(surveyWith('OPEN')));
    auth.isAuthenticated.and.returnValue(false);
    responseService.submit.and.returnValue(of({}));

    const c = build();
    c.ngOnInit();
    c.working[0].textValue = 'Calle Mayor 12';  // required question
    c.submit();

    expect(invitationService.accept).toHaveBeenCalledWith('TOKEN123');
  });
});