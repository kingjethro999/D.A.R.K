import Link from "next/link";
import { Container } from "@/components/ui/Container";

const NAV_LINKS = [
  { label: "FEATURES", href: "#features" },
  { label: "SCREENSHOTS", href: "#screenshots" },
  { label: "COMMANDS", href: "#commands" },
  { label: "INSTALL", href: "#download" },
] as const;

export function Navbar() {
  return (
    <header className="sticky top-0 z-40 border-b border-[#111111] bg-black/90 backdrop-blur">
      <Container className="flex h-16 items-center justify-between">
        <a
          href="#top"
          className="font-mono text-sm font-bold tracking-[0.3em] text-[#00FF9C]"
        >
          D.A.R.K.
        </a>
        <nav className="flex items-center gap-6">
          {NAV_LINKS.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className="hidden font-mono text-[11px] tracking-[0.2em] text-[#7A7A7A] transition-colors hover:text-[#E8E8E8] sm:block"
            >
              {link.label}
            </Link>
          ))}
          <Link
            href="#download"
            className="border border-[#00FF9C]/50 px-4 py-2 font-mono text-[11px] tracking-[0.2em] text-[#00FF9C] transition-colors hover:bg-[#00FF9C]/10"
          >
            INSTALL
          </Link>
        </nav>
      </Container>
    </header>
  );
}
