import { createFileRoute, Link, notFound } from "@tanstack/react-router";
import { useEffect, useMemo, useState } from "react";
import { useServerFn } from "@tanstack/react-start";
import { SiteFooter } from "@/components/SiteFooter";
import { GamemodeTabs } from "@/components/GamemodeTabs";
import { PlayerSearch } from "@/components/PlayerSearch";
import { PlayerProfileModal } from "@/components/PlayerProfileModal";
import { TierTaggerDownload } from "@/components/TierTaggerDownload";
import {
  GAMEMODES,
  MODERN_GAMEMODES,
  REGION_COLOR,
  TIER_COLOR,
  TIER_POINTS,
  avatarUrl,
  bustUrl,
  FALLBACK_AVATAR,
  titleColorForPoints,
  titleForPoints,
  tiersFor,
  type GamemodeSlug,
  type Tier,
} from "@/lib/gamemodes";
import { usePvpMode } from "@/hooks/usePvpMode";
import { getCachedTiers, loadTiers } from "@/lib/tier-cache";
import { resolveMinecraftUuids } from "@/lib/skin.functions";

const ALL_SLUGS = [
  ...GAMEMODES.map((g) => g.slug),
  ...MODERN_GAMEMODES.map((g) => g.slug),
] as readonly string[];

export const Route = createFileRoute("/tier/$gamemode")({
  beforeLoad: ({ params }) => {
    if (!ALL_SLUGS.includes(params.gamemode)) throw notFound();
  },
  head: ({ params }) => {
    const g =
      GAMEMODES.find((x) => x.slug === params.gamemode) ??
      MODERN_GAMEMODES.find((x) => x.slug === params.gamemode);
    const name = g?.name ?? "Tier List";
    return {
      meta: [
        { title: `${name} Tier List — Forgified Tiers` },
        { name: "description", content: `Official ${name} PvP tier list. ${g?.description ?? ""}` },
        { property: "og:title", content: `${name} Tier List — Forgified Tiers` },
        { property: "og:description", content: g?.description ?? "Minecraft PvP tier list" },
      ],
    };
  },
  component: GamemodePage,
  notFoundComponent: () => (
    <div className="min-h-screen bg-background text-foreground">
      <div className="mx-auto max-w-md px-4 py-20 text-center">
        <h1 className="text-3xl font-bold">Gamemode not found</h1>
        <Link to="/" className="mt-4 inline-block text-primary hover:underline">
          ← Back to home
        </Link>
      </div>
    </div>
  ),
});

interface Entry {
  id: string;
  player_name: string;
  tier: Tier;
  position: number;
  region: string;
  points: number;
  title: string | null;
  glow: boolean;
}

