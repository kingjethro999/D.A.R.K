export interface AuthorResult {
  name: string;
  headline: string;
  summary: string;
  facts: string[];
  sources: { title: string; url: string }[];
  truncated: boolean;
}

export interface AuthorError {
  error: string;
}

export const DEFAULT_AUTHOR = "King Jethro Jerry";
