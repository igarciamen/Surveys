import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { ResponseService } from '../../service/response-service';
import { SurveyResults } from '../../model/survey-results';
import { QuestionResult } from '../../model/question-result';

@Component({
  selector: 'app-survey-results-component',
  imports: [CommonModule, RouterLink],
  templateUrl: './survey-results-component.html',
  styleUrl: './survey-results-component.css',
})
export class SurveyResultsComponent implements OnInit {
  surveyId!: number;
  results?: SurveyResults;
  loading = true;
  forbidden = false;
  errorMessage = '';
  exporting = false;
 
  constructor(private route: ActivatedRoute, private responseService: ResponseService) {}

  ngOnInit(): void {
    this.surveyId = Number(this.route.snapshot.paramMap.get('id'));
    this.responseService.getResults(this.surveyId).subscribe({
      next: (res) => { this.results = res; this.loading = false; },
      error: (err) => {
        if (err.status === 403) { this.forbidden = true; }
        else { this.errorMessage = err.error?.message || 'Could not load the results.'; }
        this.loading = false;
      },
    });
  }

  percent(count: number, q: QuestionResult): number {
    if (!q.totalAnswered) { return 0; }
    return Math.round((count / q.totalAnswered) * 100);
  }

  isOption(type: string): boolean {
    return ['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'DROPDOWN', 'RATING'].includes(type);
  }

  downloadCsv(): void {
  this.exporting = true;
  this.responseService.exportCsv(this.surveyId).subscribe({
    next: (blob) => { this.saveFile(blob, `survey-${this.surveyId}-responses.csv`); this.exporting = false; },
    error: () => { this.exporting = false; },
  });
}
 
downloadXlsx(): void {
  this.exporting = true;
  this.responseService.exportXlsx(this.surveyId).subscribe({
    next: (blob) => { this.saveFile(blob, `survey-${this.surveyId}-responses.xlsx`); this.exporting = false; },
    error: () => { this.exporting = false; },
  });
}
 
private saveFile(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url; a.download = filename; a.click();
  URL.revokeObjectURL(url);
}

}
