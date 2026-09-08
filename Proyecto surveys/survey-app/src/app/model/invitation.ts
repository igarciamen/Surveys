export interface Invitation {
  id: number;
  surveyId: number;
  email: string;
  token: string;
  status: 'PENDING' | 'RESPONDED';
  createdAt: string;
  respondedAt?: string;
}