function GamemodePage() {
  const { gamemode } = Route.useParams() as { gamemode: GamemodeSlug };
  const [pvpMode] = usePvpMode();
  const isModern = MODERN_GAMEMODES.some((g) => g.slug === gamemode);
  // For shared slug "uhc", honor the user's mode; otherwise infer from slug.
  const effectiveMode: "classic" | "modern" =
    gamemode === "uhc" ? pvpMode : isModern ? "modern" : "classic";
  const meta =
    (effectiveMode === "modern" ? MODERN_GAMEMODES : GAMEMODES).find(
      (g) => g.slug === gamemode,
    ) ?? GAMEMODES.find((g) => g.slug === gamemode) ?? MODERN_GAMEMODES.find((g) => g.slug === gamemode)!;
  const tierList = tiersFor(effectiveMode) as readonly Tier[];

  const buildView = (data: { entries: (Entry & { gamemode: string })[]; totals: Map<string, number> }) => {
    const filtered = data.entries.filter((e) => e.gamemode === gamemode);
    const sorted = filtered.slice().sort((a, b) => {
      const ai = (tierList as readonly string[]).indexOf(a.tier);
      const bi = (tierList as readonly string[]).indexOf(b.tier);
      if (ai !== bi) return ai - bi;
      if (b.points !== a.points) return b.points - a.points;
      return (a.position ?? 0) - (b.position ?? 0);
    });
    return { entries: sorted, totals: data.totals };
  };

  const initial = (() => {
    const c = getCachedTiers(effectiveMode);
    if (!c) return { entries: null, totals: new Map<string, number>() };
    const v = buildView(c as unknown as { entries: (Entry & { gamemode: string })[]; totals: Map<string, number> });
    return { entries: v.entries as Entry[], totals: v.totals };
  })();

  const [entries, setEntries] = useState<Entry[] | null>(initial.entries);
  const [totals, setTotals] = useState<Map<string, number>>(initial.totals);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [openPlayer, setOpenPlayer] = useState<string | null>(null);
  const [uuidByName, setUuidByName] = useState<Record<string, string | null>>({});
  const resolveUuids = useServerFn(resolveMinecraftUuids);

  useEffect(() => {
    let active = true;
    const c = getCachedTiers(effectiveMode);
    if (c) {
      const v = buildView(c as unknown as { entries: (Entry & { gamemode: string })[]; totals: Map<string, number> });
      setEntries(v.entries as Entry[]);
      setTotals(v.totals);
    } else {
      setEntries(null);
    }
    setError(null);
    setSearch("");
    loadTiers(effectiveMode)
      .then((data) => {
        if (!active) return;
        const v = buildView(data as unknown as { entries: (Entry & { gamemode: string })[]; totals: Map<string, number> });
        setEntries(v.entries as Entry[]);
        setTotals(v.totals);
      })
      .catch((err: Error) => {
        if (!active) return;
        setError(err.message);
      });
    return () => {
      active = false;
    };
  }, [gamemode, effectiveMode]);

  const playerNames = useMemo(
    () => entries?.map((e) => e.player_name) ?? [],
    [entries],
  );

  const filtered =
    search.trim() === ""
      ? entries
      : entries?.filter((e) =>
          e.player_name.toLowerCase().includes(search.trim().toLowerCase()),
        );

  useEffect(() => {
    if (!filtered || filtered.length === 0) return;
    let active = true;
    const names = filtered.slice(0, 60).map((e) => e.player_name);
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
  }, [filtered, resolveUuids]);

  return (
    <div className="min-h-screen bg-background text-foreground">
      <GamemodeTabs active={gamemode} />
      <main key={gamemode} className="mx-auto max-w-6xl px-4 py-8 md:py-10 animate-in fade-in slide-in-from-bottom-2 duration-300 ease-out">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <div className="md:max-w-sm flex-1 min-w-[200px]">
            <PlayerSearch
              value={search}
              onChange={setSearch}
              suggestions={playerNames}
              onPick={(name) => setOpenPlayer(name)}
            />
          </div>
          <TierTaggerDownload mode={effectiveMode} />
        </div>

        {error && (
          <div className="rounded-lg border border-destructive/50 bg-destructive/10 p-4 text-sm text-destructive-foreground">
            Failed to load tier list: {error}
          </div>
        )}

        <div className="overflow-visible rounded-2xl border border-border/60 bg-card/35 shadow-[var(--shadow-tier)]">
          <div className="ft-gamemode-grid mx-2 hidden md:grid gap-3 border-b border-border/60 bg-card/60 px-5 py-3 text-[11px] uppercase tracking-[0.18em] text-muted-foreground">
            <div className="text-center">#</div>
            <div>Player</div>
            <div className="text-center">Region</div>
            <div className="text-right">Tier</div>
          </div>

          {entries === null && !error ? (
            <div className="space-y-2 p-2">
              {Array.from({ length: 8 }).map((_, i) => (
                <div
                  key={i}
                  className="h-[76px] animate-pulse rounded-xl border border-border/40 bg-card/25"
                />
              ))}
            </div>
          ) : filtered && filtered.length === 0 ? (
            <div className="px-6 py-16 text-center text-muted-foreground">
              No players ranked yet for {meta.name}.
            </div>
          ) : (
          <ul className="space-y-3 overflow-visible p-2">
              {filtered?.map((e, idx) => {
                const rank = (entries ?? []).indexOf(e) + 1;
                return (
                  <li
                    key={e.id}
                    style={{ animationDelay: `${Math.min(idx * 25, 400)}ms` }}
                    className="animate-in fade-in slide-in-from-bottom-1 duration-300 ease-out fill-mode-both"
                  >
                    <PlayerRow
                      rank={rank}
                      entry={e}
                      uuid={uuidByName[e.player_name.toLowerCase()] ?? null}
                      overallTotal={totals.get(e.player_name.toLowerCase()) ?? (e.points || TIER_POINTS[e.tier])}
                      onOpen={() => setOpenPlayer(e.player_name)}
                    />
                  </li>
                );
              })}
            </ul>
          )}
        </div>

        <div className="mt-8 flex flex-wrap items-center justify-between gap-3 text-xs text-muted-foreground">
          <span>Rankings curated by Forgified Tiers testers · Updated regularly</span>
          <span className="font-semibold">{entries?.length ?? 0} ranked players</span>
        </div>
      </main>
      <SiteFooter />
      <PlayerProfileModal
        playerName={openPlayer}
        gamemode={gamemode}
        mode={effectiveMode}
        rank={
          openPlayer
            ? ((entries ?? []).findIndex(
                (e) => e.player_name.toLowerCase() === openPlayer.toLowerCase(),
              ) + 1) || null
            : null
        }
        onClose={() => setOpenPlayer(null)}
      />
    </div>
  );
}

