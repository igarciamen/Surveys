export type SurveyStatus = 'DRAFT' | 'PUBLISHED' | 'CLOSED';
export type SurveyAccess = 'OPEN' | 'PASSWORD' | 'RESTRICTED';
export type QuestionType =
  | 'TEXT' | 'TEXTAREA' | 'NUMBER' | 'DATE' | 'BOOLEAN'
  | 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'DROPDOWN' | 'RATING';

export interface QuestionOption {
  id?: number;
  label: string;
  value?: string;
  position?: number;
}

export interface Question {
  id?: number;
  type: QuestionType;
  label: string;
  helpText?: string;
  required: boolean;
  position?: number;
  options: QuestionOption[];
}

export interface SurveyOwner {
  id: number;
  username: string;
  email: string;
  roles: string[];
}

export interface Survey {
  id?: number;
  ownerUserId?: number;
  title: string;
  description?: string;
  status?: SurveyStatus;
  access: SurveyAccess;
  accessPassword?: string;
  publishedAt?: string;
  closesAt?: string;
  createdAt?: string;
  updatedAt?: string;
  owner?: SurveyOwner;
  questions: Question[];
}
