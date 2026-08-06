import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import { ReduxProvider } from "@/providers/ReduxProvider";
import { Navbar } from "@/components/Navbar";
import { Footer } from "@/components/Footer";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "D.A.R.K. — Minimalist Text-Only Android Launcher",
  description:
    "A minimalist, OLED-black, text-only Android launcher with a built-in terminal. Side-load the APK and make your home screen a terminal.",
  keywords: [
    "android launcher",
    "text launcher",
    "OLED black",
    "minimalist",
    "terminal launcher",
    "D.A.R.K.",
  ],
  applicationName: "D.A.R.K.",
  icons: {
    icon: "/favicon.png",
  },
  openGraph: {
    title: "D.A.R.K. — Minimalist Text-Only Android Launcher",
    description:
      "A minimalist, OLED-black, text-only Android launcher with a built-in terminal.",
    images: ["/banner.png"],
    type: "website",
  },
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html
      lang="en"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body className="min-h-full bg-black font-sans text-[#E8E8E8]">
        <ReduxProvider>
          <Navbar />
          {children}
          <Footer />
        </ReduxProvider>
      </body>
    </html>
  );
}
