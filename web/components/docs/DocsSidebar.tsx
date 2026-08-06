"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { DOC_GROUPS, DOCS } from "@/lib/docs";
import { cn } from "@/lib/utils";

export function DocsSidebar() {
  const [query, setQuery] = useState("");
  const pathname = usePathname();

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return DOCS;
    return DOCS.filter((doc) =>
      [doc.title, doc.nav, doc.description, doc.group]
        .join(" ")
        .toLowerCase()
        .includes(q)
    );
  }, [query]);

  return (
    <aside className="lg:sticky lg:top-24 lg:self-start">
      <div className="border border-[#1E1E1E] bg-[#040404] p-4">
        <p className="font-mono text-[10px] tracking-[0.3em] text-[#7A7A7A]">
          SEARCH DOCS
        </p>
        <input
          type="search"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="filter..."
          autoComplete="off"
          className="mt-2 w-full border border-[#1E1E1E] bg-black px-3 py-2 font-mono text-xs text-[#E8E8E8] outline-none placeholder:text-[#4A4A4A] focus:border-[#00FF9C]/50"
          aria-label="Search documentation"
        />
      </div>

      <nav className="mt-4 space-y-6">
        {DOC_GROUPS.map((group) => {
          const docs = filtered.filter((d) => d.group === group);
          if (docs.length === 0 && query) return null;
          return (
            <div key={group}>
              <p className="mb-2 font-mono text-[10px] tracking-[0.3em] text-[#4A4A4A]">
                {group}
              </p>
              <ul className="space-y-1">
                {docs.map((doc) => {
                  const active =
                    pathname === `/docs/${doc.slug}` ||
                    (pathname === "/docs" && doc.slug === "installation");
                  return (
                    <li key={doc.slug}>
                      <Link
                        href={`/docs/${doc.slug}`}
                        className={cn(
                          "block border-l-2 px-3 py-1.5 font-mono text-xs transition-colors",
                          active
                            ? "border-[#00FF9C] bg-[#00FF9C]/5 text-[#00FF9C]"
                            : "border-transparent text-[#7A7A7A] hover:border-[#333333] hover:text-[#E8E8E8]"
                        )}
                      >
                        {doc.nav}
                      </Link>
                    </li>
                  );
                })}
              </ul>
            </div>
          );
        })}
        {filtered.length === 0 ? (
          <p className="font-mono text-xs text-[#7A7A7A]">
            no docs match &quot;{query}&quot;
          </p>
        ) : null}
      </nav>
    </aside>
  );
}
