import { Link } from "@tanstack/react-router";
import {
  GAMEMODES,
  MODERN_GAMEMODES,
  GAMEMODE_ICON,
  MODERN_GAMEMODE_ICON,
  TIER_COLOR,
  type GamemodeSlug,
  type Tier,
} from "@/lib/gamemodes";
import type { PvpMode } from "@/hooks/usePvpMode";

interface Props {
  tiers: Partial<Record<GamemodeSlug, Tier>>;
  mode?: PvpMode;
}

export function TierStrip({ tiers, mode = "classic" }: Props) {
  const list = mode === "modern" ? MODERN_GAMEMODES : GAMEMODES;
  const icons: Record<string, string> =
    mode === "modern" ? MODERN_GAMEMODE_ICON : GAMEMODE_ICON;
  return (
    <div className="no-scrollbar flex flex-wrap items-start justify-center gap-3 overflow-x-auto overflow-y-visible px-4 py-4 md:flex-nowrap md:overflow-visible md:px-6 [-webkit-overflow-scrolling:touch]">
      {list.map((g) => {
        const t = tiers[g.slug as GamemodeSlug];
        const ranked = Boolean(t);
        const inner = (
          <div
            className={`flex flex-col items-center gap-1.5 overflow-visible py-1 ${ranked ? "" : "opacity-25"}`}
            title={g.name}
          >
            <div
              className="hex-badge ft-3d flex h-14 w-14 items-center justify-center"
              style={
                ranked
                  ? {
                      ["--hex-border-color" as any]: TIER_COLOR[t!],
                      ["--hex-glow-color" as any]: TIER_COLOR[t!],
                      ["--hex-glow" as any]: `0 0 0 2px ${TIER_COLOR[t!]}, 0 0 26px -8px ${TIER_COLOR[t!]}`,
                    }
                  : { ["--hex-border-color" as any]: "var(--border)" }
              }
            >
              <img
                src={icons[g.slug]}
                alt={g.name}
                className="pixel-icon h-9 w-9 object-contain"
              />
            </div>
            <div
              className="pb-0.5 text-center text-xs font-black tabular-nums leading-tight"
              style={{ color: ranked ? TIER_COLOR[t!] : undefined }}
            >
              {t ?? "—"}
            </div>
          </div>
        );
        return ranked ? (
          <Link
            key={g.slug}
            to="/tier/$gamemode"
            className="block overflow-visible"
            params={{ gamemode: g.slug }}
            onClick={(e) => e.stopPropagation()}
          >
            {inner}
          </Link>
        ) : (
          <div key={g.slug}>{inner}</div>
        );
      })}
    </div>
  );
}
