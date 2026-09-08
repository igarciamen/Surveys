import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { SurveyService } from '../../service/survey-service';
import { Survey, SurveyStatus } from '../../model/survey';

@Component({
  selector: 'app-my-surveys-component',
  imports: [CommonModule, RouterLink],
  templateUrl: './my-surveys-component.html',
  styleUrl: './my-surveys-component.css',
})
export class MySurveysComponent implements OnInit {
  surveys: Survey[] = [];
  page = 0;
  size = 10;
  totalPages = 0;
  loading = false;
  message = '';

  constructor(private surveyService: SurveyService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.surveyService.listMine(this.page, this.size).subscribe({
      next: (res) => { this.surveys = res.content; this.totalPages = res.page.totalPages; this.loading = false; },
      error: () => { this.loading = false; },
    });
  }

  publish(s: Survey): void { this.transition(s, 'PUBLISHED'); }
  close(s: Survey): void { this.transition(s, 'CLOSED'); }

  private transition(s: Survey, status: SurveyStatus): void {
    if (!s.id) { return; }
    this.message = '';
    this.surveyService.changeStatus(s.id, status).subscribe({
      next: () => this.load(),
      error: (err) => { this.message = err.error?.message || `Could not change status to ${status}.`; },
    });
  }

  remove(s: Survey): void {
    if (!s.id || !confirm(`Delete "${s.title}"?`)) { return; }
    this.surveyService.delete(s.id).subscribe({
      next: () => this.load(),
      error: (err) => { this.message = err.error?.message || 'Could not delete the survey.'; },
    });
  }

  badgeClass(status?: SurveyStatus): string {
    switch (status) {
      case 'PUBLISHED': return 'bg-success';
      case 'CLOSED': return 'bg-secondary';
      default: return 'bg-warning text-dark';
    }
  }

  prev(): void { if (this.page > 0) { this.page--; this.load(); } }
  next(): void { if (this.page + 1 < this.totalPages) { this.page++; this.load(); } }
}
