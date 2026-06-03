import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useMemo, useState } from "react";
import { useServerFn } from "@tanstack/react-start";
import { SiteFooter } from "@/components/SiteFooter";
import { GamemodeTabs } from "@/components/GamemodeTabs";
import { PlayerSearch } from "@/components/PlayerSearch";
import { PlayerProfileModal } from "@/components/PlayerProfileModal";
import { TierStrip } from "@/components/TierStrip";
import { TierTaggerDownload } from "@/components/TierTaggerDownload";
import {
  REGION_COLOR,
  TIER_COLOR,
  avatarUrl,
  bustUrl,
  FALLBACK_AVATAR,
  titleColorForPoints,
  titleForPoints,
  gamemodesFor,
  tiersFor,
  type GamemodeSlug,
  type Tier,
} from "@/lib/gamemodes";
import { usePvpMode } from "@/hooks/usePvpMode";
import { getCachedTiers, loadTiers } from "@/lib/tier-cache";
import { resolveMinecraftUuids } from "@/lib/skin.functions";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Forgified Tiers — Minecraft PvP Tier Lists" },
      {
        name: "description",
        content:
          "Master Minecraft PvP rankings across classic and modern gamemodes. Curated by Forgified testers.",
      },
      { property: "og:title", content: "Forgified Tiers — Minecraft PvP Tier Lists" },
      {
        property: "og:description",
        content: "Master Minecraft PvP rankings across all gamemodes — classic and modern.",
      },
    ],
  }),
  component: HomeOverall,
});

interface Entry {
  id: string;
  player_name: string;
  gamemode: GamemodeSlug;
  tier: Tier;
  region: string;
  points: number;
  glow: boolean;
  mode_kind: "classic" | "modern";
}

interface PlayerRow {
  name: string;
  region: string;
  total: number;
  bestTier: Tier;
  tiers: Partial<Record<GamemodeSlug, Tier>>;
  glow: boolean;
}

function tierOrder(tier: Tier, mode: "classic" | "modern"): number {
  const list = mode === "modern" ? tiersFor("modern") : tiersFor("classic");
  return (list as readonly string[]).indexOf(tier);
}

function medalFor(rank: number) {
  if (rank === 1)
    return { from: "oklch(0.85 0.17 90)", to: "oklch(0.7 0.15 75)", text: "oklch(0.2 0.04 60)" };
  if (rank === 2)
    return { from: "oklch(0.88 0.02 250)", to: "oklch(0.72 0.02 250)", text: "oklch(0.2 0.02 250)" };
  if (rank === 3)
    return { from: "oklch(0.7 0.13 50)", to: "oklch(0.55 0.12 40)", text: "oklch(0.18 0.03 40)" };
  return null;
}

function buildPlayerRows(
  data: { entries: { player_name: string; gamemode: GamemodeSlug; tier: Tier; region: string; points: number; glow: boolean }[]; totals: Map<string, number> },
  mode: "classic" | "modern",
): PlayerRow[] {
  const map = new Map<string, PlayerRow>();
  for (const e of data.entries) {
    const key = e.player_name.toLowerCase();
    let row = map.get(key);
    if (!row) {
      row = {
        name: e.player_name,
        region: e.region,
        total: 0,
        bestTier: e.tier,
        tiers: {},
        glow: false,
      };
      map.set(key, row);
    }
    row.tiers[e.gamemode] = e.tier;
    if (tierOrder(e.tier, mode) < tierOrder(row.bestTier, mode)) row.bestTier = e.tier;
    if (e.glow) row.glow = true;
  }
  for (const row of map.values()) {
    row.total = data.totals.get(row.name.toLowerCase()) ?? 0;
  }
  return Array.from(map.values()).sort((a, b) => b.total - a.total);
}

