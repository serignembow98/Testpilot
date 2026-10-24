export type Status = 'PROPOSED' | 'APPROVED' | 'REJECTED';
export type Decision = Exclude<Status, 'PROPOSED'>;

export interface Suggestion {
  id: string;
  className: string;
  methodName: string;
  generatedTest: string;
  status: Status;
  createdAt: string;
}

export interface Stats {
  total: number;
  proposed: number;
  approved: number;
  rejected: number;
  acceptanceRate: number;
  redactions: number;
}

export interface AnalysisResult {
  className: string;
  suggestions: Suggestion[];
  redactions: number;
}
