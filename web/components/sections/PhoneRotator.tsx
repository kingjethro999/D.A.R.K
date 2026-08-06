"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import { SCREENSHOTS } from "@/lib/app";
import { cn } from "@/lib/utils";

export function PhoneRotator() {
  const [index, setIndex] = useState(0);
  const [rotate, setRotate] = useState(false);

  useEffect(() => {
    const timer = setInterval(() => {
      setRotate(true);
      setTimeout(() => {
        setIndex((i) => (i + 1) % SCREENSHOTS.length);
        setRotate(false);
      }, 350);
    }, 4200);
    return () => clearInterval(timer);
  }, []);

  const shot = SCREENSHOTS[index];

  return (
    <div className="flex flex-col items-center">
      <div className="[perspective:1400px]">
        <div
          className={cn(
            "relative rounded-[2.4rem] border border-[#1E1E1E] bg-[#0A0A0A] p-3 shadow-[0_0_80px_rgba(0,255,156,0.08)] transition-transform duration-500",
            rotate
              ? "[transform:rotateY(-12deg)_rotateX(6deg)_scale(0.98)]"
              : "[transform:rotateY(-6deg)_rotateX(3deg)]"
          )}
        >
          <div className="mx-auto flex w-fit items-center gap-1 rounded-t-xl bg-black px-4 pb-1.5 pt-2">
            <span className="h-1.5 w-1.5 rounded-full bg-[#1E1E1E]" />
            <span className="ml-1 h-1 w-20 rounded-full bg-[#1E1E1E]" />
          </div>
          <Image
            src={shot.src}
            alt={shot.alt}
            width={shot.width}
            height={shot.height}
            className="h-[480px] w-auto rounded-2xl object-cover"
          />
          <div
            aria-hidden
            className="pointer-events-none absolute inset-3 rounded-2xl bg-gradient-to-tr from-transparent via-transparent to-white/5"
          />
        </div>
      </div>

      <div className="mt-6 flex items-center gap-2">
        {SCREENSHOTS.map((s, i) => (
          <button
            key={s.src}
            type="button"
            aria-label={`Show ${s.caption}`}
            onClick={() => {
              setIndex(i);
              setRotate(false);
            }}
            className={cn(
              "h-1.5 rounded-full transition-all duration-300",
              i === index ? "w-8 bg-[#00FF9C]" : "w-1.5 bg-[#2A2A2A] hover:bg-[#4A4A4A]"
            )}
          />
        ))}
      </div>

      <p className="mt-3 font-mono text-[10px] tracking-[0.3em] text-[#7A7A7A]">
        {shot.caption}
      </p>
    </div>
  );
}
