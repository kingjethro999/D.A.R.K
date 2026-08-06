import { cn } from "@/lib/utils";

interface TerminalPanelProps {
  title: string;
  children: React.ReactNode;
  className?: string;
  bodyClassName?: string;
}

export function TerminalPanel({
  title,
  children,
  className,
  bodyClassName,
}: TerminalPanelProps) {
  return (
    <div
      className={cn(
        "overflow-hidden border border-[#1E1E1E] bg-[#050505]",
        className
      )}
    >
      <div className="flex items-center gap-2 border-b border-[#1E1E1E] bg-[#0A0A0A] px-4 py-3">
        <span className="h-2.5 w-2.5 rounded-full bg-[#FF5F57]" />
        <span className="h-2.5 w-2.5 rounded-full bg-[#FEBC2E]" />
        <span className="h-2.5 w-2.5 rounded-full bg-[#28C840]" />
        <span className="ml-3 font-mono text-[11px] tracking-[0.15em] text-[#7A7A7A]">
          {title}
        </span>
      </div>
      <div className={cn("p-5", bodyClassName)}>{children}</div>
    </div>
  );
}
