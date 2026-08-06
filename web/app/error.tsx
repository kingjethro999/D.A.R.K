"use client";

import { useEffect } from "react";
import { ErrorState } from "@/components/ErrorState";
import { logger } from "@/lib/logger";

interface ErrorBoundaryProps {
  error: Error & { digest?: string };
  reset: () => void;
}

export default function ErrorBoundary({ error, reset }: ErrorBoundaryProps) {
  useEffect(() => {
    logger.error("app", "uncaught error in app tree", {
      message: error.message,
      digest: error.digest,
    });
  }, [error]);

  return (
    <main className="flex min-h-screen items-center justify-center bg-black p-6">
      <div className="w-full max-w-md">
        <ErrorState
          title="Fatal Error"
          message={`${error.message || "Unknown error"}${error.digest ? ` (${error.digest})` : ""}`}
          onRetry={reset}
        />
      </div>
    </main>
  );
}
