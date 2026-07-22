import { useEffect, useState } from "react";
import { X, Trophy, ExternalLink } from "lucide-react";
import { useServerFn } from "@tanstack/react-start";
import {
  GAMEMODES,
  MODERN_GAMEMODES,
  GAMEMODE_ICON,
  MODERN_GAMEMODE_ICON,
  TIER_COLOR,
  TIER_POINTS,
  fullPlayerUrl,
  regionFullName,
  titleColorForPoints,
  titleForPoints,
  type GamemodeSlug,
  type Tier,
} from "@/lib/gamemodes";
import type { PvpMode } from "@/hooks/usePvpMode";

import { loadTiers } from "@/lib/tier-cache";
import { resolveMinecraftUuid } from "@/lib/skin.functions";

interface Props {
  playerName: string | null;
  rank?: number | null;
  gamemode?: GamemodeSlug | null;
  mode?: PvpMode;
  onClose: () => void;
}

interface Row {
  gamemode: GamemodeSlug;
  tier: Tier;
  region: string;
  points: number;
  glow: boolean;
}

function medalStyle(rank: number | null | undefined) {
  if (rank === 1)
    return { from: "oklch(0.85 0.17 90)", to: "oklch(0.7 0.15 75)", text: "oklch(0.2 0.04 60)" };
  if (rank === 2)
    return {
      from: "oklch(0.88 0.02 250)",
      to: "oklch(0.72 0.02 250)",
      text: "oklch(0.2 0.02 250)",
    };
  if (rank === 3)
    return { from: "oklch(0.7 0.13 50)", to: "oklch(0.55 0.12 40)", text: "oklch(0.18 0.03 40)" };
  return null;
}

