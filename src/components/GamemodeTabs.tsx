import { Link, useNavigate, useLocation } from "@tanstack/react-router";
import { Trophy, Settings, LogIn, LogOut, Sun, Moon, Shield, Newspaper, Layers } from "lucide-react";
import { useEffect, useState } from "react";
import { NewsHistoryDialog } from "@/components/NewsHistoryDialog";
import { gamemodesFor, iconFor } from "@/lib/gamemodes";
import { usePvpMode, type PvpMode } from "@/hooks/usePvpMode";
import wordmark from "@/assets/forgifiedtiers-wordmark.png";
import { useAuth } from "@/hooks/useAuth";
import { supabase } from "@/integrations/supabase/client";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

interface Props {
  active: "overall" | string;
}

function useTheme() {
  const [theme, setTheme] = useState<"dark" | "light">("dark");
  useEffect(() => {
    const stored = (localStorage.getItem("ft-theme") as "dark" | "light" | null) ?? "dark";
    setTheme(stored);
    document.documentElement.classList.toggle("light", stored === "light");
  }, []);
  const toggle = () => {
    const next = theme === "dark" ? "light" : "dark";
    setTheme(next);
    localStorage.setItem("ft-theme", next);
    document.documentElement.classList.toggle("light", next === "light");
  };
  return { theme, toggle };
}

export function GamemodeTabs({ active }: Props) {
  const [newsOpen, setNewsOpen] = useState(false);
  const { session, hasAnyRole } = useAuth();
  const { theme, toggle } = useTheme();
  const [mode, setMode] = usePvpMode();
  const navigate = useNavigate();
  const loc = useLocation();

  const list = gamemodesFor(mode);

  const switchMode = (m: PvpMode) => {
    setMode(m);
    if (loc.pathname.startsWith("/tier/")) navigate({ to: "/" });
  };

  const tabWidth = mode === "modern" ? "w-[88px]" : "w-[104px]";
  const baseTab =
    `ft-3d ft-bubble group shrink-0 flex flex-col items-center justify-center gap-0.5 rounded-md px-2 py-1.5 text-xs font-semibold whitespace-nowrap border ${tabWidth} h-[60px] transition-colors duration-700 ease-[cubic-bezier(0.16,1,0.3,1)] shadow-[0_16px_32px_-26px_oklch(0_0_0_/_0.9)]`;
  const inactive =
    "border-transparent bg-card/20 text-muted-foreground hover:text-foreground hover:bg-accent/40";
  const activeCls =
    "border-primary/50 bg-primary/15 text-foreground shadow-[0_0_22px_-8px_var(--primary)]";

  return (
    <div className="sticky top-0 z-40 border-b border-border/60 bg-background/80 backdrop-blur-md">
      <div className="mx-auto max-w-7xl px-2 md:px-4">
        <div className="flex items-center gap-2 pt-3 pb-2 md:pt-4 md:pb-2">
          <Link to="/" className="shrink-0 flex items-center">
            <img
              src={wordmark}
              alt="Forgified Tiers"
              className="h-10 md:h-12 w-auto object-contain"
            />
          </Link>

          <div className="no-scrollbar flex-1 flex items-center justify-start md:justify-center gap-1.5 overflow-x-auto overflow-y-visible px-2 py-2 md:px-0 [-webkit-overflow-scrolling:touch]">
            <Link
              to="/"
              className={`${baseTab} ${active === "overall" ? activeCls : inactive}`}
            >
              <Trophy
                className="h-5 w-5"
                style={{ color: active === "overall" ? "var(--primary)" : undefined }}
              />
              <span>Overall</span>
            </Link>
            {list.map((g) => (
              <Link
                key={g.slug}
                to="/tier/$gamemode"
                params={{ gamemode: g.slug }}
                className={`${baseTab} ${active === g.slug ? activeCls : inactive}`}
              >
                <img
                  src={iconFor(mode, g.slug)}
                  alt=""
                  aria-hidden
                  className="pixel-icon h-5 w-5 object-contain"
                />
                <span>{g.name}</span>
              </Link>
            ))}
          </div>

          {/* Mode switch button (3 lines = list icon) */}
          <DropdownMenu>
            <DropdownMenuTrigger
              aria-label="Switch tier list"
              className="ft-3d ft-bubble shrink-0 inline-flex h-10 items-center justify-center gap-1.5 rounded-md border border-border bg-card/60 px-2.5 text-xs font-bold text-muted-foreground hover:text-foreground hover:border-primary/50 transition-colors"
            >
              <Layers className="h-4 w-4" />
              <span className="hidden sm:inline uppercase tracking-wider">{mode}</span>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-48">
              <DropdownMenuLabel>Tier list</DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem onSelect={() => switchMode("classic")}>
                <span className={mode === "classic" ? "font-bold text-primary" : ""}>
                  Classic PvP (1.8.9)
                </span>
              </DropdownMenuItem>
              <DropdownMenuItem onSelect={() => switchMode("modern")}>
                <span className={mode === "modern" ? "font-bold text-primary" : ""}>
                  Modern PvP
                </span>
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>

          <DropdownMenu>
            <DropdownMenuTrigger
              aria-label="Settings"
              className="ft-3d ft-bubble shrink-0 inline-flex h-10 w-10 items-center justify-center rounded-md border border-border bg-card/60 text-muted-foreground hover:text-foreground hover:border-primary/50 transition-colors"
            >
              <Settings className="h-5 w-5" />
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-56">
              <DropdownMenuLabel>
                {session ? (
                  <span className="block truncate text-xs font-normal text-muted-foreground">
                    {session.user.email}
                  </span>
                ) : (
                  "Settings"
                )}
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem onSelect={() => setNewsOpen(true)}>
                <Newspaper className="h-4 w-4" />
                <span>News & Tier history</span>
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem onSelect={toggle}>
                {theme === "dark" ? (
                  <>
                    <Sun className="h-4 w-4" />
                    <span>Light theme</span>
                  </>
                ) : (
                  <>
                    <Moon className="h-4 w-4" />
                    <span>Dark theme</span>
                  </>
                )}
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              {hasAnyRole && (
                <DropdownMenuItem asChild>
                  <Link to="/admin">
                    <Shield className="h-4 w-4" />
                    <span>Dashboard</span>
                  </Link>
                </DropdownMenuItem>
              )}
              {session ? (
                <DropdownMenuItem onSelect={() => supabase.auth.signOut()}>
                  <LogOut className="h-4 w-4" />
                  <span>Sign out</span>
                </DropdownMenuItem>
              ) : (
                <DropdownMenuItem asChild>
                  <Link to="/auth">
                    <LogIn className="h-4 w-4" />
                    <span>Sign in</span>
                  </Link>
                </DropdownMenuItem>
              )}
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>
      <NewsHistoryDialog open={newsOpen} onOpenChange={setNewsOpen} />
    </div>
  );
}
