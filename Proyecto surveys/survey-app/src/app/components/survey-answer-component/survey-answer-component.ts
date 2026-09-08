import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { SurveyService } from '../../service/survey-service';
import { ResponseService } from '../../service/response-service';
import { AuthService } from '../../service/auth-service';
import { Survey, Question } from '../../model/survey';
import { AnswerInput } from '../../model/answer-input';
import { SubmissionRequest } from '../../model/submission-request';
import { InvitationService } from '../../service/invitation-service';


interface WorkingAnswer {
  question: Question;
  textValue: string;
  single: number | null;   // selected option id for single / dropdown / rating
  multi: number[];         // selected option ids for multiple choice
}

@Component({
  selector: 'app-survey-answer-component',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './survey-answer-component.html',
  styleUrl: './survey-answer-component.css',
})
export class SurveyAnswerComponent implements OnInit {
  surveyId!: number;
  survey?: Survey;
  working: WorkingAnswer[] = [];

  loading = true;
  alreadyAnswered = false;
  needsLogin = false;       // RESTRICTED survey and no session
  submitted = false;
  saving = false;
  errorMessage = '';

  accessPassword = '';       // typed by the user for PASSWORD surveys
  inviteToken: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private surveyService: SurveyService,
    private responseService: ResponseService,
    private auth: AuthService,
    private invitationService: InvitationService
  ) {}

  ngOnInit(): void {
    this.surveyId = Number(this.route.snapshot.paramMap.get('id'));
    this.inviteToken = this.route.snapshot.queryParamMap.get('invite');
    // The public detail also tells us the access mode (OPEN / PASSWORD / RESTRICTED).
    this.surveyService.getPublished(this.surveyId).subscribe({
      next: (s) => { this.survey = s; this.afterSurveyLoaded(); },
      error: () => { this.errorMessage = 'This survey is not available.'; this.loading = false; },
    });
  }

  private afterSurveyLoaded(): void {
    const access = this.survey!.access;

    // RESTRICTED and PASSWORD surveys require a registered account.
    if ((access === 'RESTRICTED' || access === 'PASSWORD') && !this.auth.isAuthenticated()) {
      this.needsLogin = true;
      this.loading = false;
      return;
    }

    // Logged-in users: check whether they already answered (one submission per user).
    if (this.auth.isAuthenticated()) {
      this.responseService.getMine(this.surveyId).subscribe({
        next: () => { this.alreadyAnswered = true; this.loading = false; },
        error: (err) => {
          if (err.status === 404) { this.buildForm(); }
          else { this.errorMessage = 'Could not load the survey.'; this.loading = false; }
        },
      });
    } else {
      // Anonymous respondent on an OPEN or PASSWORD survey.
      this.buildForm();
    }
  }

  private buildForm(): void {
    this.working = this.survey!.questions.map((q) => ({
      question: q, textValue: '', single: null, multi: [],
    }));
    this.loading = false;
  }

  get passwordRequired(): boolean {
    return this.survey?.access === 'PASSWORD';
  }

  isSingle(type: string): boolean {
    return ['SINGLE_CHOICE', 'DROPDOWN', 'RATING'].includes(type);
  }
  isMulti(type: string): boolean {
    return type === 'MULTIPLE_CHOICE';
  }

  toggleMulti(w: WorkingAnswer, optionId: number, checked: boolean): void {
    if (checked) {
      if (!w.multi.includes(optionId)) { w.multi.push(optionId); }
    } else {
      w.multi = w.multi.filter((id) => id !== optionId);
    }
  }

  submit(): void {
    this.errorMessage = '';
    if (this.passwordRequired && !this.accessPassword.trim()) {
      this.errorMessage = 'This survey requires a password.';
      return;
    }

    const answers: AnswerInput[] = this.working.map((w) => {
      const q = w.question;
      if (this.isSingle(q.type)) {
        return { questionId: q.id!, selectedOptionIds: w.single != null ? [w.single] : [] };
      }
      if (this.isMulti(q.type)) {
        return { questionId: q.id!, selectedOptionIds: w.multi };
      }
      return { questionId: q.id!, textValue: String(w.textValue ?? '') };
    });

    const payload: SubmissionRequest = { answers };
    if (this.passwordRequired) {
      payload.accessPassword = this.accessPassword;
    }

    this.saving = true;
this.responseService.submit(this.surveyId, payload).subscribe({
  next: () => {
    this.saving = false;
    this.submitted = true;
    if (this.inviteToken) {
      this.invitationService.accept(this.inviteToken).subscribe({
        next: () => {}, error: () => {}, // no bloquea la confirmación de respuesta
      });
    }
  },
  error: (err) => {
    this.saving = false;
    if (err.status === 409) { this.alreadyAnswered = true; }
    else { this.errorMessage = err.error?.message || 'Could not submit your answers.'; }
  },
});
  }
}
