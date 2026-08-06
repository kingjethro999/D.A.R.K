import { cn } from "@/lib/utils";

interface BadgeProps {
  children: React.ReactNode;
  color?: "green" | "amber" | "muted";
  className?: string;
}

const colors = {
  green: "border-[#00FF9C]/40 text-[#00FF9C]",
  amber: "border-[#FFB300]/40 text-[#FFB300]",
  muted: "border-[#7A7A7A]/40 text-[#7A7A7A]",
} as const;

export function Badge({ children, color = "green", className }: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-block border px-2 py-0.5 font-mono text-[10px] tracking-[0.2em]",
        colors[color],
        className
      )}
    >
      {children}
    </span>
  );
}
