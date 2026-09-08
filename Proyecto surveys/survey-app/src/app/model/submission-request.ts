import { AnswerInput } from "./answer-input";

export interface SubmissionRequest {
  answers: AnswerInput[];
  // Only sent when the survey access mode is PASSWORD.
  accessPassword?: string;
}
