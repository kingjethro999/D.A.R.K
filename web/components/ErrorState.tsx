import { cn } from "@/lib/utils";

interface ErrorStateProps {
  title?: string;
  message: string;
  retryLabel?: string;
  onRetry?: () => void;
  className?: string;
}

export function ErrorState({
  title = "Something went wrong",
  message,
  retryLabel = "TRY AGAIN",
  onRetry,
  className,
}: ErrorStateProps) {
  return (
    <div
      role="alert"
      className={cn(
        "flex w-full flex-col items-center gap-3 border border-[#FF6B6B]/40 bg-[#FF6B6B]/5 px-5 py-4 text-center",
        className
      )}
    >
      <p className="font-mono text-xs tracking-[0.25em] text-[#FF6B6B]">
        {"\u2715"} {title.toUpperCase()}
      </p>
      <p className="font-mono text-sm leading-relaxed text-[#E8E8E8]/80">
        {message}
      </p>
      {onRetry ? (
        <button
          type="button"
          onClick={onRetry}
          className="mt-1 border border-[#00FF9C]/50 px-4 py-2 font-mono text-xs tracking-[0.2em] text-[#00FF9C] transition-colors hover:bg-[#00FF9C]/10"
        >
          {retryLabel}
        </button>
      ) : null}
    </div>
  );
}
