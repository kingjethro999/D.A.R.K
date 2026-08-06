"use client";

import { useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";

export const NAV_LINKS = [
  { label: "HOME", href: "/" },
  { label: "JOURNEY", href: "/journey" },
  { label: "TERMINAL", href: "/terminal" },
  { label: "DOCS", href: "/docs" },
  { label: "ABOUT", href: "/about" },
] as const;

export function Navbar() {
  const [open, setOpen] = useState(false);
  const pathname = usePathname();

  const isActive = (href: string) =>
    href === "/" ? pathname === "/" : pathname.startsWith(href);

  return (
    <header className="sticky top-4 z-50">
      <nav className="mx-auto flex w-full max-w-4xl items-center justify-between gap-4 rounded-full border border-[#1E1E1E] bg-[#050505]/80 px-5 py-2.5 backdrop-blur-md">
        <Link
          href="/"
          className="flex items-center gap-2 font-mono text-sm font-bold tracking-[0.3em] text-[#00FF9C]"
        >
          <span className="inline-block h-2 w-2 rounded-full bg-[#00FF9C]" />
          D.A.R.K.
        </Link>

        <div className="hidden items-center gap-1 md:flex">
          {NAV_LINKS.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className={cn(
                "rounded-full px-3 py-1.5 font-mono text-[11px] tracking-[0.2em] transition-colors",
                isActive(link.href)
                  ? "bg-[#00FF9C]/10 text-[#00FF9C]"
                  : "text-[#7A7A7A] hover:text-[#E8E8E8]"
              )}
            >
              {link.label}
            </Link>
          ))}
          <Link
            href="/download"
            className="ml-2 rounded-full bg-[#00FF9C] px-4 py-1.5 font-mono text-[11px] font-bold tracking-[0.2em] text-black transition-opacity hover:opacity-80"
          >
            INSTALL
          </Link>
        </div>

        <button
          type="button"
          aria-label="Toggle menu"
          aria-expanded={open}
          onClick={() => setOpen((v) => !v)}
          className="flex h-9 w-9 flex-col items-center justify-center gap-1.5 rounded-full border border-[#1E1E1E] text-[#E8E8E8] md:hidden"
        >
          <span
            className={cn(
              "h-0.5 w-4 bg-current transition-transform",
              open && "translate-y-[6px] rotate-45"
            )}
          />
          <span
            className={cn(
              "h-0.5 w-4 bg-current transition-opacity",
              open && "opacity-0"
            )}
          />
          <span
            className={cn(
              "h-0.5 w-4 bg-current transition-transform",
              open && "-translate-y-[6px] -rotate-45"
            )}
          />
        </button>
      </nav>

      <div
        className={cn(
          "mx-auto mt-2 w-full max-w-4xl overflow-hidden transition-all duration-300 md:hidden",
          open
            ? "max-h-96 rounded-2xl border border-[#1E1E1E] bg-[#050505]/95 backdrop-blur-md"
            : "max-h-0"
        )}
      >
        <div className="flex flex-col gap-1 p-3">
          {NAV_LINKS.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              onClick={() => setOpen(false)}
              className={cn(
                "rounded-xl px-4 py-2.5 font-mono text-xs tracking-[0.2em] transition-colors",
                isActive(link.href)
                  ? "bg-[#00FF9C]/10 text-[#00FF9C]"
                  : "text-[#7A7A7A] hover:text-[#E8E8E8]"
              )}
            >
              {link.label}
            </Link>
          ))}
          <Link
            href="/download"
            onClick={() => setOpen(false)}
            className="mt-1 rounded-xl bg-[#00FF9C] px-4 py-2.5 text-center font-mono text-xs font-bold tracking-[0.2em] text-black"
          >
            INSTALL
          </Link>
        </div>
      </div>
    </header>
  );
}
