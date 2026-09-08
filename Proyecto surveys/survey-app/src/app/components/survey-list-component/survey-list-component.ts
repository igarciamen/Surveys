import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { SurveyService } from '../../service/survey-service';
import { Survey } from '../../model/survey';
import { PageResponse } from '../../model/page-response';

@Component({
  selector: 'app-survey-list-component',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './survey-list-component.html',
  styleUrl: './survey-list-component.css',
})
export class SurveyListComponent implements OnInit {
  surveys: Survey[] = [];
  q = '';
  page = 0;
  size = 9;
  totalPages = 0;
  totalElements = 0;
  loading = false;

  constructor(private surveyService: SurveyService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.surveyService.listPublished(this.q, this.page, this.size).subscribe({
      next: (res: PageResponse<Survey>) => {
        this.surveys = res.content;
        this.totalPages = res.page.totalPages;
        this.totalElements = res.page.totalElements;
        this.loading = false;
      },
      error: () => {
        this.surveys = [];
        this.loading = false;
      },
    });
  }

  search(): void {
    this.page = 0;
    this.load();
  }

  prev(): void {
    if (this.page > 0) { this.page--; this.load(); }
  }

  next(): void {
    if (this.page + 1 < this.totalPages) { this.page++; this.load(); }
  }
}
