import { useEffect, useRef, useState } from "react";
import { avatarUrl, FALLBACK_AVATAR } from "@/lib/gamemodes";

interface Props {
  value: string;
  onChange: (v: string) => void;
  suggestions: string[]; // unique player names available
  onPick: (name: string) => void;
  placeholder?: string;
  className?: string;
}

export function PlayerSearch({
  value,
  onChange,
  suggestions,
  onPick,
  placeholder = "Search players...",
  className,
}: Props) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const onDoc = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") setOpen(false);
    };
    document.addEventListener("mousedown", onDoc);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDoc);
      document.removeEventListener("keydown", onKey);
    };
  }, []);

  const q = value.trim();
  const ql = q.toLowerCase();
  const matches =
    q === ""
      ? []
      : suggestions.filter((n) => n.toLowerCase().includes(ql)).slice(0, 8);

  const validName = /^[A-Za-z0-9_]{2,16}$/.test(q);
  const exact = matches.some((n) => n.toLowerCase() === ql);
  const showUnrankedOption = q !== "" && validName && !exact;

  return (
    <div ref={ref} className={`relative ${className ?? ""}`}>
      <input
        type="text"
        value={value}
        onChange={(e) => {
          onChange(e.target.value);
          setOpen(true);
        }}
        onFocus={() => setOpen(true)}
        placeholder={placeholder}
        className="ft-input w-full rounded-lg px-4 py-2.5 text-sm placeholder:text-muted-foreground/60 focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
      />
      {open && (matches.length > 0 || showUnrankedOption) && (
        <div className="ft-surface absolute z-40 mt-1 w-full overflow-hidden rounded-lg shadow-[var(--shadow-tier)]">
          {matches.map((name) => (
            <button
              key={name}
              type="button"
              onClick={() => {
                setOpen(false);
                onPick(name);
              }}
              className="flex w-full items-center gap-3 px-3 py-2 text-left text-sm hover:bg-accent/50"
            >
              <img
                src={avatarUrl(name, 32)}
                alt=""
                className="h-7 w-7 rounded bg-secondary"
                loading="lazy"
                onError={(e) => {
                  const img = e.currentTarget;
                  if (img.src !== FALLBACK_AVATAR) img.src = FALLBACK_AVATAR;
                }}
              />
              <span className="font-semibold">{name}</span>
            </button>
          ))}
          {showUnrankedOption && (
            <button
              type="button"
              onClick={() => {
                setOpen(false);
                onPick(q);
              }}
              className="flex w-full items-center gap-3 border-t border-border/40 px-3 py-2 text-left text-sm hover:bg-accent/50"
            >
              <img
                src={avatarUrl(q, 32)}
                alt=""
                className="h-7 w-7 rounded bg-secondary opacity-70"
                loading="lazy"
                onError={(e) => {
                  const img = e.currentTarget;
                  if (img.src !== FALLBACK_AVATAR) img.src = FALLBACK_AVATAR;
                }}
              />
              <span className="flex-1 font-semibold">{q}</span>
              <span
                className="rounded-full px-2 py-0.5 text-[10px] font-black uppercase tracking-wider"
                style={{
                  backgroundColor: "oklch(0.3 0.005 250)",
                  color: "oklch(0.7 0.01 250)",
                }}
              >
                Unranked
              </span>
            </button>
          )}
        </div>
      )}
    </div>
  );
}
