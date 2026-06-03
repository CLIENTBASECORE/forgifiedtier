import { createServerFn } from "@tanstack/react-start";
import { z } from "zod";
import { supabaseAdmin } from "@/integrations/supabase/client.server";
import { TIER_POINTS, tiersFor, type GamemodeSlug, type Tier } from "@/lib/gamemodes";

const shiftSchema = z.object({
  mode: z.enum(["classic", "modern"]),
  gamemode: z.string(),
  delta: z.union([z.literal(1), z.literal(-1)]),
});

export const bulkShiftTier = createServerFn({ method: "POST" })
  .inputValidator((data) => shiftSchema.parse(data))
  .handler(async ({ data }) => {
    const tiersArr = [...tiersFor(data.mode)] as Tier[];

    const { data: rows, error } = await supabaseAdmin
      .from("tier_entries")
      .select("id, tier")
      .eq("mode_kind", data.mode)
      .eq("gamemode", data.gamemode as GamemodeSlug)
      .eq("is_manual", true);

    if (error) return { ok: false, updated: 0, error: error.message };

    let updated = 0;
    const errors: string[] = [];
    const step = data.delta === 1 ? -1 : 1;

    await Promise.all(
      (rows ?? []).map(async (r) => {
        const idx = tiersArr.indexOf(r.tier as Tier);
        if (idx < 0) return;

        const newIdx = Math.min(tiersArr.length - 1, Math.max(0, idx + step));
        if (newIdx === idx) return;

        const newTier = tiersArr[newIdx];
        const { error: updateError } = await supabaseAdmin
          .from("tier_entries")
          .update({ tier: newTier, points: TIER_POINTS[newTier] ?? 0, is_manual: true })
          .eq("id", r.id);

        if (updateError) errors.push(updateError.message);
        else updated++;
      }),
    );

    return { ok: errors.length === 0, updated, error: errors[0] ?? null };
  });
