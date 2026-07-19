ALTER TABLE public.news_posts ADD COLUMN is_important boolean NOT NULL DEFAULT false;

CREATE POLICY "Staff can delete tier history"
  ON public.tier_history FOR DELETE
  TO authenticated
  USING (public.is_staff(auth.uid()));