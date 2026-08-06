import type { Metadata } from "next";
import Link from "next/link";
import { Container } from "@/components/ui/Container";
import { PageHeader } from "@/components/ui/PageHeader";
import { TerminalPanel } from "@/components/ui/TerminalPanel";
import { DownloadButton, DownloadMeta } from "@/components/ui/DownloadButton";
import { APP } from "@/lib/app";

export const metadata: Metadata = {
  title: "Download — D.A.R.K.",
  description:
    "Download D.A.R.K. v1.0.6 — a 1.7 MB signed APK. No Play Store, no ads, no tracking. Side-loadable on Android 8.0+.",
};

const APK_SHA256 = "5b9f906b0094866de0cd0c1c491bb5bb783e2e196c47fb4ac2ccc9bc60b7d09c";

const REQUIREMENTS = [
  { k: "ANDROID", v: "8.0 (API 26) or newer" },
  { k: "TARGET", v: "SDK 35" },
  { k: "SIZE", v: "1.7 MB" },
  { k: "SIGNED", v: "Yes — release build" },
  { k: "STORE", v: "None — direct APK" },
  { k: "ADS", v: "Zero" },
] as const;

const STEPS = [
  {
    step: "01",
    title: "Download",
    text: `Tap DOWNLOAD v${APP.version}. Your browser saves ${APP.downloadFileName}.`,
  },
  {
    step: "02",
    title: "Open",
    text: "Tap the file. Android asks to allow installing from this source — allow it.",
  },
  {
    step: "03",
    title: "Install",
    text: "Confirm the installer. No permissions are requested at install time.",
  },
  {
    step: "04",
    title: "Set home",
    text: "Press Home, pick D.A.R.K., tap Always. Double-tap the screen to open the terminal.",
  },
] as const;

export default function DownloadPage() {
  return (
    <main className="mt-6">
      <PageHeader
        index="DOWNLOAD"
        title="GET THE APK"
        subtitle="One signed file, 1.7 megabytes, no Play Store in the chain. Side-load it on any Android 8.0+ device and your home screen becomes a terminal."
      />

      <Container className="mt-12 grid gap-10 lg:grid-cols-2 lg:items-start">
        <TerminalPanel title={`${APP.name} ${APP.version} - sideload package`}>
          <p className="font-mono text-xs leading-relaxed text-[#7A7A7A]">
            {APP.fullName} v{APP.version} (build {APP.build}). Compiled with R8
            minification and resource shrinking, signed as a release build, and
            served from this site. No telemetry is bundled.
          </p>
          <div className="mt-6 flex flex-col gap-3">
            <DownloadButton />
            <DownloadMeta />
          </div>
          <p className="mt-4 font-mono text-[10px] tracking-[0.2em] text-[#4A4A4A]">
            SHA-256: {APK_SHA256}
          </p>
          <p className="mt-2 font-mono text-[10px] tracking-[0.2em] text-[#4A4A4A]">
            Direct link:{" "}
            <a
              href={APP.sourceApk}
              download={APP.downloadFileName}
              className="text-[#7A7A7A] underline hover:text-[#00FF9C]"
            >
              {APP.downloadFileName}
            </a>
          </p>
        </TerminalPanel>

        <div>
          <p className="font-mono text-xs tracking-[0.25em] text-[#7A7A7A]">
            REQUIREMENTS
          </p>
          <dl className="mt-4 grid grid-cols-2 gap-px overflow-hidden border border-[#111111] bg-[#111111]">
            {REQUIREMENTS.map((req) => (
              <div key={req.k} className="bg-[#050505] p-4">
                <dt className="font-mono text-[10px] tracking-[0.2em] text-[#4A4A4A]">
                  {req.k}
                </dt>
                <dd className="mt-1 font-mono text-xs font-bold text-[#E8E8E8]">
                  {req.v}
                </dd>
              </div>
            ))}
          </dl>

          <p className="mt-10 font-mono text-xs tracking-[0.25em] text-[#7A7A7A]">
            SIDE-LOAD IN 4 STEPS
          </p>
          <ol className="mt-4 space-y-3">
            {STEPS.map((item) => (
              <li
                key={item.step}
                className="flex gap-4 border border-[#141414] bg-[#050505] p-4"
              >
                <span className="font-mono text-sm font-bold text-[#00FF9C]">
                  {item.step}
                </span>
                <div>
                  <p className="font-mono text-xs font-bold tracking-[0.15em] text-[#E8E8E8]">
                    {item.title}
                  </p>
                  <p className="mt-1 font-mono text-xs leading-relaxed text-[#7A7A7A]">
                    {item.text}
                  </p>
                </div>
              </li>
            ))}
          </ol>
        </div>
      </Container>

      <Container className="mt-14">
        <div className="flex flex-col gap-4 border border-[#141414] bg-[#050505] p-6 sm:flex-row sm:items-center sm:justify-between">
          <p className="font-mono text-xs leading-relaxed text-[#7A7A7A]">
            Curious how this build came to exist?{" "}
            <Link href="/journey" className="text-[#00FF9C] underline">
              Read the journey.
            </Link>{" "}
            Installing for the first time?{" "}
            <Link href="/docs/installation" className="text-[#00FF9C] underline">
              Follow the install guide.
            </Link>
          </p>
        </div>
      </Container>
    </main>
  );
}
