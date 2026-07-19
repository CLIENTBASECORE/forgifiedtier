CREATE TABLE public.app_settings (
  key text PRIMARY KEY,
  value jsonb NOT NULL,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid
);

ALTER TABLE public.app_settings ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Anyone can view app settings"
ON public.app_settings FOR SELECT
USING (true);

CREATE POLICY "Staff can insert app settings"
ON public.app_settings FOR INSERT TO authenticated
WITH CHECK (is_staff(auth.uid()));

CREATE POLICY "Staff can update app settings"
ON public.app_settings FOR UPDATE TO authenticated
USING (is_staff(auth.uid()))
WITH CHECK (is_staff(auth.uid()));

INSERT INTO public.app_settings (key, value) VALUES
  ('show_external_modern', 'true'::jsonb),
  ('show_external_classic', 'true'::jsonb);
