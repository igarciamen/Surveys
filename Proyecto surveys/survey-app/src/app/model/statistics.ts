import { DailyCount } from "./daily-count";
import { QuestionStat } from "./question-stat";

export interface Statistics {
  surveyId: number;
  title: string;
  totalResponses: number;
  firstResponseAt?: string;
  lastResponseAt?: string;
  responsesByDay: DailyCount[];
  questions: QuestionStat[];
}
