import type { DocBlock } from "@/lib/docs";
import { CopyButton } from "@/components/docs/CopyButton";

export function DocBlocks({ blocks }: { blocks: DocBlock[] }) {
  return (
    <div className="space-y-6">
      {blocks.map((block, index) => {
        switch (block.type) {
          case "heading":
            return (
              <h2
                key={index}
                className={`font-mono font-bold tracking-[0.1em] text-[#E8E8E8] ${
                  block.level === 3 ? "text-lg" : "mt-10 text-xl"
                }`}
              >
                {block.text}
              </h2>
            );
          case "p":
            return (
              <p
                key={index}
                className="font-mono text-sm leading-relaxed text-[#9A9A9A]"
              >
                {block.text}
              </p>
            );
          case "list":
            return (
              <ul key={index} className="space-y-2">
                {block.items?.map((item) => (
                  <li
                    key={item}
                    className="flex gap-2 font-mono text-sm leading-relaxed text-[#9A9A9A]"
                  >
                    <span className="mt-px text-[#00FF9C]">{"\u25B8"}</span>
                    <span>{item}</span>
                  </li>
                ))}
              </ul>
            );
          case "code":
            return (
              <div
                key={index}
                className="overflow-hidden border border-[#1E1E1E] bg-[#030303]"
              >
                <div className="flex items-center justify-between border-b border-[#1E1E1E] bg-[#0A0A0A] px-4 py-2">
                  <span className="font-mono text-[10px] tracking-[0.2em] text-[#4A4A4A]">
                    root@dark:~#
                  </span>
                  {block.text ? <CopyButton text={block.text} /> : null}
                </div>
                <pre className="overflow-x-auto p-4 font-mono text-sm leading-relaxed text-[#00FF9C]">
                  {block.text}
                </pre>
              </div>
            );
          case "table":
            return (
              <div
                key={index}
                className="overflow-x-auto border border-[#141414]"
              >
                <table className="w-full border-collapse font-mono text-xs">
                  <tbody>
                    {block.rows?.map((row, rowIndex) => (
                      <tr
                        key={rowIndex}
                        className={
                          row.isHeader
                            ? "bg-[#0A0A0A]"
                            : rowIndex % 2 === 0
                              ? "bg-[#050505]"
                              : "bg-[#030303]"
                        }
                      >
                        {row.cols.map((cell, cellIndex) => (
                          <td
                            key={cellIndex}
                            className={`border-b border-[#111111] px-4 py-3 ${
                              row.isHeader
                                ? "font-bold tracking-[0.15em] text-[#00FF9C]"
                                : cellIndex === 0
                                  ? "text-[#E8E8E8]"
                                  : "text-[#7A7A7A]"
                            }`}
                          >
                            {cell}
                          </td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            );
          case "warn":
            return (
              <div
                key={index}
                className="border border-[#FFB300]/30 bg-[#FFB300]/5 p-4"
              >
                <p className="font-mono text-xs leading-relaxed text-[#FFB300]">
                  {"\u26A0"} {block.text}
                </p>
              </div>
            );
          default:
            return null;
        }
      })}
    </div>
  );
}
