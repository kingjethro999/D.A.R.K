import type { Metadata } from "next";
import { Container } from "@/components/ui/Container";
import { PageHeader } from "@/components/ui/PageHeader";
import { JOURNEY, CURRENT_RELEASE, CURRENT_BUILD } from "@/lib/journey";
import { APP } from "@/lib/app";

export const metadata: Metadata = {
  title: "Journey — D.A.R.K.",
  description:
    "The real build history of D.A.R.K., from a rant against bloated launchers to a 1.7 MB signed release.",
};

export default function JourneyPage() {
  return (
    <main className="mt-6">
      <PageHeader
        index="JOURNEY"
        title="HOW D.A.R.K. GOT BUILT"
        subtitle="Reconstructed from the actual build log. No invented roadmap — every chapter below maps to code that shipped. Current release:"
      />

      <Container className="mt-10">
        <div className="flex flex-wrap gap-3 font-mono">
          <span className="border border-[#00FF9C]/40 bg-[#00FF9C]/5 px-4 py-2 text-xs tracking-[0.2em] text-[#00FF9C]">
            v{CURRENT_RELEASE} (build {CURRENT_BUILD}) — current
          </span>
          <span className="border border-[#141414] px-4 py-2 text-xs tracking-[0.2em] text-[#7A7A7A]">
            1.7 MB signed release
          </span>
          <span className="border border-[#141414] px-4 py-2 text-xs tracking-[0.2em] text-[#7A7A7A]">
            {APP.minAndroid}
          </span>
        </div>
      </Container>

      <div className="relative mx-auto mt-16 w-full max-w-4xl px-6">
        <div
          aria-hidden
          className="absolute left-[19px] top-2 bottom-2 w-px bg-gradient-to-b from-[#00FF9C]/40 via-[#1E1E1E] to-transparent md:left-1/2"
        />
        <ol className="space-y-14">
          {JOURNEY.map((chapter, index) => (
            <li
              key={chapter.version}
              className="relative grid gap-4 pl-14 md:grid-cols-2 md:gap-12 md:pl-0"
            >
              <span className="absolute left-0 top-1 flex h-10 w-10 items-center justify-center rounded-full border border-[#00FF9C]/40 bg-black font-mono text-[10px] font-bold text-[#00FF9C] md:left-1/2 md:-translate-x-1/2">
                {String(index + 1).padStart(2, "0")}
              </span>

              <div
                className={
                  index % 2 === 0
                    ? "md:col-start-1 md:pr-16 md:text-right"
                    : "md:col-start-2 md:pl-16"
                }
              >
                <div className="inline-block">
                  <p className="font-mono text-[10px] tracking-[0.3em] text-[#4A4A4A]">
                    {chapter.date}
                  </p>
                  <h2 className="mt-1 font-mono text-xl font-bold tracking-[0.15em] text-[#E8E8E8]">
                    {chapter.version}{" "}
                    <span className="text-[#00FF9C]">
                      {"// "}
                      {chapter.codename}
                    </span>
                  </h2>
                </div>
              </div>

              <div
                className={
                  index % 2 === 0
                    ? "md:col-start-2"
                    : "md:col-start-1 md:row-start-1 md:text-right"
                }
              >
                <div className="border border-[#141414] bg-[#050505] p-6 transition-colors hover:border-[#00FF9C]/30">
                  <p className="font-mono text-xs leading-relaxed text-[#E8E8E8]/80">
                    {chapter.blurb}
                  </p>
                  <ul className="mt-4 space-y-2">
                    {chapter.points.map((point) => (
                      <li
                        key={point}
                        className="flex gap-2 font-mono text-xs leading-relaxed text-[#7A7A7A]"
                      >
                        <span className="text-[#00FF9C]">{"\u25B8"}</span>
                        <span>{point}</span>
                      </li>
                    ))}
                  </ul>
                  {chapter.highlight ? (
                    <p className="mt-4 border-l-2 border-[#00FF9C]/50 pl-3 font-mono text-[11px] italic text-[#00FF9C]">
                      {chapter.highlight}
                    </p>
                  ) : null}
                </div>
              </div>
            </li>
          ))}
        </ol>
      </div>

      <Container className="mt-20">
        <div className="border border-[#141414] bg-[#050505] p-6">
          <p className="font-mono text-xs tracking-[0.3em] text-[#00FF9C]">
            {"//"} ON VERSIONING
          </p>
          <p className="mt-3 max-w-3xl font-mono text-xs leading-relaxed text-[#7A7A7A]">
            D.A.R.K. version.properties auto-increments the patch number and
            versionCode on every build, so the timeline above is grouped by
            feature chapter rather than by individual increment. v1.0.7 is
            the release shipped on this site. The full transcript of the build
            lives in the repository&apos;s chat log.
          </p>
        </div>
      </Container>
    </main>
  );
}