function PlayerRow({
  rank,
  entry,
  uuid,
  overallTotal,
  onOpen,
}: {
  rank: number;
  entry: Entry;
  uuid: string | null;
  overallTotal: number;
  onOpen: () => void;
}) {
  const tierColor = TIER_COLOR[entry.tier];
  const regionColor = REGION_COLOR[entry.region] ?? "oklch(0.4 0.05 30)";
  const points = entry.points || TIER_POINTS[entry.tier];
  const title = titleForPoints(overallTotal);
  const titleColor = titleColorForPoints(overallTotal);

  const medal =
    rank === 1
      ? { from: "oklch(0.85 0.17 90)", to: "oklch(0.7 0.15 75)", text: "oklch(0.2 0.04 60)" }
      : rank === 2
        ? { from: "oklch(0.88 0.02 250)", to: "oklch(0.72 0.02 250)", text: "oklch(0.2 0.02 250)" }
        : rank === 3
          ? { from: "oklch(0.7 0.13 50)", to: "oklch(0.55 0.12 40)", text: "oklch(0.18 0.03 40)" }
          : null;

  return (
    <div
      onClick={onOpen}
      className="row-interactive ft-gamemode-grid cursor-pointer overflow-visible rounded-xl border border-border/40 bg-card/25 px-3 md:px-5 py-4 md:grid md:items-center md:gap-3"
    >
      <div className="flex items-center gap-3 md:contents">
        <div className="md:flex md:items-center">
          <div
            className="ft-rank-badge flex h-12 w-16 md:h-14 md:w-[72px] items-center justify-center rounded-lg font-black italic text-xl md:text-2xl shrink-0"
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
        </div>

        <div className="flex items-center gap-4 min-w-0 flex-1">
          <img
            src={uuid ? bustUrl(uuid, 160) : bustUrl(entry.player_name, 160)}
            alt={entry.player_name}
            className="pixel-icon ft-avatar h-16 w-16 md:h-[76px] md:w-[76px] shrink-0 object-contain"
            style={{ ["--avatar-glow" as any]: tierColor }}
            loading="lazy"
            onError={(e) => {
              const img = e.currentTarget;
              const step = img.dataset.fallback ?? "0";
              if (step === "0") {
                img.dataset.fallback = "1";
                // Always show a 3D model, even if uuid/skin fetch fails.
                img.src = bustUrl("MHF_Steve", 160);
              } else if (step === "1") {
                img.dataset.fallback = "2";
                img.src = avatarUrl(entry.player_name, 64);
              } else if (img.src !== FALLBACK_AVATAR) {
                img.src = FALLBACK_AVATAR;
              }
            }}
          />
          <div className="min-w-0">
            <div
              className={`truncate text-xl font-black tracking-tight md:text-2xl ${entry.glow ? "owner-name" : ""}`}
            >
              {entry.glow && <span className="mr-1">♛</span>}
              {entry.player_name}
            </div>
            <div className="text-xs md:text-sm text-muted-foreground truncate">
              <span className="text-primary">◆</span>{" "}
              <span
                className="ft-title-glow"
                style={{ ["--title-color" as any]: titleColor } as any}
              >
                {title}
              </span>{" "}
              <span className="text-muted-foreground/80">({points} pts)</span>
            </div>
          </div>
        </div>
      </div>

      <div className="hidden md:flex items-center justify-center">
        <span
          className="rounded-md px-2.5 py-1 text-xs font-bold tracking-wider"
          style={{ backgroundColor: regionColor, color: "oklch(0.97 0.01 60)" }}
        >
          {entry.region}
        </span>
      </div>

      <div className="hidden md:flex items-center justify-end">
        <span
          className="ft-tier-pill rounded-md px-3 py-1.5 text-sm font-black"
          style={
            {
              ["--tier-color" as any]: tierColor,
              backgroundColor: tierColor,
              color: "oklch(0.15 0.02 30)",
            } as any
          }
        >
          {entry.tier}
        </span>
      </div>

      <div className="mt-3 flex items-center justify-between gap-2 pl-[60px] md:hidden">
        <span
          className="rounded-md px-2 py-0.5 text-[10px] font-bold tracking-wider"
          style={{ backgroundColor: regionColor, color: "oklch(0.97 0.01 60)" }}
        >
          {entry.region}
        </span>
        <span
          className="ft-tier-pill rounded-md px-3 py-1 text-sm font-black"
          style={
            {
              ["--tier-color" as any]: tierColor,
              backgroundColor: tierColor,
              color: "oklch(0.15 0.02 30)",
            } as any
          }
        >
          {entry.tier}
        </span>
      </div>
    </div>
  );
}
