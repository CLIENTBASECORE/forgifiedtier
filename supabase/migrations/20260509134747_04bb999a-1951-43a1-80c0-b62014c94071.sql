-- Storage bucket for TierTagger mod uploads (public read)
insert into storage.buckets (id, name, public)
values ('tiertagger-mods', 'tiertagger-mods', true)
on conflict (id) do update set public = true;

-- Public read for files in this bucket
drop policy if exists "Public can read tiertagger mods" on storage.objects;
create policy "Public can read tiertagger mods"
on storage.objects for select
to public
using (bucket_id = 'tiertagger-mods');

-- Staff can upload / update / delete files
drop policy if exists "Staff can upload tiertagger mods" on storage.objects;
create policy "Staff can upload tiertagger mods"
on storage.objects for insert
to authenticated
with check (bucket_id = 'tiertagger-mods' and public.is_staff(auth.uid()));

drop policy if exists "Staff can update tiertagger mods" on storage.objects;
create policy "Staff can update tiertagger mods"
on storage.objects for update
to authenticated
using (bucket_id = 'tiertagger-mods' and public.is_staff(auth.uid()))
with check (bucket_id = 'tiertagger-mods' and public.is_staff(auth.uid()));

drop policy if exists "Staff can delete tiertagger mods" on storage.objects;
create policy "Staff can delete tiertagger mods"
on storage.objects for delete
to authenticated
using (bucket_id = 'tiertagger-mods' and public.is_staff(auth.uid()));

-- Track current mod file per mode (classic / modern)
create table if not exists public.tiertagger_mods (
  mode_kind text primary key check (mode_kind in ('classic','modern')),
  file_path text not null,
  file_name text not null,
  size_bytes bigint not null default 0,
  updated_at timestamptz not null default now(),
  updated_by uuid
);

alter table public.tiertagger_mods enable row level security;

drop policy if exists "Anyone can view tiertagger mods" on public.tiertagger_mods;
create policy "Anyone can view tiertagger mods"
on public.tiertagger_mods for select
to public
using (true);

drop policy if exists "Staff can insert tiertagger mods" on public.tiertagger_mods;
create policy "Staff can insert tiertagger mods"
on public.tiertagger_mods for insert
to authenticated
with check (public.is_staff(auth.uid()));

drop policy if exists "Staff can update tiertagger mods" on public.tiertagger_mods;
create policy "Staff can update tiertagger mods"
on public.tiertagger_mods for update
to authenticated
using (public.is_staff(auth.uid()))
with check (public.is_staff(auth.uid()));

drop policy if exists "Staff can delete tiertagger mods" on public.tiertagger_mods;
create policy "Staff can delete tiertagger mods"
on public.tiertagger_mods for delete
to authenticated
using (public.is_staff(auth.uid()));