import Image from "next/image";

export function Banner() {
  return (
    <section className="border-t border-[#111111] bg-black">
      <Image
        src="/banner.png"
        alt="D.A.R.K. banner"
        width={1983}
        height={793}
        priority
        className="mx-auto w-full max-w-6xl px-6 py-12"
      />
    </section>
  );
}
