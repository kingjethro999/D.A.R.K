import { Container } from "@/components/ui/Container";
import { cn } from "@/lib/utils";

interface PageHeaderProps {
  index: string;
  title: string;
  subtitle: string;
  className?: string;
}

export function PageHeader({ index, title, subtitle, className }: PageHeaderProps) {
  return (
    <Container className={cn("pt-14 md:pt-20", className)}>
      <p className="font-mono text-xs tracking-[0.3em] text-[#00FF9C]">
        {index} {"//"}
      </p>
      <h1 className="mt-4 font-mono text-4xl font-bold tracking-[0.12em] text-[#E8E8E8] md:text-6xl">
        {title}
      </h1>
      <p className="mt-4 max-w-2xl font-mono text-sm leading-relaxed text-[#7A7A7A]">
        {subtitle}
      </p>
    </Container>
  );
}
