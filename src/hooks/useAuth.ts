import { useEffect, useState } from "react";
import { supabase } from "@/integrations/supabase/client";
import type { Session } from "@supabase/supabase-js";

export type Role = "owner" | "admin" | "tester";

export interface AuthState {
  session: Session | null;
  loading: boolean;
  roles: Role[];
  isOwner: boolean;
  isAdmin: boolean;
  isTester: boolean;
  /** Owner or admin — can edit/remove tiers and access settings. */
  isStaff: boolean;
  /** Any role (owner/admin/tester) — can access admin panel and add tiers. */
  hasAnyRole: boolean;
}

export function useAuth(): AuthState {
  const [session, setSession] = useState<Session | null>(null);
  const [roles, setRoles] = useState<Role[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const { data: sub } = supabase.auth.onAuthStateChange((_event, s) => {
      setSession(s);
    });
    supabase.auth.getSession().then(({ data }) => {
      setSession(data.session);
      setLoading(false);
    });
    return () => sub.subscription.unsubscribe();
  }, []);

  useEffect(() => {
    if (!session?.user) {
      setRoles([]);
      return;
    }
    supabase
      .from("user_roles")
      .select("role")
      .eq("user_id", session.user.id)
      .then(({ data }) => {
        setRoles((data ?? []).map((r) => r.role as Role));
      });
  }, [session?.user?.id]);

  const isOwner = roles.includes("owner");
  const isAdmin = roles.includes("admin");
  const isTester = roles.includes("tester");
  const isStaff = isOwner || isAdmin;
  const hasAnyRole = isStaff || isTester;

  return { session, loading, roles, isOwner, isAdmin, isTester, isStaff, hasAnyRole };
}
