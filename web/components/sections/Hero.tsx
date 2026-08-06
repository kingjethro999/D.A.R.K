import Image from "next/image";
import Link from "next/link";
import { Container } from "@/components/ui/Container";
import { Badge } from "@/components/ui/Badge";

const HERO_STATS = [
  { value: "100%", label: "TEXT-ONLY" },
  { value: "0", label: "ICONS" },
  { value: "26", label: "API MIN SDK" },
  { value: "FREE", label: "NO ADS" },
] as const;

export function Hero() {
  return (
    <section id="top" className="relative overflow-hidden">
      <div
        aria-hidden
        className="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_at_top_right,rgba(0,255,156,0.06),transparent_60%)]"
      />
      <Container className="relative flex flex-col items-center gap-12 py-20 md:flex-row md:items-center md:py-28">
        <div className="flex flex-1 flex-col items-center gap-6 text-center md:items-start md:text-left">
          <Badge>ANDROID LAUNCHER</Badge>
          <h1 className="font-mono text-5xl font-bold tracking-[0.18em] text-[#E8E8E8] md:text-7xl">
            D<span className="text-[#00FF9C]">.</span>A
            <span className="text-[#00FF9C]">.</span>R
            <span className="text-[#00FF9C]">.</span>K
            <span className="text-[#00FF9C]">.</span>
          </h1>
          <p className="max-w-md font-mono text-sm leading-relaxed text-[#7A7A7A]">
            Developers&apos; Adaptive Responsive Kernel. A minimalist,
            text-only, OLED-black launcher that renders instantly and ships
            with a real terminal.
          </p>
          <div className="flex flex-col gap-3 sm:flex-row">
            <Link
              href="/download"
              className="bg-[#00FF9C] px-6 py-3 text-center font-mono text-xs font-bold tracking-[0.25em] text-black transition-opacity hover:opacity-80"
            >
              INSTALL APK
            </Link>
            <Link
              href="/terminal"
              className="border border-[#333333] px-6 py-3 text-center font-mono text-xs tracking-[0.25em] text-[#E8E8E8] transition-colors hover:border-[#00FF9C] hover:text-[#00FF9C]"
            >
              TRY THE TERMINAL
            </Link>
            <Link
              href="/journey"
              className="border border-[#333333] px-6 py-3 text-center font-mono text-xs tracking-[0.25em] text-[#E8E8E8] transition-colors hover:border-[#00FF9C] hover:text-[#00FF9C]"
            >
              THE JOURNEY
            </Link>
          </div>
          <dl className="mt-4 grid w-full max-w-md grid-cols-2 gap-3 sm:grid-cols-4">
            {HERO_STATS.map((stat) => (
              <div
                key={stat.label}
                className="border border-[#141414] bg-[#050505] px-3 py-3"
              >
                <dt className="order-2 mt-1 font-mono text-[9px] tracking-[0.2em] text-[#7A7A7A]">
                  {stat.label}
                </dt>
                <dd className="order-1 font-mono text-lg font-bold text-[#00FF9C]">
                  {stat.value}
                </dd>
              </div>
            ))}
          </dl>
        </div>
        <div className="relative flex-shrink-0">
          <Image
            src="/main.png"
            alt="D.A.R.K. app icon"
            width={420}
            height={420}
            priority
            className="h-64 w-64 rounded-2xl object-cover md:h-[420px] md:w-[420px]"
          />
        </div>
      </Container>
    </section>
  );
}
