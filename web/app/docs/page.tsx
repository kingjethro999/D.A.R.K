import type { Metadata } from "next";
import Link from "next/link";
import { Container } from "@/components/ui/Container";
import { PageHeader } from "@/components/ui/PageHeader";
import { DOC_GROUPS, DOCS } from "@/lib/docs";

export const metadata: Metadata = {
  title: "Docs — D.A.R.K.",
  description:
    "Documentation for D.A.R.K.: installation, the home screen, the complete terminal command reference, settings, gestures, and troubleshooting.",
};

export default function DocsPage() {
  return (
    <main className="mt-6">
      <PageHeader
        index="DOCS"
        title="THE MANUAL"
        subtitle="Everything D.A.R.K. can do, in writing. Pick a document, or use the sidebar on any doc page to jump around."
      />

      <Container className="mt-12">
        <div className="grid gap-px overflow-hidden border border-[#111111] bg-[#111111] sm:grid-cols-2 lg:grid-cols-3">
          {DOC_GROUPS.map((group) => {
            const docs = DOCS.filter((d) => d.group === group);
            return (
              <div key={group} className="bg-[#050505] p-6">
                <p className="font-mono text-[10px] tracking-[0.3em] text-[#00FF9C]">
                  {group} {"//"}
                </p>
                <ul className="mt-4 space-y-1">
                  {docs.map((doc) => (
                    <li key={doc.slug}>
                      <Link
                        href={`/docs/${doc.slug}`}
                        className="group block border-b border-[#111111] py-3 transition-colors hover:border-[#00FF9C]/30"
                      >
                        <span className="font-mono text-sm font-bold tracking-[0.1em] text-[#E8E8E8] group-hover:text-[#00FF9C]">
                          {doc.title}
                        </span>
                        <span className="mt-1 block font-mono text-[11px] leading-relaxed text-[#7A7A7A]">
                          {doc.description}
                        </span>
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>
            );
          })}
        </div>
      </Container>
    </main>
  );
}
