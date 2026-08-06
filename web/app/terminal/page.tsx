import type { Metadata } from "next";
import Link from "next/link";
import { Container } from "@/components/ui/Container";
import { PageHeader } from "@/components/ui/PageHeader";
import { TerminalDemo } from "@/components/terminal/TerminalDemo";

export const metadata: Metadata = {
  title: "Terminal — D.A.R.K.",
  description:
    "Try the D.A.R.K. command line in your browser. Fuzzy app launching, flashlight toggles, git stats, vault, logcat and git-style typo suggestions.",
};

const HIGHLIGHTS = [
  {
    cmd: "source whatsapp dual",
    desc: "Fuzzy-launches an app — even the second (dual) instance.",
  },
  { cmd: "nox on", desc: "Flashlight via CameraManager. Zero permissions." },
  { cmd: "git refresh", desc: "One GraphQL query for repos, stars, commits." },
  { cmd: "log sprint 100m 11.2s", desc: "Workouts logged straight to Room." },
  { cmd: "vault lock", desc: "AES-encrypts the focus vault with your PIN." },
  { cmd: "logcat -t 20", desc: "Stream the last lines of the system log." },
  { cmd: "srce whatsapp", desc: "Typos get git-style suggestions, not errors." },
  { cmd: "my name is king jethro", desc: "Natural language is understood." },
];

export default function TerminalPage() {
  return (
    <main className="mt-6">
      <PageHeader
        index="TERMINAL"
        title="TRY THE COMMAND LINE"
        subtitle="This is a faithful simulation of the terminal that lives on your home screen. Type help, autocomplete with tab, and browse history with the arrow keys."
      />

      <Container className="mt-12">
        <TerminalDemo />
      </Container>

      <Container className="mt-20">
        <p className="font-mono text-xs tracking-[0.3em] text-[#00FF9C]">
          REAL INPUT {"//"} REAL COMMANDS
        </p>
        <h2 className="mt-3 font-mono text-2xl font-bold tracking-[0.15em] text-[#E8E8E8]">
          WHAT YOU JUST TYPED IS WHAT THE PHONE RUNS.
        </h2>
        <div className="mt-10 grid gap-px overflow-hidden border border-[#111111] bg-[#111111] sm:grid-cols-2 lg:grid-cols-4">
          {HIGHLIGHTS.map((item) => (
            <article key={item.cmd} className="bg-[#050505] p-5">
              <p className="font-mono text-sm font-bold text-[#00FF9C]">
                $ {item.cmd}
              </p>
              <p className="mt-2 font-mono text-xs leading-relaxed text-[#7A7A7A]">
                {item.desc}
              </p>
            </article>
          ))}
        </div>

        <div className="mt-12 flex flex-col items-start gap-3 border border-[#141414] bg-[#050505] p-6 sm:flex-row sm:items-center sm:justify-between">
          <p className="font-mono text-xs leading-relaxed text-[#7A7A7A]">
            The full reference — every command, argument, and edge case —{" "}
            <Link href="/docs/terminal" className="text-[#00FF9C] underline">
              lives in the docs.
            </Link>
          </p>
          <Link
            href="/docs"
            className="border border-[#00FF9C]/50 px-5 py-2.5 font-mono text-[11px] tracking-[0.25em] text-[#00FF9C] transition-colors hover:bg-[#00FF9C]/10"
          >
            OPEN DOCS
          </Link>
        </div>
      </Container>
    </main>
  );
}
