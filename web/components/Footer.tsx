import Link from "next/link";
import { Container } from "@/components/ui/Container";
import { APP } from "@/lib/app";

const FOOTER_LINKS = [
  { label: "JOURNEY", href: "/journey" },
  { label: "TERMINAL", href: "/terminal" },
  { label: "DOCS", href: "/docs" },
  { label: "DOWNLOAD", href: "/download" },
  { label: "ABOUT", href: "/about" },
] as const;

export function Footer() {
  return (
    <footer className="mt-24 border-t border-[#111111] py-12">
      <Container className="flex flex-col items-center gap-8">
        <nav className="flex flex-wrap items-center justify-center gap-x-8 gap-y-3">
          {FOOTER_LINKS.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className="font-mono text-[10px] tracking-[0.25em] text-[#7A7A7A] transition-colors hover:text-[#00FF9C]"
            >
              {link.label}
            </Link>
          ))}
        </nav>
        <div className="flex flex-col items-center gap-2 text-center">
          <p className="font-mono text-xs tracking-[0.3em] text-[#7A7A7A]">
            D.A.R.K. v{APP.version} {"//"} {APP.fullName}
          </p>
          <p className="font-mono text-[10px] tracking-[0.2em] text-[#4A4A4A]">
            OLED BLACK {"\u00B7"} TEXT ONLY {"\u00B7"} NO ADS {"\u00B7"} BUILT
            WITH KOTLIN {"\u00B7"} COMPOSE
          </p>
          <p className="font-mono text-[10px] tracking-[0.2em] text-[#2A2A2A]">
            {"\u00A9"} 2026 KING JETHRO JERRY
          </p>
          <a
            href={APP.repository}
            target="_blank"
            rel="noopener noreferrer"
            className="font-mono text-[10px] tracking-[0.2em] text-[#4A4A4A] transition-colors hover:text-[#00FF9C]"
          >
            {"\u25B8"} github.com/kingjethro999/D.A.R.K
          </a>
        </div>
      </Container>
    </footer>
  );
}
