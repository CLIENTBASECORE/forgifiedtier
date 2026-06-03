import { Link } from "@tanstack/react-router";
import { useAuth } from "@/hooks/useAuth";
import { supabase } from "@/integrations/supabase/client";
import wordmark from "@/assets/forgifiedtiers-wordmark.png";

export function SiteHeader() {
  const { session, hasAnyRole } = useAuth();
  return (
    <header className="sticky top-0 z-40 border-b border-border/60 bg-background/80 backdrop-blur-md">
      <div className="mx-auto flex max-w-7xl items-center justify-between gap-4 px-4 py-3">
        <Link to="/" className="flex items-center shrink-0">
          <img
            src={wordmark}
            alt="Forgified Tiers"
            className="h-8 md:h-10 w-auto object-contain"
          />
        </Link>
        <div className="flex items-center gap-2">
          {hasAnyRole && (
            <Link
              to="/admin"
              className="ft-3d ft-bubble rounded-md border border-border bg-card/60 px-3 py-1.5 text-xs font-medium text-muted-foreground shadow-[0_16px_30px_-26px_oklch(0_0_0_/_0.9)] hover:text-foreground hover:border-primary/50 transition-colors"
            >
              Dashboard
            </Link>
          )}
          {session ? (
            <>
              <span className="hidden sm:inline text-xs text-muted-foreground max-w-[160px] truncate">
                {session.user.email}
              </span>
              <button
                onClick={() => supabase.auth.signOut()}
                className="ft-3d ft-bubble rounded-md border border-border bg-card/60 px-3 py-1.5 text-xs font-medium text-muted-foreground shadow-[0_16px_30px_-26px_oklch(0_0_0_/_0.9)] hover:text-foreground hover:border-primary/50 transition-colors"
              >
                Sign out
              </button>
            </>
          ) : (
            <Link
              to="/auth"
              className="ft-3d ft-bubble rounded-md border border-border bg-card/60 px-3 py-1.5 text-xs font-medium text-muted-foreground shadow-[0_16px_30px_-26px_oklch(0_0_0_/_0.9)] hover:text-foreground hover:border-primary/50 transition-colors"
            >
              Sign in
            </Link>
          )}
        </div>
      </div>
    </header>
  );
}
