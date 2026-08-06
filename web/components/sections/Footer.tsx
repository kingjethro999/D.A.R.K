import { Container } from "@/components/ui/Container";
import { APP } from "@/lib/app";

export function Footer() {
  return (
    <footer className="border-t border-[#111111] py-10">
      <Container className="flex flex-col items-center justify-between gap-4 sm:flex-row">
        <p className="font-mono text-xs tracking-[0.3em] text-[#7A7A7A]">
          D.A.R.K. v{APP.version} {"//"} {APP.fullName}
        </p>
        <p className="font-mono text-xs tracking-[0.2em] text-[#7A7A7A]">
          OLED BLACK {"\u00B7"} TEXT ONLY {"\u00B7"} NO ADS
        </p>
      </Container>
    </footer>
  );
}
