-- Owners can now promote other registered accounts to owner.
-- Non-owner roles are still exclusive, but owner is intentionally preserved.
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
    INSERT INTO public.user_roles (user_id, role)
    VALUES (_user_id, 'owner')
    ON CONFLICT DO NOTHING;
    RETURN;
  END IF;

  DELETE FROM public.user_roles
   WHERE user_id = _user_id AND role <> 'owner';

  INSERT INTO public.user_roles (user_id, role)
  VALUES (_user_id, _role)
  ON CONFLICT DO NOTHING;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.set_user_role(uuid, public.app_role) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.set_user_role(uuid, public.app_role) TO authenticated;
