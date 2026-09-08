import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { Invitation } from '../../model/invitation';
import { InvitationService } from '../../service/invitation-service';

@Component({
  selector: 'app-survey-invitations-component',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './survey-invitations-component.html',
  styleUrl: './survey-invitations-component.css',
})
export class SurveyInvitationsComponent implements OnInit {
  surveyId!: number;
  invitations: Invitation[] = [];
  loading = true;
  forbidden = false;
  errorMessage = '';

  emailsText = '';
  saving = false;
  copiedToken = '';

  constructor(private route: ActivatedRoute, private invitationService: InvitationService) {}

  ngOnInit(): void {
    this.surveyId = Number(this.route.snapshot.paramMap.get('id'));
    this.load();
  }

  private load(): void {
    this.invitationService.list(this.surveyId).subscribe({
      next: (list) => { this.invitations = list; this.loading = false; },
      error: (err) => {
        if (err.status === 403) { this.forbidden = true; }
        else { this.errorMessage = err.error?.message || 'Could not load invitations.'; }
        this.loading = false;
      },
    });
  }

  add(): void {
    // Split the textarea by commas, semicolons, spaces or new lines.
    const emails = this.emailsText
      .split(/[\s,;]+/)
      .map((e) => e.trim())
      .filter((e) => e.length > 0);

    if (emails.length === 0) { return; }

    this.saving = true;
    this.errorMessage = '';
    this.invitationService.create(this.surveyId, emails).subscribe({
      next: (list) => { this.invitations = list; this.emailsText = ''; this.saving = false; },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Could not create invitations.';
        this.saving = false;
      },
    });
  }

  linkFor(inv: Invitation): string {
    return `${location.origin}/surveys/${inv.surveyId}/answer?invite=${inv.token}`;
  }

  copy(inv: Invitation): void {
    const link = this.linkFor(inv);
    navigator.clipboard.writeText(link).then(() => {
      this.copiedToken = inv.token;
      setTimeout(() => { if (this.copiedToken === inv.token) { this.copiedToken = ''; } }, 2000);
    });
  }

  remove(inv: Invitation): void {
    if (!confirm(`Delete the invitation for ${inv.email}?`)) { return; }
    this.invitationService.delete(inv.id).subscribe({
      next: () => { this.invitations = this.invitations.filter((x) => x.id !== inv.id); },
      error: (err) => { this.errorMessage = err.error?.message || 'Could not delete the invitation.'; },
    });
  }

  get pendingCount(): number {
    return this.invitations.filter((i) => i.status === 'PENDING').length;
  }

  get respondedCount(): number {
    return this.invitations.filter((i) => i.status === 'RESPONDED').length;
  }
}
