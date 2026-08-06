import { Container } from "@/components/ui/Container";
import { TerminalPanel } from "@/components/ui/TerminalPanel";
import { COMMANDS } from "@/lib/app";

export function Commands() {
  return (
    <section id="commands" className="border-t border-[#111111] py-20 md:py-28">
      <Container>
        <p className="font-mono text-xs tracking-[0.3em] text-[#00FF9C]">
          03 {"//"} COMMANDS
        </p>
        <h2 className="mt-3 font-mono text-2xl font-bold tracking-[0.15em] text-[#E8E8E8] md:text-4xl">
          THE TERMINAL IS REAL.
        </h2>
        <p className="mt-4 max-w-2xl font-mono text-sm leading-relaxed text-[#7A7A7A]">
          A built-in command line lives at the bottom of the app list. It
          understands natural language, suggests similar commands on typos,
          and passes anything else to the Linux shell.
        </p>
        <div className="mt-12 grid gap-6 lg:grid-cols-2">
          {COMMANDS.map((example) => (
            <TerminalPanel key={example.command} title="root@dark:~#">
              <p className="mb-2 font-mono text-[11px] tracking-[0.15em] text-[#00FF9C]">
                {"$"} {example.command}
              </p>
              <div className="space-y-1">
                {example.output.map((line, index) => (
                  <p
                    key={`${example.command}-${index}`}
                    className="font-mono text-xs leading-relaxed text-[#9EFFC7]"
                  >
                    {line}
                  </p>
                ))}
              </div>
              <p className="mt-3 border-t border-[#141414] pt-3 font-mono text-[10px] tracking-[0.15em] text-[#7A7A7A]">
                {"//"} {example.description}
              </p>
            </TerminalPanel>
          ))}
        </div>
      </Container>
    </section>
  );
}
