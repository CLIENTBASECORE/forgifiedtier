-- Function: any authenticated staff (owner/admin/tester) can add tier entries
CREATE OR REPLACE FUNCTION public.can_manage_tiers(_user_id uuid)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.user_roles
    WHERE user_id = _user_id
      AND role IN ('owner','admin','tester')
  )
$$;

REVOKE EXECUTE ON FUNCTION public.can_manage_tiers(uuid) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.can_manage_tiers(uuid) TO authenticated;

-- Replace tier_entries INSERT policy so testers can insert
DROP POLICY IF EXISTS "Staff can insert tier entries" ON public.tier_entries;
CREATE POLICY "Staff and testers can insert tier entries"
  ON public.tier_entries FOR INSERT
  TO authenticated
  WITH CHECK (public.can_manage_tiers(auth.uid()));

-- Owner-only function: list users with email + roles
CREATE OR REPLACE FUNCTION public.list_users_with_roles()
RETURNS TABLE (user_id uuid, email text, roles public.app_role[], created_at timestamptz)
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF NOT public.has_role(auth.uid(), 'owner') THEN
    RAISE EXCEPTION 'Only the owner can list users';
  END IF;

  RETURN QUERY
  SELECT
    u.id AS user_id,
    u.email::text AS email,
    COALESCE(
      (SELECT array_agg(ur.role ORDER BY ur.role) FROM public.user_roles ur WHERE ur.user_id = u.id),
      ARRAY[]::public.app_role[]
    ) AS roles,
    u.created_at
  FROM auth.users u
  ORDER BY u.created_at DESC;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.list_users_with_roles() FROM public, anon;
GRANT EXECUTE ON FUNCTION public.list_users_with_roles() TO authenticated;

-- Owner-only function: set a user's role (replaces any existing non-owner role)
CREATE OR REPLACE FUNCTION public.set_user_role(_user_id uuid, _role public.app_role)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF NOT public.has_role(auth.uid(), 'owner') THEN
    RAISE EXCEPTION 'Only the owner can change roles';
  END IF;

  IF _role = 'owner' THEN
    RAISE EXCEPTION 'Cannot assign owner role';
  END IF;

  -- Remove any non-owner roles for this user, then insert the new one
  DELETE FROM public.user_roles
   WHERE user_id = _user_id AND role <> 'owner';

  INSERT INTO public.user_roles (user_id, role)
  VALUES (_user_id, _role)
  ON CONFLICT DO NOTHING;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.set_user_role(uuid, public.app_role) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.set_user_role(uuid, public.app_role) TO authenticated;

-- Owner-only function: clear all non-owner roles for a user
CREATE OR REPLACE FUNCTION public.clear_user_roles(_user_id uuid)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF NOT public.has_role(auth.uid(), 'owner') THEN
    RAISE EXCEPTION 'Only the owner can change roles';
  END IF;

  DELETE FROM public.user_roles
   WHERE user_id = _user_id AND role <> 'owner';
END;
$$;

REVOKE EXECUTE ON FUNCTION public.clear_user_roles(uuid) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.clear_user_roles(uuid) TO authenticated;