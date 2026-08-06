"use client";

import { useDownloadApk } from "@/hooks/useDownloadApk";
import { APP } from "@/lib/app";
import { cn } from "@/lib/utils";

function formatBytes(bytes: number | null): string {
  if (!bytes || bytes <= 0) return "-- MB";
  const mb = bytes / (1024 * 1024);
  return `${mb.toFixed(1)} MB`;
}

export function DownloadButton() {
  const { state, startDownload } = useDownloadApk();

  const busy = state.status === "downloading";

  const label =
    state.status === "success"
      ? "DOWNLOAD STARTED"
      : busy
        ? "PREPARING..."
        : `DOWNLOAD v${state.version}`;

  return (
    <button
      type="button"
      onClick={() => {
        void startDownload();
      }}
      disabled={busy}
      className={cn(
        "w-full px-8 py-4 text-center font-mono text-sm font-bold tracking-[0.3em] text-black transition-all",
        busy
          ? "cursor-wait bg-[#7A7A7A]"
          : "bg-[#00FF9C] hover:opacity-85"
      )}
      aria-live="polite"
    >
      {label}
    </button>
  );
}

export function DownloadMeta() {
  const { state } = useDownloadApk();
  return (
    <dl className="flex flex-wrap gap-x-8 gap-y-2 font-mono text-xs text-[#7A7A7A]">
      <div className="flex gap-2">
        <dt className="text-[#E8E8E8]">FILE:</dt>
        <dd>{state.fileName}</dd>
      </div>
      <div className="flex gap-2">
        <dt className="text-[#E8E8E8]">SIZE:</dt>
        <dd>{formatBytes(state.sizeBytes)}</dd>
      </div>
      <div className="flex gap-2">
        <dt className="text-[#E8E8E8]">ANDROID:</dt>
        <dd>{APP.minAndroid}</dd>
      </div>
    </dl>
  );
}
