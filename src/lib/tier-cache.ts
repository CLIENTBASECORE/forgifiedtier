import { supabase } from "@/integrations/supabase/client";
import { fetchAllRows } from "@/lib/fetch-all";
import { TIER_POINTS, type GamemodeSlug, type Tier } from "@/lib/gamemodes";

export interface RawEntry {
  id: string;
  player_name: string;
  gamemode: GamemodeSlug;
  tier: Tier;
  position: number;
  region: string;
  points: number;
  title: string | null;
  glow: boolean;
  mode_kind: "classic" | "modern";
  is_manual?: boolean;
}

export interface ModeData {
  entries: RawEntry[];
  totals: Map<string, number>;
  fetchedAt: number;
}

const cache: Partial<Record<"classic" | "modern", ModeData>> = {};
const inflight: Partial<Record<"classic" | "modern", Promise<ModeData>>> = {};
const TTL_MS = 60 * 60_000;

export function getCachedTiers(mode: "classic" | "modern"): ModeData | null {
  const c = cache[mode];
  return c ?? null;
}

export function loadTiers(
  mode: "classic" | "modern",
  { force = false }: { force?: boolean } = {},
): Promise<ModeData> {
  const existing = cache[mode];
  if (!force && existing && Date.now() - existing.fetchedAt < TTL_MS) {
    return Promise.resolve(existing);
  }
  if (inflight[mode]) return inflight[mode]!;

  const p = (async (): Promise<ModeData> => {
    const { data: manualData, error: manualErr } = await fetchAllRows<RawEntry>((from, to) =>
      supabase
        .from("tier_entries")
        .select(
          "id, player_name, gamemode, tier, position, region, points, title, glow, mode_kind, is_manual",
        )
        .eq("mode_kind", mode)
        .eq("is_manual", true)
        .range(from, to),
    );
    if (manualErr) throw new Error(manualErr.message);

    const all: RawEntry[] = manualData ?? [];

    const totals = new Map<string, number>();
    for (const e of all) {
      const k = e.player_name.toLowerCase();
      totals.set(k, (totals.get(k) ?? 0) + (e.points || TIER_POINTS[e.tier]));
    }

    const data: ModeData = { entries: all, totals, fetchedAt: Date.now() };
    cache[mode] = data;
    return data;
  })();

  inflight[mode] = p;
  p.finally(() => {
    delete inflight[mode];
  });
  return p;
}

export function invalidateTiers(mode?: "classic" | "modern") {
  if (mode) {
    delete cache[mode];
  } else {
    delete cache.classic;
    delete cache.modern;
  }
}
