import { QuestionResult } from "./question-result";

export interface SurveyResults {
  surveyId: number;
  title: string;
  totalResponses: number;
  questions: QuestionResult[];
}
