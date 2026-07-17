-- Drop unused tables
DROP TABLE IF EXISTS public.app_settings CASCADE;
DROP TABLE IF EXISTS public.tiertagger_mods CASCADE;

-- Clear history logs and news posts
TRUNCATE TABLE public.tier_history RESTART IDENTITY CASCADE;
TRUNCATE TABLE public.news_posts RESTART IDENTITY CASCADE;

-- Empty the tiertagger-mods storage bucket metadata
DELETE FROM storage.objects WHERE bucket_id = 'tiertagger-mods';
