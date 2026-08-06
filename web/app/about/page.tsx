import type { Metadata } from "next";
import { Container } from "@/components/ui/Container";
import { PageHeader } from "@/components/ui/PageHeader";
import { TerminalPanel } from "@/components/ui/TerminalPanel";
import { StarRepo } from "@/components/sections/StarRepo";
import { AuthorBio } from "@/components/author/AuthorBio";

export const metadata: Metadata = {
  title: "About — D.A.R.K.",
  description:
    "The person behind D.A.R.K.: King Jethro Jerry. Live author bio powered by web search and Groq.",
};

const STACK = [
  { k: "ANDROID", v: "Kotlin · Jetpack Compose" },
  { k: "ARCH", v: "MVVM + StateFlow · UDF" },
  { k: "DATA", v: "Room · DataStore · Health Connect" },
  { k: "SYSTEM", v: "LauncherApps · AIDL · ShellStream" },
  { k: "WEB", v: "Next.js · Tailwind · Redux" },
  { k: "BUILD", v: "R8 · Gradle · version.properties" },
] as const;

export default function AboutPage() {
  return (
    <main className="mt-6">
      <PageHeader
        index="ABOUT"
        title="BEHIND THE KERNEL"
        subtitle="D.A.R.K. is a one-person project. No company, no team, no funding — just a developer who wanted a home screen that gets out of the way."
      />

      <Container className="mt-12 grid gap-10 lg:grid-cols-2 lg:items-start">
        <TerminalPanel title="dark@author:~# whoami">
          <p className="font-mono text-xs tracking-[0.3em] text-[#00FF9C]">
            {"$"} whoami
          </p>
          <p className="mt-2 font-mono text-2xl font-bold tracking-[0.1em] text-[#E8E8E8]">
            KING JETHRO JERRY
          </p>
          <p className="mt-4 font-mono text-xs leading-relaxed text-[#7A7A7A]">
            Android system engineer and UI developer. Believes interfaces should
            be fast first and pretty second, prefers text over icons, and
            refuses to ship ads in a launcher. D.A.R.K. is the product of that
            opinion.
          </p>
          <p className="mt-4 font-mono text-xs leading-relaxed text-[#7A7A7A]">
            This site, the launcher, and the release APK are all maintained by
            one person — which is exactly why everything on here is honest:
            real build history, real commands, real APK.
          </p>
          <p className="mt-4 font-mono text-xs leading-relaxed text-[#7A7A7A]">
            Everything is open source:{" "}
            <a
              href="https://github.com/kingjethro999/D.A.R.K"
              target="_blank"
              rel="noopener noreferrer"
              className="text-[#00FF9C] underline"
            >
              github.com/kingjethro999/D.A.R.K
            </a>
          </p>
        </TerminalPanel>

        <div>
          <p className="font-mono text-xs tracking-[0.25em] text-[#7A7A7A]">
            THE STACK
          </p>
          <dl className="mt-4 grid grid-cols-1 gap-px overflow-hidden border border-[#111111] bg-[#111111] sm:grid-cols-2">
            {STACK.map((item) => (
              <div key={item.k} className="bg-[#050505] p-4">
                <dt className="font-mono text-[10px] tracking-[0.2em] text-[#4A4A4A]">
                  {item.k}
                </dt>
                <dd className="mt-1 font-mono text-xs font-bold text-[#E8E8E8]">
                  {item.v}
                </dd>
              </div>
            ))}
          </dl>
        </div>
      </Container>

      <Container className="mt-10">
        <StarRepo />
      </Container>

      <Container className="mt-16">
        <p className="font-mono text-xs tracking-[0.3em] text-[#00FF9C]">
          RESEARCH ENGINE {"//"} ABOUT THE AUTHOR
        </p>
        <h2 className="mt-3 font-mono text-2xl font-bold tracking-[0.15em] text-[#E8E8E8]">
          SEARCH ME ON THE LIVE WEB.
        </h2>
        <p className="mt-3 max-w-2xl font-mono text-sm leading-relaxed text-[#7A7A7A]">
          The panel below runs a real web search through this server and asks a
          Groq model to write the bio from what it finds. Same
          developer-tooling spirit as the launcher — just for people instead of
          packages.
        </p>
        <div className="mt-8">
          <AuthorBio />
        </div>
      </Container>
    </main>
  );
}
