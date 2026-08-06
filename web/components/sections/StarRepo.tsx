"use client";

import { useEffect, useState } from "react";
import { APP } from "@/lib/app";
import { cn } from "@/lib/utils";
import { Container } from "@/components/ui/Container";

const API_URL = "https://api.github.com/repos/kingjethro999/D.A.R.K";

interface RepoStats {
  stars: number | null;
  forks: number | null;
}

function format(n: number | null): string {
  if (n === null) return "--";
  if (n >= 1000) return `${(n / 1000).toFixed(1)}k`;
  return String(n);
}

export function StarRepo({ compact = false }: { compact?: boolean }) {
  const [stats, setStats] = useState<RepoStats>({ stars: null, forks: null });

  useEffect(() => {
    let alive = true;
    fetch(API_URL)
      .then((r) => (r.ok ? r.json() : null))
      .then((data) => {
        if (alive && data) {
          setStats({
            stars: data.stargazers_count ?? null,
            forks: data.forks_count ?? null,
          });
        }
      })
      .catch(() => {
        /* rate-limited or offline — keep "--" */
      });
    return () => {
      alive = false;
    };
  }, []);

  return (
    <section
      className={cn(
        "border-t border-[#111111]",
        compact ? "py-6" : "py-12 md:py-16"
      )}
    >
      <Container>
        <div
          className={cn(
            "flex flex-col gap-5 border border-[#1E1E1E] bg-[#040404] md:flex-row md:items-center",
            compact ? "p-4" : "p-5 sm:p-6"
          )}
        >
          <div className="flex items-center gap-4">
            <span className="text-xl text-[#FFB300]">{"\u2605"}</span>
            <div>
              <p className="font-mono text-lg font-bold leading-none text-[#E8E8E8]">
                {format(stats.stars)}
              </p>
              <p className="mt-1 font-mono text-[9px] tracking-[0.2em] text-[#4A4A4A]">
                STARS
              </p>
            </div>
            <div className="ml-2 border-l border-[#1E1E1E] pl-4">
              <p className="font-mono text-lg font-bold leading-none text-[#E8E8E8]">
                {format(stats.forks)}
              </p>
              <p className="mt-1 font-mono text-[9px] tracking-[0.2em] text-[#4A4A4A]">
                FORKS
              </p>
            </div>
          </div>
          <a
            href={APP.repository}
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center justify-center gap-2 border border-[#FFB300]/60 px-5 py-3 font-mono text-[11px] font-bold tracking-[0.25em] text-[#FFB300] transition-colors hover:bg-[#FFB300]/10 md:ml-auto"
          >
            <span>{"\u2605"}</span>
            STAR ON GITHUB
          </a>
        </div>
      </Container>
    </section>
  );
}
