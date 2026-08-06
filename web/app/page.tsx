import { Hero } from "@/components/sections/Hero";
import { Banner } from "@/components/sections/Banner";
import { Features } from "@/components/sections/Features";
import { Screenshots } from "@/components/sections/Screenshots";
import { Commands } from "@/components/sections/Commands";
import { Download } from "@/components/sections/Download";

export default function Home() {
  return (
    <main className="mt-6">
      <Hero />
      <Banner />
      <Features />
      <Screenshots />
      <Commands />
      <Download />
    </main>
  );
}
