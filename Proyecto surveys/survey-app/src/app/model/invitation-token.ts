export interface InvitationToken {
  surveyId: number;
  surveyTitle: string;
  email: string;
  status: 'PENDING' | 'RESPONDED';
}