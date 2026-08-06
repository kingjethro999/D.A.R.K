import type { Metadata } from "next";
import { notFound } from "next/navigation";
import Link from "next/link";
import { Container } from "@/components/ui/Container";
import { DocsSidebar } from "@/components/docs/DocsSidebar";
import { DocBlocks } from "@/components/docs/DocBlocks";
import { DOCS, findDoc } from "@/lib/docs";

export function generateStaticParams() {
  return DOCS.map((doc) => ({ slug: doc.slug }));
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ slug: string }>;
}): Promise<Metadata> {
  const { slug } = await params;
  const doc = findDoc(slug);
  if (!doc) return { title: "Not found — D.A.R.K." };
  return {
    title: `${doc.title} — D.A.R.K. Docs`,
    description: doc.description,
  };
}

export default async function DocPage({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;
  const doc = findDoc(slug);
  if (!doc) notFound();

  const index = DOCS.findIndex((d) => d.slug === slug);
  const prev = DOCS[index - 1];
  const next = DOCS[index + 1];

  return (
    <main className="mt-6">
      <Container className="grid gap-10 pt-12 lg:grid-cols-[260px_1fr]">
        <DocsSidebar />

        <article>
          <p className="font-mono text-xs tracking-[0.3em] text-[#00FF9C]">
            DOCS {"//"} {doc.group}
          </p>
          <h1 className="mt-3 font-mono text-3xl font-bold tracking-[0.12em] text-[#E8E8E8] md:text-4xl">
            {doc.title}
          </h1>
          <p className="mt-3 font-mono text-sm leading-relaxed text-[#7A7A7A]">
            {doc.description}
          </p>

          <div className="mt-10">
            <DocBlocks blocks={doc.blocks} />
          </div>

          <div className="mt-14 flex flex-col gap-3 border-t border-[#141414] pt-6 sm:flex-row sm:justify-between">
            {prev ? (
              <Link
                href={`/docs/${prev.slug}`}
                className="font-mono text-xs tracking-[0.2em] text-[#7A7A7A] transition-colors hover:text-[#00FF9C]"
              >
                {"\u2190"} {prev.title}
              </Link>
            ) : (
              <span />
            )}
            {next ? (
              <Link
                href={`/docs/${next.slug}`}
                className="font-mono text-xs tracking-[0.2em] text-[#7A7A7A] transition-colors hover:text-[#00FF9C]"
              >
                {next.title} {"\u2192"}
              </Link>
            ) : null}
          </div>
        </article>
      </Container>
    </main>
  );
}