function HomeOverall() {
  const [mode] = usePvpMode();
  const cached = getCachedTiers(mode);
  const [rows, setRows] = useState<PlayerRow[] | null>(
    cached ? buildPlayerRows(cached, mode) : null,
  );
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [openPlayer, setOpenPlayer] = useState<string | null>(null);
  const [uuidByName, setUuidByName] = useState<Record<string, string | null>>({});
  const resolveUuids = useServerFn(resolveMinecraftUuids);
  const gamemodes = gamemodesFor(mode);
  const tierList = tiersFor(mode);

  useEffect(() => {
    let active = true;
    const c = getCachedTiers(mode);
    setRows(c ? buildPlayerRows(c, mode) : null);
    setError(null);
    loadTiers(mode)
      .then((data) => {
        if (!active) return;
        setRows(buildPlayerRows(data, mode));
      })
      .catch((err: Error) => {
        if (!active) return;
        setError(err.message);
      });
    return () => {
      active = false;
    };
  }, [mode]);

  const filteredRows = useMemo(() => {
    if (!rows) return rows;
    const q = search.trim().toLowerCase();
    if (!q) return rows;
    return rows.filter((r) => r.name.toLowerCase().includes(q));
  }, [rows, search]);

  useEffect(() => {
    if (!filteredRows || filteredRows.length === 0) return;
    let active = true;
    const names = filteredRows.slice(0, 60).map((r) => r.name);
    (async () => {
      try {
        const res = await resolveUuids({ data: { names } });
        if (!active) return;
        setUuidByName((prev) => ({ ...prev, ...res.uuids }));
      } catch {
        // ignore
      }
    })();
    return () => {
      active = false;
    };
  }, [filteredRows, resolveUuids]);

  const rankByName = useMemo(() => {
    const m = new Map<string, number>();
    rows?.forEach((r, i) => m.set(r.name.toLowerCase(), i + 1));
    return m;
  }, [rows]);

  const playerNames = useMemo(() => rows?.map((r) => r.name) ?? [], [rows]);

  return (
    <div className="min-h-screen bg-background text-foreground">
      <GamemodeTabs active="overall" />
      <main className="mx-auto max-w-7xl px-4 py-10 animate-in fade-in slide-in-from-bottom-2 duration-300 ease-out">
        <div className="mb-4 flex items-center gap-3">
          <div className="flex-1 md:max-w-sm">
            <PlayerSearch
              value={search}
              onChange={setSearch}
              suggestions={playerNames}
              onPick={(name) => setOpenPlayer(name)}
            />
          </div>
          <div className="ml-auto">
            <TierTaggerDownload mode={mode} />
          </div>
        </div>

        {error && (
          <div className="mb-4 rounded-lg border border-destructive/50 bg-destructive/10 p-4 text-sm">
            Failed to load: {error}
          </div>
        )}

        <div className="relative rounded-2xl border border-border/60 bg-card/35 shadow-[var(--shadow-tier)]">
          <div className="ft-overall-grid mx-2 hidden md:grid gap-3 border-b border-border/60 bg-card/60 px-5 py-3 text-[11px] uppercase tracking-[0.18em] text-muted-foreground">
            <div className="text-center">#</div>
            <div>Player</div>
            <div className="text-center">Region</div>
            <div className="text-center">Tiers</div>
            <div className="text-right">Points</div>
          </div>

          {rows === null && !error ? (
            <div className="space-y-2 p-2">
              {Array.from({ length: 6 }).map((_, i) => (
                <div
                  key={i}
                  className="h-[72px] animate-pulse rounded-xl border border-border/40 bg-card/25"
                />
              ))}
            </div>
          ) : rows && rows.length === 0 ? (
            <div className="px-6 py-16 text-center text-muted-foreground">
              No players ranked yet. Add players from the admin panel.
            </div>
          ) : (
            <ul className="space-y-3 overflow-visible p-2">
              {filteredRows?.map((r, i) => {
                  const rank = rankByName.get(r.name.toLowerCase()) ?? i + 1;
                  const medal = medalFor(rank);
                  const uuid = uuidByName[r.name.toLowerCase()] ?? null;
                  return (
                    <li
                      key={r.name}
                      className="row-interactive ft-overall-grid cursor-pointer grid grid-cols-[70px_1fr_auto] items-center gap-3 overflow-visible rounded-xl border border-border/40 bg-card/25 px-3 md:px-5 py-4"
                      onClick={() => setOpenPlayer(r.name)}
                    >
                      <div className="flex items-center">
                        <div
                          className="ft-rank-badge flex h-12 w-16 items-center justify-center rounded-lg font-black italic text-xl shrink-0"
                          style={{
                            background: medal
                              ? `linear-gradient(135deg, ${medal.from}, ${medal.to})`
                              : "oklch(0.22 0.02 30)",
                            color: medal ? medal.text : "oklch(0.7 0.03 60)",
                            boxShadow: medal ? `0 0 12px -2px ${medal.from}` : undefined,
                          }}
                        >
                          {rank}.
                        </div>
                      </div>

                      <div className="flex items-center gap-3 min-w-0">
                        <img
                          src={uuid ? bustUrl(uuid, 160) : bustUrl(r.name, 160)}
                          alt={r.name}
                          loading="lazy"
                          className="pixel-icon ft-avatar h-16 w-16 md:h-[76px] md:w-[76px] shrink-0 object-contain"
                          style={{ ["--avatar-glow" as any]: TIER_COLOR[r.bestTier] }}
                          onError={(e) => {
                            const img = e.currentTarget;
                            const step = img.dataset.fallback ?? "0";
                            if (step === "0") {
                              img.dataset.fallback = "1";
                              // Always show a 3D model, even if we can't resolve skin/uuid (fallback to Steve model).
                              img.src = bustUrl("MHF_Steve", 160);
                            } else if (step === "1") {
                              img.dataset.fallback = "2";
                              img.src = avatarUrl(r.name, 64);
                            } else if (img.src !== FALLBACK_AVATAR) {
                              img.src = FALLBACK_AVATAR;
                            }
                          }}
                        />
                        <div className="min-w-0">
                          <div className={`truncate text-lg font-black tracking-tight md:text-xl ${r.glow ? "owner-name" : ""}`}>
                            {r.glow && <span className="mr-1">♛</span>}
                            {r.name}
                          </div>
                          <div className="text-[11px] text-muted-foreground flex items-center gap-1">
                            <span style={{ color: TIER_COLOR[r.bestTier] }}>◆</span>
                            <span
                              className="ft-title-glow"
                              style={{ ["--title-color" as any]: titleColorForPoints(r.total) } as any}
                            >
                              {titleForPoints(r.total)}
                            </span>
                          </div>
                        </div>
                      </div>

                      <div className="hidden md:flex items-center justify-center">
                        <span
                          className="rounded-full px-2.5 py-1 text-[11px] font-black tracking-wider"
                          style={{
                            backgroundColor: REGION_COLOR[r.region] ?? "oklch(0.4 0.05 30)",
                            color: "oklch(0.97 0.01 60)",
                          }}
                        >
                          {r.region}
                        </span>
                      </div>

                      <div className="hidden md:flex items-center justify-center">
                        <TierStrip tiers={r.tiers} mode={mode} />
                      </div>

                      <div className="text-right font-bold tabular-nums text-base md:text-lg">
                        {r.total}
                      </div>

                      <div className="col-span-3 md:hidden mt-2 flex flex-col gap-2 pl-[70px]">
                        <TierStrip tiers={r.tiers} mode={mode} />
                        <span
                          className="self-start rounded-full px-2 py-0.5 text-[10px] font-black tracking-wider"
                          style={{
                            backgroundColor: REGION_COLOR[r.region] ?? "oklch(0.4 0.05 30)",
                            color: "oklch(0.97 0.01 60)",
                          }}
                        >
                          {r.region}
                        </span>
                      </div>
                    </li>
                  );
                })}
            </ul>
          )}
        </div>

        <div className="mt-6 flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
          <span className="font-semibold uppercase tracking-wider">Tier scale:</span>
          {tierList.map((t) => (
            <span
              key={t}
              className="ft-tier-pill rounded px-1.5 py-0.5 font-bold"
              style={
                {
                  ["--tier-color" as any]: TIER_COLOR[t],
                  backgroundColor: TIER_COLOR[t],
                  color: "oklch(0.15 0.02 30)",
                } as any
              }
            >
              {t}
            </span>
          ))}
        </div>

        <div className="mt-6 flex flex-wrap items-center justify-between gap-3 text-xs text-muted-foreground">
          <div>
            Showing each player's <strong className="text-foreground">best tier</strong> across all
            {" "}{gamemodes.length} gamemodes ({mode === "modern" ? "Modern" : "Classic"} PvP). Click a player to view their gamemode-specific rankings.
          </div>
          <div className="whitespace-nowrap font-semibold">
            {rows?.length ?? 0} ranked players
          </div>
        </div>

        <p className="mt-12 border-t border-border/60 pt-6 text-center text-xs text-muted-foreground">
          Forgified Tiers · Curated by trusted testers · Not affiliated with Mojang
        </p>
      </main>
      <SiteFooter />
      <PlayerProfileModal
        playerName={openPlayer}
        mode={mode}
        rank={openPlayer ? rankByName.get(openPlayer.toLowerCase()) ?? null : null}
        onClose={() => setOpenPlayer(null)}
      />
    </div>
  );
}
