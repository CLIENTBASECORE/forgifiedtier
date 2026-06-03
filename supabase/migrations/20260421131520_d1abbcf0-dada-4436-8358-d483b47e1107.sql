DELETE FROM public.tier_entries;
ALTER TABLE public.tier_entries DROP COLUMN tier;
DROP TYPE public.tier_rank;
CREATE TYPE public.tier_rank AS ENUM ('S+','S-','A+','A-','B+','B-','C+','C-','D+','D-','F+','F-');
ALTER TABLE public.tier_entries ADD COLUMN tier public.tier_rank NOT NULL;
ALTER TABLE public.tier_entries ADD COLUMN region text NOT NULL DEFAULT 'NA';
ALTER TABLE public.tier_entries ADD COLUMN points integer NOT NULL DEFAULT 0;
ALTER TABLE public.tier_entries ADD COLUMN title text;