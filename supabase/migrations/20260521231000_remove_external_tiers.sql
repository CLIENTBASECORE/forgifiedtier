DELETE FROM public.tier_entries
WHERE is_manual = false;

INSERT INTO public.app_settings (key, value)
VALUES ('modern_source', 'off')
ON CONFLICT (key) DO UPDATE
SET value = excluded.value,
    updated_at = now();
