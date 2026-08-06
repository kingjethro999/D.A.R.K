"use client";

import { useCallback, useState } from "react";
import {
  DEFAULT_AUTHOR,
  type AuthorError,
  type AuthorResult,
} from "@/lib/author";

type Status = "idle" | "searching" | "done" | "error";

const SEARCH_SEEDS = [
  "King Jethro Jerry",
  "Jethro Jerry developer",
  "Jethro Jerry Android",
];

export function AuthorBio() {
  const [status, setStatus] = useState<Status>("idle");
  const [result, setResult] = useState<AuthorResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const run = useCallback(async (query: string) => {
    setStatus("searching");
    setError(null);
    try {
      const res = await fetch("/api/author", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ query }),
      });
      const data = (await res.json()) as AuthorResult | AuthorError;
      if (!res.ok || "error" in data) {
        throw new Error("error" in data ? data.error : "lookup failed");
      }
      setResult(data);
      setStatus("done");
    } catch (err) {
      setError(err instanceof Error ? err.message : "lookup failed");
      setStatus("error");
    }
  }, []);

  return (
    <div className="border border-[#141414] bg-[#050505]">
      <div className="border-b border-[#1E1E1E] bg-[#0A0A0A] px-6 py-4">
        <p className="font-mono text-[10px] tracking-[0.3em] text-[#7A7A7A]">
          RESEARCH ENGINE {"//"} WEB SEARCH {"\u00D7"} GROQ
        </p>
      </div>

      <div className="p-6">
        {status === "idle" ? (
          <div className="space-y-4">
            <p className="font-mono text-sm leading-relaxed text-[#9A9A9A]">
              This page is wired to the real internet. Hit search and the
              terminal below runs a live web search for the author, then has a
              language model write the bio from what it finds — same stack as
              the launcher&apos;s own git bridge, minus the APK.
            </p>
            <div className="flex flex-wrap gap-3">
              {SEARCH_SEEDS.map((seed) => (
                <button
                  key={seed}
                  type="button"
                  onClick={() => void run(seed)}
                  className="border border-[#1E1E1E] px-4 py-2 font-mono text-[11px] tracking-[0.15em] text-[#7A7A7A] transition-colors hover:border-[#00FF9C]/50 hover:text-[#00FF9C]"
                >
                  $ search &quot;{seed}&quot;
                </button>
              ))}
            </div>
          </div>
        ) : null}

        {status === "searching" ? (
          <div className="font-mono text-sm text-[#00FF9C]">
            <p>
              <span className="text-[#00FF9C]">root@dark:~# </span>
              searching the web for &quot;{result?.name ?? DEFAULT_AUTHOR}&quot;...
            </p>
            <p className="mt-1 text-[#9EFFC7]">querying 6 sources... ok</p>
            <p className="mt-1 text-[#9EFFC7]">
              running llm bio synthesis...<span className="blink">_</span>
            </p>
          </div>
        ) : null}

        {status === "done" && result ? (
          <div className="space-y-6">
            <div>
              <p className="font-mono text-[10px] tracking-[0.3em] text-[#4A4A4A]">
                {"//"} HEADLINE
              </p>
              <h3 className="mt-2 font-mono text-xl font-bold tracking-[0.1em] text-[#00FF9C]">
                {result.headline}
              </h3>
            </div>

            <div>
              <p className="font-mono text-[10px] tracking-[0.3em] text-[#4A4A4A]">
                {"//"} SUMMARY
              </p>
              <p className="mt-2 font-mono text-sm leading-relaxed text-[#9A9A9A]">
                {result.summary}
              </p>
            </div>

            {result.facts.length > 0 ? (
              <div>
                <p className="font-mono text-[10px] tracking-[0.3em] text-[#4A4A4A]">
                  {"//"} FACTS
                </p>
                <ul className="mt-2 space-y-2">
                  {result.facts.map((fact) => (
                    <li
                      key={fact}
                      className="flex gap-2 font-mono text-xs leading-relaxed text-[#E8E8E8]/80"
                    >
                      <span className="text-[#00FF9C]">{"\u25B8"}</span>
                      <span>{fact}</span>
                    </li>
                  ))}
                </ul>
              </div>
            ) : null}

            {result.sources.length > 0 ? (
              <div>
                <p className="font-mono text-[10px] tracking-[0.3em] text-[#4A4A4A]">
                  {"//"} SOURCES
                </p>
                <ul className="mt-2 space-y-1">
                  {result.sources.map((src) => (
                    <li key={src.url}>
                      <a
                        href={src.url}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="font-mono text-xs text-[#7A7A7A] underline transition-colors hover:text-[#00FF9C]"
                      >
                        {src.title}
                      </a>
                    </li>
                  ))}
                </ul>
              </div>
            ) : (
              <p className="font-mono text-[11px] text-[#FFB300]">
                {"\u26A0"} no public sources matched — the bio is best-effort
                from the model&apos;s general knowledge.
              </p>
            )}

            <button
              type="button"
              onClick={() => void run(result.name)}
              className="border border-[#00FF9C]/50 px-4 py-2 font-mono text-[11px] tracking-[0.2em] text-[#00FF9C] transition-colors hover:bg-[#00FF9C]/10"
            >
              SEARCH AGAIN
            </button>
          </div>
        ) : null}

        {status === "error" ? (
          <div className="space-y-4">
            <p className="font-mono text-sm text-[#FF6B6B]">
              {"\u2715"} {error}
            </p>
            <button
              type="button"
              onClick={() => void run(DEFAULT_AUTHOR)}
              className="border border-[#FF6B6B]/50 px-4 py-2 font-mono text-[11px] tracking-[0.2em] text-[#FF6B6B] transition-colors hover:bg-[#FF6B6B]/10"
            >
              RETRY
            </button>
          </div>
        ) : null}
      </div>
    </div>
  );
}
