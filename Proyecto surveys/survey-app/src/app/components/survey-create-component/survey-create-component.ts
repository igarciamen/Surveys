import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { SurveyService } from '../../service/survey-service';
import { Survey, Question, QuestionType, SurveyAccess } from '../../model/survey';

@Component({
  selector: 'app-survey-create-component',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './survey-create-component.html',
  styleUrl: './survey-create-component.css',
})
export class SurveyCreateComponent implements OnInit {
  editId?: number;
  loading = false;
  saving = false;
  errorMessage = '';
  closesAtLocal = '';

  survey: Survey = {
    title: '',
    description: '',
    access: 'OPEN',
    accessPassword: '',
    questions: [],
  };

  readonly questionTypes: { value: QuestionType; label: string }[] = [
    { value: 'TEXT', label: 'Short text' },
    { value: 'TEXTAREA', label: 'Long text' },
    { value: 'NUMBER', label: 'Number' },
    { value: 'DATE', label: 'Date' },
    { value: 'BOOLEAN', label: 'Yes / No' },
    { value: 'SINGLE_CHOICE', label: 'Single choice' },
    { value: 'MULTIPLE_CHOICE', label: 'Multiple choice' },
    { value: 'DROPDOWN', label: 'Dropdown' },
    { value: 'RATING', label: 'Rating' },
  ];

  readonly accessTypes: { value: SurveyAccess; label: string }[] = [
    { value: 'OPEN', label: 'Open to everyone' },
    { value: 'PASSWORD', label: 'Protected by password' },
    { value: 'RESTRICTED', label: 'Registered users only' },
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private surveyService: SurveyService,
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.editId = Number(idParam);
      this.loading = true;
      this.surveyService.getOwned(this.editId).subscribe({
next: (s) => {
  s.questions = (s.questions || []).map((q) => ({ ...q, options: q.options || [] }));
  this.survey = s;
  this.closesAtLocal = s.closesAt ? this.toLocalInput(s.closesAt) : '';
  this.loading = false;
},
        error: () => {
          this.errorMessage = 'Could not load the survey for editing.';
          this.loading = false;
        },
      });
    }
  }

  get isEdit(): boolean { return this.editId != null; }

  needsOptions(type: QuestionType): boolean {
    return ['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'DROPDOWN', 'RATING'].includes(type);
  }

  addQuestion(): void {
    const q: Question = { type: 'TEXT', label: '', helpText: '', required: false, options: [] };
    this.survey.questions.push(q);
  }

  removeQuestion(i: number): void {
    this.survey.questions.splice(i, 1);
  }

  onTypeChange(q: Question): void {
    if (this.needsOptions(q.type) && q.options.length === 0) {
      q.options.push({ label: '', value: '' });
    }
  }

  addOption(q: Question): void {
    q.options.push({ label: '', value: '' });
  }

  removeOption(q: Question, j: number): void {
    q.options.splice(j, 1);
  }

  private toLocalInput(iso: string): string {
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

  save(): void {
    this.errorMessage = '';
    if (!this.survey.title.trim()) {
      this.errorMessage = 'The survey needs a title.';
      return;
    }
 const payload: Survey = { ...this.survey };
if (payload.access !== 'PASSWORD') { payload.accessPassword = undefined; }
payload.closesAt = this.closesAtLocal ? new Date(this.closesAtLocal).toISOString() : undefined;

    this.saving = true;
    const request = this.isEdit
      ? this.surveyService.update(this.editId!, payload)
      : this.surveyService.create(payload);

    request.subscribe({
      next: () => { this.saving = false; this.router.navigate(['/surveys/manage']); },
      error: (err) => {
        this.saving = false;
        this.errorMessage = err.error?.message || 'Could not save the survey.';
      },
    });
  }
}