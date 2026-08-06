import Link from "next/link";
import { ErrorState } from "@/components/ErrorState";

export default function NotFound() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-black p-6">
      <div className="w-full max-w-md">
        <ErrorState
          title="404"
          message="That path does not exist in this kernel."
        />
        <Link
          href="/"
          className="mt-4 block text-center font-mono text-xs tracking-[0.2em] text-[#00FF9C] hover:underline"
        >
          RETURN TO HOME
        </Link>
      </div>
    </main>
  );
}
