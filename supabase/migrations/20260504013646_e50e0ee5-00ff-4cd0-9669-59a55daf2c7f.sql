
-- Add modern gamemodes
ALTER TYPE public.gamemode ADD VALUE IF NOT EXISTS 'sword';
ALTER TYPE public.gamemode ADD VALUE IF NOT EXISTS 'nethop';
ALTER TYPE public.gamemode ADD VALUE IF NOT EXISTS 'axe';
ALTER TYPE public.gamemode ADD VALUE IF NOT EXISTS 'pot';
ALTER TYPE public.gamemode ADD VALUE IF NOT EXISTS 'vanilla';
ALTER TYPE public.gamemode ADD VALUE IF NOT EXISTS 'smp';
ALTER TYPE public.gamemode ADD VALUE IF NOT EXISTS 'mace';

-- Add modern tier ranks
ALTER TYPE public.tier_rank ADD VALUE IF NOT EXISTS 'HT1';
ALTER TYPE public.tier_rank ADD VALUE IF NOT EXISTS 'LT1';
ALTER TYPE public.tier_rank ADD VALUE IF NOT EXISTS 'HT2';
ALTER TYPE public.tier_rank ADD VALUE IF NOT EXISTS 'LT2';
ALTER TYPE public.tier_rank ADD VALUE IF NOT EXISTS 'HT3';
ALTER TYPE public.tier_rank ADD VALUE IF NOT EXISTS 'LT3';
ALTER TYPE public.tier_rank ADD VALUE IF NOT EXISTS 'HT4';
ALTER TYPE public.tier_rank ADD VALUE IF NOT EXISTS 'LT4';
ALTER TYPE public.tier_rank ADD VALUE IF NOT EXISTS 'HT5';
ALTER TYPE public.tier_rank ADD VALUE IF NOT EXISTS 'LT5';

-- Add mode_kind column to distinguish classic vs modern lists
ALTER TABLE public.tier_entries
  ADD COLUMN IF NOT EXISTS mode_kind text NOT NULL DEFAULT 'classic';

ALTER TABLE public.tier_entries
  DROP CONSTRAINT IF EXISTS tier_entries_mode_kind_check;
ALTER TABLE public.tier_entries
  ADD CONSTRAINT tier_entries_mode_kind_check CHECK (mode_kind IN ('classic','modern'));

ALTER TABLE public.tier_history
  ADD COLUMN IF NOT EXISTS mode_kind text NOT NULL DEFAULT 'classic';

-- Update history trigger to include mode_kind
CREATE OR REPLACE FUNCTION public.log_tier_change()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $function$
BEGIN
  IF TG_OP = 'INSERT' THEN
    INSERT INTO public.tier_history (player_name, gamemode, old_tier, new_tier, change_type, changed_by, mode_kind)
    VALUES (NEW.player_name, NEW.gamemode, NULL, NEW.tier, 'created', auth.uid(), NEW.mode_kind);
    RETURN NEW;
  ELSIF TG_OP = 'UPDATE' THEN
    IF NEW.tier IS DISTINCT FROM OLD.tier THEN
      INSERT INTO public.tier_history (player_name, gamemode, old_tier, new_tier, change_type, changed_by, mode_kind)
      VALUES (NEW.player_name, NEW.gamemode, OLD.tier, NEW.tier, 'updated', auth.uid(), NEW.mode_kind);
    END IF;
    RETURN NEW;
  ELSIF TG_OP = 'DELETE' THEN
    INSERT INTO public.tier_history (player_name, gamemode, old_tier, new_tier, change_type, changed_by, mode_kind)
    VALUES (OLD.player_name, OLD.gamemode, OLD.tier, NULL, 'deleted', auth.uid(), OLD.mode_kind);
    RETURN OLD;
  END IF;
  RETURN NULL;
END;
$function$;
