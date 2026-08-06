"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { completionsFor, runDemoCommand } from "@/lib/terminalDemo";
import { cn } from "@/lib/utils";

interface Entry {
  id: number;
  kind: "cmd" | "out" | "sys";
  text: string;
}

const BOOT_LINES = [
  "D.A.R.K. KERNEL v1.0.7 — Developers' Adaptive Responsive Kernel",
  "decompressing launcher... ok",
  "mounting app list.... ok",
  "starting terminal service... ok",
  "loading sensors, vault, git bridge... ok",
  "ready. type 'help' to begin.",
  "",
];

let idCounter = 0;
const nextId = () => ++idCounter;

export function TerminalDemo() {
  const [entries, setEntries] = useState<Entry[]>([]);
  const [input, setInput] = useState("");
  const [busy, setBusy] = useState(true);
  const [suggestion, setSuggestion] = useState<string | null>(null);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [cursorVisible, setCursorVisible] = useState(true);
  const inputRef = useRef<HTMLInputElement>(null);
  const scrollRef = useRef<HTMLDivElement>(null);
  const historyRef = useRef<string[]>([]);
  const historyIndexRef = useRef(-1);

  useEffect(() => {
    let i = 0;
    const timer = setInterval(() => {
      if (i < BOOT_LINES.length) {
        setEntries((prev) => [...prev, { id: nextId(), kind: "sys", text: BOOT_LINES[i] }]);
        i += 1;
      } else {
        clearInterval(timer);
        setBusy(false);
      }
    }, 120);
    return () => clearInterval(timer);
  }, []);

  useEffect(() => {
    const el = scrollRef.current;
    if (el) el.scrollTop = el.scrollHeight;
  }, [entries]);

  useEffect(() => {
    const blink = setInterval(() => setCursorVisible((v) => !v), 500);
    return () => clearInterval(blink);
  }, []);

  useEffect(() => {
    if (!busy) inputRef.current?.focus();
  }, [busy]);

  const streamOut = useCallback((lines: string[], baseId: number) => {
    lines.forEach((line, idx) => {
      setTimeout(() => {
        setEntries((prev) => [
          ...prev,
          { id: baseId + idx, kind: "out", text: line },
        ]);
      }, idx * 55);
    });
    return baseId + lines.length;
  }, []);

  const submit = useCallback(
    (raw: string) => {
      const text = raw.trim();
      if (!text) return;
      setEntries((prev) => [...prev, { id: nextId(), kind: "cmd", text }]);
      historyRef.current.unshift(text);
      historyIndexRef.current = -1;
      setInput("");
      setSuggestion(null);
      setShowSuggestions(false);

      const resp = runDemoCommand(text);
      if (resp.clear) {
        setEntries([]);
        return;
      }
      const baseId = nextId();
      streamOut(resp.lines, baseId);
    },
    [streamOut]
  );

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      submit(input);
      return;
    }
    if (e.key === "Tab") {
      e.preventDefault();
      if (suggestion) {
        setInput(suggestion);
        setSuggestion(null);
        setShowSuggestions(false);
      }
      return;
    }
    if (e.key === "ArrowUp") {
      e.preventDefault();
      const hist = historyRef.current;
      if (!hist.length) return;
      const next = Math.min(historyIndexRef.current + 1, hist.length - 1);
      historyIndexRef.current = next;
      setInput(hist[next]);
      setShowSuggestions(false);
      return;
    }
    if (e.key === "ArrowDown") {
      e.preventDefault();
      const next = historyIndexRef.current - 1;
      historyIndexRef.current = next;
      setInput(next >= 0 ? historyRef.current[next] : "");
      setShowSuggestions(false);
    }
  };

  const handleChange = (value: string) => {
    setInput(value);
    const completions = completionsFor(value);
    if (value.trim().length > 0 && completions.length > 0) {
      const word = value.trim().toLowerCase();
      const exact = completions.find((c) => c === word);
      if (!exact) {
        setSuggestion(completions[0]);
        setShowSuggestions(true);
        return;
      }
    }
    setSuggestion(null);
    setShowSuggestions(false);
  };

  return (
    <div
      onClick={() => inputRef.current?.focus()}
      className="relative overflow-hidden rounded-2xl border border-[#1E1E1E] bg-[#040404]"
    >
      <div className="flex items-center gap-2 border-b border-[#1E1E1E] bg-[#0A0A0A] px-4 py-3">
        <span className="h-2.5 w-2.5 rounded-full bg-[#FF5F57]" />
        <span className="h-2.5 w-2.5 rounded-full bg-[#FEBC2E]" />
        <span className="h-2.5 w-2.5 rounded-full bg-[#28C840]" />
        <span className="ml-3 font-mono text-[11px] tracking-[0.15em] text-[#7A7A7A]">
          dark@home:~# — interactive demo
        </span>
      </div>

      <div
        ref={scrollRef}
        className="h-[460px] overflow-y-auto px-5 py-4 font-mono text-sm leading-relaxed sm:h-[520px]"
      >
        {entries.map((entry) => (
          <div key={entry.id}>
            {entry.kind === "cmd" ? (
              <p>
                <span className="text-[#00FF9C]">root@dark:~# </span>
                <span className="text-[#E8E8E8]">{entry.text}</span>
              </p>
            ) : (
              <p
                className={cn(
                  "whitespace-pre-wrap",
                  entry.kind === "sys"
                    ? "text-[#00FF9C]/60"
                    : "text-[#9EFFC7]"
                )}
              >
                {entry.text}
              </p>
            )}
          </div>
        ))}

        <div className="mt-1 flex items-center">
          <span className="text-[#00FF9C]">root@dark:~# </span>
          <span className="relative inline-block">
            <input
              ref={inputRef}
              value={input}
              onChange={(e) => handleChange(e.target.value)}
              onKeyDown={handleKeyDown}
              onBlur={() => setTimeout(() => inputRef.current?.focus(), 50)}
              autoComplete="off"
              autoCapitalize="off"
              spellCheck={false}
              disabled={busy}
              size={Math.max(input.length, 1)}
              className="w-auto min-w-0 bg-transparent px-0 text-[#E8E8E8] caret-transparent outline-none"
              aria-label="Terminal input"
            />
            <span
              className={cn(
                "absolute right-0 top-0 h-5 w-2.5 bg-[#00FF9C]",
                !cursorVisible && "opacity-0"
              )}
            />
          </span>
        </div>

        {showSuggestions && suggestion ? (
          <p className="mt-1 font-mono text-xs text-[#7A7A7A]">
            tab to autocomplete:{" "}
            <span className="text-[#00FF9C]">{suggestion}</span>
          </p>
        ) : null}
      </div>

      <div className="flex flex-wrap gap-x-5 gap-y-1 border-t border-[#1E1E1E] bg-[#050505] px-5 py-3 font-mono text-[10px] tracking-[0.15em] text-[#4A4A4A]">
        <span>TAB autocomplete</span>
        <span>{"\u2191\u2193"} history</span>
        <span>ENTER run</span>
        <span className="text-[#00FF9C]/70">help</span>
      </div>
    </div>
  );
}