export function PlayerProfileModal({
  playerName,
  rank,
  gamemode,
  mode = "classic",
  onClose,
}: Props) {
  const [rows, setRows] = useState<Row[] | null>(null);
  const [region, setRegion] = useState<string>("NA");
  const [glow, setGlow] = useState(false);
  const [uuid, setUuid] = useState<string | null>(null);
  const resolveUuid = useServerFn(resolveMinecraftUuid);

  useEffect(() => {
    if (!playerName) return;
    let active = true;
    setRows(null);
    setUuid(null);
    const lname = playerName.toLowerCase();
    (async () => {
      try {
        const data = await loadTiers(mode);
        if (!active) return;
        const list = data.entries
          .filter((e) => e.player_name.toLowerCase() === lname)
          .map(
            ({ gamemode: g, tier, region: r, points, glow: gl }) =>
              ({
                gamemode: g,
                tier,
                region: r,
                points,
                glow: gl,
              }) as Row,
          );
        setRows(list);
        if (list[0]) setRegion(list[0].region);
        setGlow(list.some((r) => r.glow));
      } catch {
        if (active) setRows([]);
      }
    })();
    return () => {
      active = false;
    };
  }, [playerName, mode]);

  useEffect(() => {
    if (!playerName) return;
    let active = true;
    (async () => {
      try {
        const res = await resolveUuid({ data: { name: playerName } });
        if (!active) return;
        setUuid(res.uuid);
      } catch {
        if (active) setUuid(null);
      }
    })();
    return () => {
      active = false;
    };
  }, [playerName, resolveUuid]);

  useEffect(() => {
    if (!playerName) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKey);
    document.body.style.overflow = "hidden";
    return () => {
      window.removeEventListener("keydown", onKey);
      document.body.style.overflow = "";
    };
  }, [playerName, onClose]);

  if (!playerName) return null;

  const modesList = mode === "modern" ? MODERN_GAMEMODES : GAMEMODES;
  const iconMap: Record<string, string> = mode === "modern" ? MODERN_GAMEMODE_ICON : GAMEMODE_ICON;

  const total = (rows ?? []).reduce((s, r) => s + (r.points || TIER_POINTS[r.tier]), 0);
  const title = titleForPoints(total);
  const titleColor = titleColorForPoints(total);
  const medal = medalStyle(rank);
  const tiersByMode = new Map<GamemodeSlug, Tier>();
  for (const r of rows ?? []) tiersByMode.set(r.gamemode, r.tier);

  const gamemodeMeta = gamemode ? modesList.find((g) => g.slug === gamemode) : null;
  const gamemodeRow = gamemode ? (rows ?? []).find((r) => r.gamemode === gamemode) : null;
  const positionLabel = gamemodeMeta ? gamemodeMeta.name.toUpperCase() : "OVERALL";
  const positionPoints = gamemodeRow ? gamemodeRow.points || TIER_POINTS[gamemodeRow.tier] : total;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-in fade-in duration-200"
      onClick={onClose}
    >
      <div
        className="ft-surface relative w-full max-w-lg rounded-2xl p-6 shadow-[var(--shadow-tier)] animate-in fade-in zoom-in-95 slide-in-from-bottom-4 duration-300 ease-out"
        onClick={(e) => e.stopPropagation()}
        style={{
          backgroundImage:
            "radial-gradient(ellipse at top, oklch(0.45 0.2 30 / 0.18), transparent 60%)",
        }}
      >
        <button
          onClick={onClose}
          className="absolute right-3 top-3 rounded-md p-1.5 text-muted-foreground hover:bg-accent hover:text-foreground"
          aria-label="Close"
        >
          <X className="h-5 w-5" />
        </button>

        {/* Header */}
        <div className="flex flex-col items-center text-center">
          <div className="profile-avatar-glow relative mt-1 h-44 w-44 overflow-hidden rounded-full bg-black">
            <img
              src={fullPlayerUrl(uuid ?? "MHF_Steve", 256)}
              alt={playerName}
              className="pixel-icon profile-player-model"
              onError={(e) => {
                // Always show *some* clean model render.
                const img = e.currentTarget;
                if (img.src !== fullPlayerUrl("MHF_Steve", 256)) {
                  img.src = fullPlayerUrl("MHF_Steve", 256);
                }
              }}
            />
          </div>

          <h2 className={`mt-4 text-3xl font-black tracking-tight ${glow ? "owner-name" : ""}`}>
            {glow && <span className="mr-1">♛</span>}
            {playerName}
          </h2>

          <div
            className="mt-2 inline-flex min-w-[190px] max-w-[290px] items-center justify-center gap-2 overflow-visible rounded-full px-4 py-1.5 text-xs font-black uppercase leading-none tracking-wide"
            style={
              (rows ?? []).length === 0
                ? {
                    background:
                      "linear-gradient(135deg, oklch(0.4 0.005 250), oklch(0.32 0.005 250))",
                    color: "oklch(0.85 0.01 250)",
                  }
                : {
                    background: titleColor,
                    color: "oklch(0.15 0.02 30)",
                    boxShadow: `0 0 18px -6px ${titleColor}`,
                  }
            }
          >
            <Trophy className="h-3.5 w-3.5 shrink-0" />
            {(rows ?? []).length === 0 ? (
              "Unranked"
            ) : (
              <span className="min-w-0 truncate">{title}</span>
            )}
          </div>

          <div className="mt-2 text-sm text-muted-foreground">{regionFullName(region)}</div>

          <a
            href={`https://namemc.com/profile/${encodeURIComponent(playerName)}`}
            target="_blank"
            rel="noopener noreferrer"
            className="mt-3 inline-flex items-center gap-1.5 rounded-full border border-border/60 bg-background/50 px-3 py-1 text-xs text-muted-foreground hover:bg-accent hover:text-foreground"
          >
            NameMC
            <ExternalLink className="h-3 w-3" />
          </a>
        </div>

        {/* Position */}
        <div className="mt-6 rounded-xl border border-border/60 bg-background/40 p-4">
          <div className="flex items-center gap-3">
            {rank ? (
              <div
                className="ft-rank-badge flex h-14 w-16 items-center justify-center rounded-lg font-black italic text-2xl"
                style={{
                  background: medal
                    ? `linear-gradient(135deg, ${medal.from}, ${medal.to})`
                    : "oklch(0.22 0.02 30)",
                  color: medal ? medal.text : "oklch(0.85 0.02 60)",
                  boxShadow: medal ? `0 0 12px -2px ${medal.from}` : undefined,
                }}
              >
                {rank}.
              </div>
            ) : (
              <div className="flex h-12 w-14 items-center justify-center rounded-md bg-secondary text-muted-foreground">
                <Trophy className="h-5 w-5" />
              </div>
            )}
            <div className="flex-1">
              <div className="text-[10px] uppercase tracking-[0.2em] text-muted-foreground">
                Position
              </div>
              <div className="font-black tracking-wide">{positionLabel}</div>
            </div>
            <div className="text-right">
              <div className="text-[10px] uppercase tracking-[0.2em] text-muted-foreground">
                Points
              </div>
              <div className="text-2xl font-black tabular-nums text-primary">{positionPoints}</div>
            </div>
          </div>
        </div>

        {/* Tiers row */}
        <div className="mt-4 rounded-xl border border-border/60 bg-background/40 p-4">
          <div className="text-[10px] uppercase tracking-[0.2em] text-muted-foreground mb-3">
            {gamemodeMeta ? "Tier" : "Tiers"}
          </div>
          {gamemodeMeta ? (
            <div className="flex items-center justify-between gap-3">
              <div className="flex items-center gap-3 min-w-0">
                <div
                  className="hex-badge flex h-14 w-14 items-center justify-center border bg-card shrink-0"
                  style={
                    gamemodeRow
                      ? {
                          ["--hex-border-color" as any]: TIER_COLOR[gamemodeRow.tier],
                          ["--hex-glow-color" as any]: TIER_COLOR[gamemodeRow.tier],
                          ["--hex-glow" as any]: `0 0 0 2px ${TIER_COLOR[gamemodeRow.tier]}, 0 0 26px -8px ${TIER_COLOR[gamemodeRow.tier]}`,
                        }
                      : { ["--hex-border-color" as any]: "var(--border)" }
                  }
                >
                  <img
                    src={iconMap[gamemodeMeta.slug]}
                    alt={gamemodeMeta.name}
                    className="pixel-icon h-8 w-8 object-contain"
                  />
                </div>
                <div className="min-w-0">
                  <div className="font-black tracking-wide truncate">{gamemodeMeta.name}</div>
                  <div className="text-xs text-muted-foreground">
                    {gamemodeRow ? `${positionPoints} pts` : "Unranked"}
                  </div>
                </div>
              </div>
              {gamemodeRow ? (
                <span
                  className="ft-tier-pill rounded-xl px-5 py-2 text-lg font-black"
                  style={{
                    ["--tier-color" as any]: TIER_COLOR[gamemodeRow.tier],
                    backgroundColor: TIER_COLOR[gamemodeRow.tier],
                    color: "oklch(0.15 0.02 30)",
                    boxShadow: `0 0 16px -4px ${TIER_COLOR[gamemodeRow.tier]}`,
                  }}
                >
                  {gamemodeRow.tier}
                </span>
              ) : (
                <span className="rounded-xl border border-border/60 px-5 py-2 text-lg font-black text-muted-foreground">
                  —
                </span>
              )}
            </div>
          ) : (
            <div className="flex flex-wrap justify-center gap-3 overflow-visible px-2 py-2">
              {modesList.map((g) => {
                const t = tiersByMode.get(g.slug as GamemodeSlug);
                return (
                  <div
                    key={g.slug}
                    className={`flex flex-col items-center gap-1 overflow-visible transition-all duration-700 ease-[cubic-bezier(0.16,1,0.3,1)] hover:-translate-y-0.5 ${t ? "" : "opacity-30"}`}
                    title={g.name}
                  >
                    <div
                      className="hex-badge flex h-11 w-11 items-center justify-center border border-border/60 bg-card transition-all duration-700 ease-[cubic-bezier(0.16,1,0.3,1)] hover:scale-110"
                      style={
                        t
                          ? {
                              ["--hex-border-color" as any]: TIER_COLOR[t],
                              ["--hex-glow-color" as any]: TIER_COLOR[t],
                              ["--hex-glow" as any]: `0 0 0 2px ${TIER_COLOR[t]}, 0 0 22px -10px ${TIER_COLOR[t]}`,
                            }
                          : { ["--hex-border-color" as any]: "color-mix(in oklab, var(--border) 60%, transparent)" }
                      }
                    >
                      <img
                        src={iconMap[g.slug]}
                        alt={g.name}
                        className="pixel-icon h-7 w-7 object-contain"
                      />
                    </div>
                    <div
                      className="pb-0.5 text-center text-[10px] font-black tabular-nums leading-tight"
                      style={{ color: t ? TIER_COLOR[t] : undefined }}
                    >
                      {t ?? "—"}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
