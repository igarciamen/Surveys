import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { SurveyService } from '../../service/survey-service';
import { Survey } from '../../model/survey';

@Component({
  selector: 'app-survey-detail-component',
  imports: [CommonModule, RouterLink],
  templateUrl: './survey-detail-component.html',
  styleUrl: './survey-detail-component.css',
})
export class SurveyDetailComponent implements OnInit {
  survey?: Survey;
  loading = true;
  notFound = false;

  constructor(private route: ActivatedRoute, private surveyService: SurveyService) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.surveyService.getPublished(id).subscribe({
      next: (s) => { this.survey = s; this.loading = false; },
      error: () => { this.notFound = true; this.loading = false; },
    });
  }
}
