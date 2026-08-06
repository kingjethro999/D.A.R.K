import { NextResponse } from "next/server";

export const runtime = "nodejs";

const GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
const TAVILY_URL = "https://api.tavily.com/search";

const GROQ_MODELS = ["llama-3.3-70b-versatile", "llama-3.1-8b-instant"];

interface TavilyResult {
  title?: string;
  url?: string;
  content?: string;
}

interface AuthorPayload {
  name: string;
  headline: string;
  summary: string;
  facts: string[];
  sources: { title: string; url: string }[];
  truncated: boolean;
}

async function searchWeb(query: string): Promise<TavilyResult[]> {
  const key = process.env.TAVILY_API_KEY;
  if (!key) throw new Error("Tavily key not configured");
  const res = await fetch(TAVILY_URL, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      api_key: key,
      query,
      max_results: 6,
      include_answer: false,
    }),
  });
  if (!res.ok) throw new Error(`Tavily HTTP ${res.status}`);
  const data = await res.json();
  return Array.isArray(data.results) ? data.results : [];
}

function extractJson(raw: string): unknown {
  const cleaned = raw.replace(/```json/gi, "").replace(/```/g, "").trim();
  const tryParse = (s: string): unknown => {
    try {
      return JSON.parse(s);
    } catch {
      return undefined;
    }
  };
  const direct = tryParse(cleaned);
  if (direct !== undefined) return direct;
  const start = cleaned.indexOf("{");
  const end = cleaned.lastIndexOf("}");
  if (start >= 0 && end > start) {
    const sliced = tryParse(cleaned.slice(start, end + 1));
    if (sliced !== undefined) return sliced;
  }
  const repaired = tryParse(cleaned.replace(/,\s*([}\]])/g, "$1"));
  if (repaired !== undefined) return repaired;
  if (start >= 0 && end > start) {
    const repairedSlice = tryParse(
      cleaned.slice(start, end + 1).replace(/,\s*([}\]])/g, "$1")
    );
    if (repairedSlice !== undefined) return repairedSlice;
  }
  return null;
}

function asFacts(value: unknown, fallback: string[]): string[] {
  if (Array.isArray(value)) {
    return value.filter((f): f is string => typeof f === "string").slice(0, 4);
  }
  if (typeof value === "string") {
    const bullets = value
      .split(/[\n•]+/)
      .map((s) => s.replace(/^-\s*/, "").trim())
      .filter((s) => s.length > 3);
    if (bullets.length >= 1) return bullets.slice(0, 4);
  }
  return fallback;
}

async function askGroq(
  messages: { role: string; content: string }[]
): Promise<string> {  const key = process.env.GROQ_API_KEY;
  if (!key) throw new Error("Groq key not configured");

  for (const model of GROQ_MODELS) {
    try {
      const res = await fetch(GROQ_URL, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${key}`,
        },
        body: JSON.stringify({
          model,
          messages,
          temperature: 0.4,
          max_tokens: 700,
        }),
      });
      if (!res.ok) continue;
      const data = await res.json();
      const text = data?.choices?.[0]?.message?.content;
      if (typeof text === "string" && text.trim()) return text.trim();
    } catch {
      // fall through to next model
    }
  }
  throw new Error("Groq unavailable");
}

export async function POST(req: Request) {
  let query = "King Jethro Jerry";
  try {
    const body = await req.json();
    if (typeof body?.query === "string" && body.query.trim()) {
      query = body.query.trim().slice(0, 80);
    }
  } catch {
    // default query
  }

  try {
    const results = await searchWeb(`${query} developer`);
    const raw = await askGroq([
      {
        role: "system",
        content:
          "You are a research assistant writing a short bio. Respond ONLY with strict JSON matching this schema: {\"headline\": string, \"summary\": string (2-3 sentences), \"facts\": string[] (exactly 4 concise factual bullets)}. Base everything on the provided web excerpts. If the excerpts give no real facts about the person, say so honestly in the summary instead of inventing details.",
      },
      {
        role: "user",
        content: `Subject: ${query}\n\nWeb excerpts:\n${results
          .map((r, i) => `[${i + 1}] ${r.title ?? "untitled"}\n${(r.content ?? "").slice(0, 400)}`)
          .join("\n\n")}`,
      },
    ]);

    const extracted = extractJson(raw);
    const parsed: Partial<AuthorPayload> =
      extracted !== null && typeof extracted === "object"
        ? (extracted as Partial<AuthorPayload>)
        : {};
    const summary =
      parsed.summary && typeof parsed.summary === "string" && parsed.summary.trim()
        ? parsed.summary.trim()
        : "No reliable public write-up was found in the sources searched. The details below are best-effort from the web.";
    const headline =
      parsed.headline && typeof parsed.headline === "string" && parsed.headline.trim()
        ? parsed.headline.trim()
        : "Developer, tinkerer, builder of dumb-fast software.";

    const sources = results
      .filter((r) => r.title && r.url)
      .slice(0, 4)
      .map((r) => ({ title: r.title as string, url: r.url as string }));

    const payload: AuthorPayload = {
      name: query,
      headline,
      summary,
      facts: asFacts(parsed.facts, [
        "Built D.A.R.K., a minimalist text-only Android launcher.",
      ]),
      sources,
      truncated: results.length === 0,
    };

    return NextResponse.json(payload);
  } catch (err) {
    return NextResponse.json(
      {
        error: err instanceof Error ? err.message : "author lookup failed",
      },
      { status: 502 }
    );
  }
}
