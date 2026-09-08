import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { StatisticsService } from '../../service/statistics-service';
import { Statistics } from '../../model/statistics';
import { DailyCount } from '../../model/daily-count';

@Component({
  selector: 'app-survey-statistics-component',
  imports: [CommonModule, RouterLink],
  templateUrl: './survey-statistics-component.html',
  styleUrl: './survey-statistics-component.css',
})
export class SurveyStatisticsComponent implements OnInit {
  surveyId!: number;
  stats?: Statistics;
  loading = true;
  forbidden = false;
  errorMessage = '';
  newResponses = 0;

  constructor(private route: ActivatedRoute, private statisticsService: StatisticsService) {}

  ngOnInit(): void {
    this.surveyId = Number(this.route.snapshot.paramMap.get('id'));
    this.statisticsService.getOverview(this.surveyId).subscribe({
    next: (s) => {
    this.stats = s;
    this.newResponses = this.markSeenAndCountNew(s.totalResponses);
    this.loading = false;
  },
  error: (err) => { /* ... igual que ahora ... */ },
});

  }

  // Height (%) of a bar relative to the busiest day.
  barHeight(d: DailyCount): number {
    const max = Math.max(1, ...(this.stats?.responsesByDay.map((x) => x.count) ?? [1]));
    return Math.round((d.count / max) * 100);
  }

  get busiestDay(): DailyCount | null {
    if (!this.stats || this.stats.responsesByDay.length === 0) { return null; }
    return this.stats.responsesByDay.reduce((a, b) => (b.count > a.count ? b : a));
  }

  ratePercent(rate: number): number {
    return Math.round(rate * 100);
  }

  markSeenAndCountNew(total: number): number {
  const key = `stats-seen:${this.surveyId}`;
  const raw = localStorage.getItem(key);
  localStorage.setItem(key, String(total));       // recuerda el total actual como "visto"
  if (raw === null) { return 0; }                 // primera visita: solo fija la referencia
  return Math.max(0, total - Number(raw));        // nuevas = actual - visto (nunca negativo)
}

}
