import { cn } from "@/lib/utils";

interface SectionHeadingProps {
  index: string;
  title: string;
  subtitle?: string;
  className?: string;
}

export function SectionHeading({
  index,
  title,
  subtitle,
  className,
}: SectionHeadingProps) {
  return (
    <div className={cn("mb-10", className)}>
      <p className="font-mono text-xs tracking-[0.3em] text-[#00FF9C]">
        {index} {"//"} {title}
      </p>
      {subtitle ? (
        <p className="mt-3 max-w-2xl font-mono text-sm leading-relaxed text-[#7A7A7A]">
          {subtitle}
        </p>
      ) : null}
    </div>
  );
}
