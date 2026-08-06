import { Container } from "@/components/ui/Container";
import { Badge } from "@/components/ui/Badge";
import { FEATURES } from "@/lib/app";

export function Features() {
  return (
    <section id="features" className="border-t border-[#111111] py-20 md:py-28">
      <Container>
        <p className="font-mono text-xs tracking-[0.3em] text-[#00FF9C]">
          01 {"//"} FEATURES
        </p>
        <h2 className="mt-3 font-mono text-2xl font-bold tracking-[0.15em] text-[#E8E8E8] md:text-4xl">
          EVERYTHING IS TEXT.
          <br />
          <span className="text-[#7A7A7A]">EVERYTHING IS FAST.</span>
        </h2>
        <div className="mt-12 grid gap-px overflow-hidden border border-[#111111] bg-[#111111] sm:grid-cols-2 lg:grid-cols-3">
          {FEATURES.map((feature) => (
            <article
              key={feature.title}
              className="group flex flex-col gap-3 bg-[#050505] p-6 transition-colors hover:bg-[#0A0A0A]"
            >
              <Badge color="muted">{feature.tag}</Badge>
              <h3 className="font-mono text-base font-bold tracking-[0.1em] text-[#E8E8E8] transition-colors group-hover:text-[#00FF9C]">
                {feature.title}
              </h3>
              <p className="font-mono text-xs leading-relaxed text-[#7A7A7A]">
                {feature.description}
              </p>
            </article>
          ))}
        </div>
      </Container>
    </section>
  );
}
