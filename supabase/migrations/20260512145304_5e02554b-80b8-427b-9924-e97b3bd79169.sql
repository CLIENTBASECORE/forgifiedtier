
CREATE OR REPLACE FUNCTION public.tier_entries_replace_old()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  DELETE FROM public.tier_entries
  WHERE id <> NEW.id
    AND mode_kind = NEW.mode_kind
    AND gamemode = NEW.gamemode
    AND lower(player_name) = lower(NEW.player_name);
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_tier_entries_replace_old ON public.tier_entries;
CREATE TRIGGER trg_tier_entries_replace_old
AFTER INSERT ON public.tier_entries
FOR EACH ROW
EXECUTE FUNCTION public.tier_entries_replace_old();
