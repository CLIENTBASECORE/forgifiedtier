
ALTER TABLE public.tier_entries
  ADD COLUMN IF NOT EXISTS is_manual boolean NOT NULL DEFAULT true;

CREATE INDEX IF NOT EXISTS idx_tier_entries_is_manual ON public.tier_entries (mode_kind, is_manual);
