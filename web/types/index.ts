export type DownloadStatus = "idle" | "downloading" | "success" | "error";

export interface DownloadState {
  status: DownloadStatus;
  fileName: string;
  sizeBytes: number | null;
  version: string;
  error: string | null;
  startedAt: number | null;
}

export interface Feature {
  title: string;
  description: string;
  tag: string;
}

export interface CommandExample {
  command: string;
  output: string[];
  description: string;
}

export interface ScreenshotItem {
  src: string;
  alt: string;
  caption: string;
  width: number;
  height: number;
}
