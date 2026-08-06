"use client";

import { useState } from "react";

export function CopyButton({ text }: { text: string }) {
  const [copied, setCopied] = useState(false);

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      setCopied(false);
    }
  };

  return (
    <button
      type="button"
      onClick={copy}
      className="border border-[#1E1E1E] px-3 py-1 font-mono text-[10px] tracking-[0.2em] text-[#7A7A7A] transition-colors hover:border-[#00FF9C]/50 hover:text-[#00FF9C]"
    >
      {copied ? "COPIED" : "COPY"}
    </button>
  );
}
