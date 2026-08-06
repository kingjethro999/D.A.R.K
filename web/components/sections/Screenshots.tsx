import Image from "next/image";
import { Container } from "@/components/ui/Container";
import { SCREENSHOTS } from "@/lib/app";
import { PhoneRotator } from "@/components/sections/PhoneRotator";

export function Screenshots() {
  return (
    <section
      id="screenshots"
      className="border-t border-[#111111] bg-[#020202] py-20 md:py-28"
    >
      <Container>
        <p className="font-mono text-xs tracking-[0.3em] text-[#00FF9C]">
          02 {"//"} SCREENSHOTS
        </p>
        <h2 className="mt-3 font-mono text-2xl font-bold tracking-[0.15em] text-[#E8E8E8] md:text-4xl">
          THE HOME SCREEN IS THE WHOLE POINT.
        </h2>
        <div className="mt-12 grid items-start gap-10 lg:grid-cols-2">
          <PhoneRotator />
          <div className="grid gap-8 sm:grid-cols-2">
            {SCREENSHOTS.map((shot) => (
              <figure key={shot.src} className="group">
                <div className="mx-auto w-fit rounded-[1.8rem] border border-[#1E1E1E] bg-[#0A0A0A] p-2">
                  <div className="mx-auto flex w-fit items-center gap-1 rounded-t-lg bg-black px-3 pb-1 pt-2">
                    <span className="h-1.5 w-1.5 rounded-full bg-[#1E1E1E]" />
                    <span className="ml-1 h-1 w-16 rounded-full bg-[#1E1E1E]" />
                  </div>
                  <Image
                    src={shot.src}
                    alt={shot.alt}
                    width={shot.width}
                    height={shot.height}
                    className="h-[320px] w-auto rounded-xl object-cover transition-transform duration-300 group-hover:scale-[1.02]"
                  />
                </div>
                <figcaption className="mt-3 text-center font-mono text-[10px] tracking-[0.3em] text-[#7A7A7A]">
                  {shot.caption}
                </figcaption>
              </figure>
            ))}
          </div>
        </div>
      </Container>
    </section>
  );
}
