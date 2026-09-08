export interface QuestionStat {
  questionId: number;
  label: string;
  type: string;
  answered: number;
  answerRate: number;   // 0..1
  highlight: string;
}