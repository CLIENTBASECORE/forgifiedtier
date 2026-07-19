-- News posts table
CREATE TABLE public.news_posts (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  title text NOT NULL,
  body text NOT NULL,
  created_by uuid,
  created_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE public.news_posts ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Anyone can view news"
  ON public.news_posts FOR SELECT
  USING (true);

CREATE POLICY "Staff can insert news"
  ON public.news_posts FOR INSERT
  TO authenticated
  WITH CHECK (public.is_staff(auth.uid()));

CREATE POLICY "Staff can update news"
  ON public.news_posts FOR UPDATE
  TO authenticated
  USING (public.is_staff(auth.uid()))
  WITH CHECK (public.is_staff(auth.uid()));

CREATE POLICY "Staff can delete news"
  ON public.news_posts FOR DELETE
  TO authenticated
  USING (public.is_staff(auth.uid()));

-- Tier history table
CREATE TABLE public.tier_history (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  player_name text NOT NULL,
  gamemode public.gamemode NOT NULL,
  old_tier public.tier_rank,
  new_tier public.tier_rank,
  change_type text NOT NULL, -- 'created' | 'updated' | 'deleted'
  changed_by uuid,
  changed_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_tier_history_changed_at ON public.tier_history (changed_at DESC);

ALTER TABLE public.tier_history ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Authenticated users can view tier history"
  ON public.tier_history FOR SELECT
  TO authenticated
  USING (true);

-- Trigger function to log tier changes
CREATE OR REPLACE FUNCTION public.log_tier_change()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF TG_OP = 'INSERT' THEN
    INSERT INTO public.tier_history (player_name, gamemode, old_tier, new_tier, change_type, changed_by)
    VALUES (NEW.player_name, NEW.gamemode, NULL, NEW.tier, 'created', auth.uid());
    RETURN NEW;
  ELSIF TG_OP = 'UPDATE' THEN
    IF NEW.tier IS DISTINCT FROM OLD.tier THEN
      INSERT INTO public.tier_history (player_name, gamemode, old_tier, new_tier, change_type, changed_by)
      VALUES (NEW.player_name, NEW.gamemode, OLD.tier, NEW.tier, 'updated', auth.uid());
    END IF;
    RETURN NEW;
  ELSIF TG_OP = 'DELETE' THEN
    INSERT INTO public.tier_history (player_name, gamemode, old_tier, new_tier, change_type, changed_by)
    VALUES (OLD.player_name, OLD.gamemode, OLD.tier, NULL, 'deleted', auth.uid());
    RETURN OLD;
  END IF;
  RETURN NULL;
END;
$$;

CREATE TRIGGER tier_entries_history
AFTER INSERT OR UPDATE OR DELETE ON public.tier_entries
FOR EACH ROW EXECUTE FUNCTION public.log_tier_change();