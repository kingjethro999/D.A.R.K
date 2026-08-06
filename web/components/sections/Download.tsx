"use client";

import { Container } from "@/components/ui/Container";
import { TerminalPanel } from "@/components/ui/TerminalPanel";
import { ErrorState } from "@/components/ErrorState";
import { DownloadButton, DownloadMeta } from "@/components/ui/DownloadButton";
import { useDownloadApk } from "@/hooks/useDownloadApk";
import { APP } from "@/lib/app";

const INSTALL_STEPS = [
  {
    step: "01",
    text: `Tap DOWNLOAD v${APP.version}. Your browser saves ${APP.downloadFileName}.`,
  },
  {
    step: "02",
    text: "Open the file. Android will ask to allow installing from this source.",
  },
  {
    step: "03",
    text: "Choose D.A.R.K. as your home app. Optional: set it as the default launcher.",
  },
  {
    step: "04",
    text: "Double-tap the screen. Your home is now a terminal.",
  },
] as const;

export function Download() {
  const { state, reset } = useDownloadApk();

  return (
    <section
      id="download"
      className="border-t border-[#111111] bg-[#020202] py-20 md:py-28"
    >
      <Container>
        <p className="font-mono text-xs tracking-[0.3em] text-[#00FF9C]">
          04 {"//"} INSTALL
        </p>
        <h2 className="mt-3 font-mono text-2xl font-bold tracking-[0.15em] text-[#E8E8E8] md:text-4xl">
          PUT D.A.R.K. ON YOUR PHONE.
        </h2>

        <div className="mt-12 grid gap-10 lg:grid-cols-2 lg:items-start">
          <TerminalPanel title={`${APP.name} ${APP.version} - sideload package`}>
            <p className="font-mono text-xs leading-relaxed text-[#7A7A7A]">
              {APP.fullName} v{APP.version} (build {APP.build}). Signed APK,
              no Play Store, no ads, no tracking. Side-loadable on any device
              running {APP.minAndroid} or newer.
            </p>
            <div className="mt-6 flex flex-col gap-3">
              <DownloadButton />
              <DownloadMeta />
            </div>
            {state.status === "error" && state.error ? (
              <div className="mt-4">
                <ErrorState
                  title="Download error"
                  message={state.error}
                  retryLabel="TRY AGAIN"
                  onRetry={reset}
                />
              </div>
            ) : null}
            {state.status === "success" ? (
              <p className="mt-4 border border-[#00FF9C]/30 bg-[#00FF9C]/5 px-4 py-3 font-mono text-xs text-[#00FF9C]">
                {"\u25B8"} Check your downloads folder. If nothing happened,
                your browser blocked automatic saves - use the direct link:{" "}
                <a
                  href={APP.sourceApk}
                  download={APP.downloadFileName}
                  className="underline"
                >
                  {APP.downloadFileName}
                </a>
              </p>
            ) : null}
          </TerminalPanel>

          <div>
            <p className="font-mono text-xs tracking-[0.25em] text-[#7A7A7A]">
              SIDE-LOAD IN 4 STEPS
            </p>
            <ol className="mt-6 space-y-4">
              {INSTALL_STEPS.map((item) => (
                <li
                  key={item.step}
                  className="flex gap-4 border border-[#141414] bg-[#050505] p-4"
                >
                  <span className="font-mono text-sm font-bold text-[#00FF9C]">
                    {item.step}
                  </span>
                  <span className="font-mono text-xs leading-relaxed text-[#E8E8E8]/80">
                    {item.text}
                  </span>
                </li>
              ))}
            </ol>
          </div>
        </div>
      </Container>
    </section>
  );
}
