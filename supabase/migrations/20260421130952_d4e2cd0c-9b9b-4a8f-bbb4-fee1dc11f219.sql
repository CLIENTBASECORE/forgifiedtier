CREATE TYPE public.tier_rank AS ENUM ('S', 'A', 'B', 'C', 'D');
CREATE TYPE public.gamemode AS ENUM ('boxing', 'bedfight', 'fireballfight', 'sumo', 'nodebuff', 'uhc');

CREATE TABLE public.tier_entries (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  player_name text NOT NULL,
  gamemode public.gamemode NOT NULL,
  tier public.tier_rank NOT NULL,
  position int NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE public.tier_entries ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Anyone can view tier entries"
  ON public.tier_entries FOR SELECT
  USING (true);

CREATE INDEX idx_tier_entries_gamemode ON public.tier_entries(gamemode, tier, position);