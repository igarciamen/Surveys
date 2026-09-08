import { OptionCount } from "./option-count";

export interface QuestionResult {
  questionId: number;
  label: string;
  type: string;
  totalAnswered: number;
  options?: OptionCount[];   // choice / dropdown / rating
  average?: number;          // rating / number
  trueCount?: number;        // boolean
  falseCount?: number;       // boolean
  samples?: string[];        // text / textarea / date
}