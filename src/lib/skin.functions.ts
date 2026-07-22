import { createServerFn } from "@tanstack/react-start";
import { z } from "zod";

const uuidSchema = z.object({
  name: z.string(),
});

const uuidsSchema = z.object({
  names: z.array(z.string()).max(100),
});

type UuidResponse = { uuid: string | null };
type UuidsResponse = { uuids: Record<string, string | null> };

const cache = new Map<string, { uuid: string | null; ts: number }>();
const CACHE_TTL_MS = 1000 * 60 * 60; // 1 hour

async function fetchUuidForName(name: string): Promise<string | null> {
  const clean = name.trim().replace(/[^A-Za-z0-9_]/g, "");
  if (!clean) return null;

  const key = clean.toLowerCase();
  const cached = cache.get(key);
  if (cached && Date.now() - cached.ts < CACHE_TTL_MS) return cached.uuid;

  try {
    const res = await fetch(
      `https://api.mojang.com/users/profiles/minecraft/${encodeURIComponent(clean)}`,
      {
        headers: { "user-agent": "forgifiedtiers/skin-resolver" },
      },
    );

    if (!res.ok) {
      cache.set(key, { uuid: null, ts: Date.now() });
      return null;
    }

    const json = (await res.json()) as { id?: string };
    const uuid = typeof json.id === "string" && json.id.length >= 32 ? json.id : null;
    cache.set(key, { uuid, ts: Date.now() });
    return uuid;
  } catch {
    cache.set(key, { uuid: null, ts: Date.now() });
    return null;
  }
}

/**
 * Resolve a Minecraft Java username to a UUID (undashed).
 * We use UUIDs for renders because username → skin lookups are commonly rate-limited.
 */
export const resolveMinecraftUuid = createServerFn({ method: "GET" })
  .inputValidator((data) => uuidSchema.parse(data))
  .handler(async ({ data }): Promise<UuidResponse> => {
    return { uuid: await fetchUuidForName(data.name) };
  });

/**
 * Resolve multiple usernames to UUIDs in one request (keys are lowercase names).
 * Keep this limited to avoid hammering Mojang.
 */
export const resolveMinecraftUuids = createServerFn({ method: "POST" })
  .inputValidator((data) => uuidsSchema.parse(data))
  .handler(async ({ data }): Promise<UuidsResponse> => {
    const nameByKey = new Map<string, string>();
    for (const raw of data.names) {
      const n = raw.trim();
      if (!n) continue;
      const key = n.toLowerCase();
      if (!nameByKey.has(key)) nameByKey.set(key, n);
      if (nameByKey.size >= 100) break;
    }
    const keys = Array.from(nameByKey.keys());

    const uuids: Record<string, string | null> = {};

    // Small concurrency to be gentle with Mojang.
    const CONCURRENCY = 4;
    for (let i = 0; i < keys.length; i += CONCURRENCY) {
      const chunk = keys.slice(i, i + CONCURRENCY);
      const results = await Promise.all(
        chunk.map((key) => fetchUuidForName(nameByKey.get(key) ?? key)),
      );
      chunk.forEach((key, idx) => {
        uuids[key] = results[idx] ?? null;
      });
    }

    return { uuids };
  });
