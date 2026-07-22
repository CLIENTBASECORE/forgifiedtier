import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState, type FormEvent } from "react";
import { useServerFn } from "@tanstack/react-start";
import { SiteHeader } from "@/components/SiteHeader";
import { SiteFooter } from "@/components/SiteFooter";
import { useAuth } from "@/hooks/useAuth";
import { supabase } from "@/integrations/supabase/client";
import { bulkShiftTier } from "@/lib/tier-sync.functions";
import { invalidateTiers, loadTiers } from "@/lib/tier-cache";
import {
  GAMEMODES,
  MODERN_GAMEMODES,
  REGIONS,
  CLASSIC_TIERS,
  MODERN_TIERS,
  TIER_COLOR,
  TIER_POINTS,
  avatarUrl,
  type GamemodeSlug,
  type Region,
  type Tier,
} from "@/lib/gamemodes";

type ModeKind = "classic" | "modern";

export const Route = createFileRoute("/admin")({
  head: () => ({
    meta: [
      { title: "Dashboard — Forgified Tiers" },
      { name: "description", content: "Forgified Tiers dashboard" },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AdminPage,
});

function AdminPage() {
  const auth = useAuth();
  return (
    <div className="min-h-screen bg-background text-foreground">
      <SiteHeader />
      <main className="mx-auto max-w-5xl px-4 py-10">
        {auth.loading ? (
          <div className="h-40 animate-pulse rounded-xl bg-card/40" />
        ) : !auth.session ? (
          <AuthForm />
        ) : !auth.hasAnyRole ? (
          <NoAccess email={auth.session.user.email ?? ""} />
        ) : (
          <Dashboard isOwner={auth.isOwner} canManageTiers={auth.isStaff} />
        )}
      </main>
      <SiteFooter />
    </div>
  );
}

function AuthForm() {
  const [mode, setMode] = useState<"signin" | "signup">("signin");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setErr(null);
    setBusy(true);
    try {
      if (mode === "signup") {
        const { error } = await supabase.auth.signUp({
          email,
          password,
          options: { emailRedirectTo: `${window.location.origin}/admin` },
        });
        if (error) throw error;
      } else {
        const { error } = await supabase.auth.signInWithPassword({ email, password });
        if (error) throw error;
      }
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Unknown error");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="ft-surface mx-auto max-w-md rounded-2xl p-8 shadow-[var(--shadow-tier)]">
      <h1 className="text-2xl font-black tracking-tight">
        {mode === "signup" ? "Create account" : "Sign in"}
      </h1>
      <p className="mt-1 text-sm text-muted-foreground">
        {mode === "signup"
          ? "Create an account to access the dashboard."
          : "Sign in to your account."}
      </p>
      <form onSubmit={submit} className="mt-6 space-y-4">
        <div>
          <label className="text-xs uppercase tracking-wider text-muted-foreground">Email</label>
          <input
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="ft-input mt-1 w-full rounded-md px-3 py-2 text-sm focus:border-primary focus:outline-none"
          />
        </div>
        <div>
          <label className="text-xs uppercase tracking-wider text-muted-foreground">Password</label>
          <input
            type="password"
            required
            minLength={6}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="ft-input mt-1 w-full rounded-md px-3 py-2 text-sm focus:border-primary focus:outline-none"
          />
        </div>
        {err && (
          <div className="rounded-md border border-destructive/50 bg-destructive/10 px-3 py-2 text-sm">
            {err}
          </div>
        )}
        <button
          type="submit"
          disabled={busy}
          className="ft-3d w-full rounded-md bg-primary px-4 py-2.5 font-bold text-primary-foreground shadow-[0_18px_34px_-26px_var(--primary)] hover:opacity-95 disabled:opacity-50"
        >
          {busy ? "..." : mode === "signup" ? "Create account" : "Sign in"}
        </button>
      </form>
      <button
        onClick={() => setMode(mode === "signup" ? "signin" : "signup")}
        className="mt-4 w-full text-sm text-muted-foreground hover:text-foreground"
      >
        {mode === "signup"
          ? "Already have an account? Sign in"
          : "First time here? Create an account"}
      </button>
    </div>
  );
}

function NoAccess({ email }: { email: string }) {
  const [ownerExists, setOwnerExists] = useState<boolean | null>(null);
  const [claiming, setClaiming] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  useEffect(() => {
    supabase
      .from("user_roles")
      .select("id", { count: "exact", head: true })
      .eq("role", "owner")
      .then(({ count }) => setOwnerExists((count ?? 0) > 0));
  }, []);

  const claim = async () => {
    setClaiming(true);
    setErr(null);
    const { data, error } = await supabase.rpc("claim_ownership");
    setClaiming(false);
    if (error) setErr(error.message);
    else if (data === true) window.location.reload();
    else setErr("Ownership has already been claimed.");
  };

  return (
    <div className="ft-surface mx-auto max-w-md rounded-2xl p-8 text-center">
      <h1 className="text-2xl font-bold">No dashboard access</h1>
      <p className="mt-2 text-sm text-muted-foreground">
        Signed in as <span className="text-foreground">{email}</span>. This account doesn't have a
        role yet (owner, admin or tester).
      </p>

      {ownerExists === false && (
        <div className="mt-6 rounded-lg border border-primary/40 bg-primary/10 p-4 text-left">
          <div className="text-sm font-bold text-foreground">No owner exists yet</div>
          <p className="mt-1 text-xs text-muted-foreground">
            You can claim ownership of this tier list. Once claimed, this option permanently
            disappears.
          </p>
          <button
            onClick={claim}
            disabled={claiming}
            className="ft-3d mt-3 w-full rounded-md bg-primary px-4 py-2 text-sm font-bold text-primary-foreground shadow-[0_18px_34px_-26px_var(--primary)] hover:opacity-95 disabled:opacity-50"
          >
            {claiming ? "Claiming..." : "Claim ownership"}
          </button>
          {err && <div className="mt-2 text-xs text-destructive">{err}</div>}
        </div>
      )}

      {ownerExists === true && (
        <p className="mt-4 text-xs text-muted-foreground">
          Ask the owner to give you a role from the dashboard.
        </p>
      )}

      <button
        onClick={() => supabase.auth.signOut()}
        className="ft-3d mt-6 rounded-md border border-border bg-card/25 px-4 py-2 text-sm hover:bg-accent"
      >
        Sign out
      </button>
    </div>
  );
}

interface TierEntry {
  id: string;
  player_name: string;
  gamemode: GamemodeSlug;
  tier: Tier;
  region: string;
  points: number;
  glow: boolean;
  mode_kind: ModeKind;
  is_manual: boolean;
}

function Dashboard({ isOwner, canManageTiers }: { isOwner: boolean; canManageTiers: boolean }) {
  const [mode, setMode] = useState<ModeKind>("classic");
  const [entries, setEntries] = useState<TierEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<GamemodeSlug | "all">("all");
  const [search, setSearch] = useState("");
  const [msg, setMsg] = useState<string | null>(null);

  const gamemodes = mode === "modern" ? MODERN_GAMEMODES : GAMEMODES;

  const load = async (force = false) => {
    setLoading(true);
    try {
      const data = await loadTiers(mode, { force });
      setEntries(
        (data.entries as TierEntry[])
          .slice()
          .sort((a, b) =>
            a.gamemode === b.gamemode ? b.points - a.points : a.gamemode.localeCompare(b.gamemode),
          ),
      );
    } catch (e) {
      setMsg(`Error: ${e instanceof Error ? e.message : "failed to load ranked players"}`);
      setEntries([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    setFilter("all");
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mode]);

  const q = search.trim().toLowerCase();
  const filtered = entries.filter(
    (e) =>
      (filter === "all" || e.gamemode === filter) &&
      (q === "" || e.player_name.toLowerCase().includes(q)),
  );

  return (
    <div className="space-y-8">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <div className="text-xs uppercase tracking-[0.2em] text-primary">Dashboard</div>
          <h1 className="text-3xl font-black tracking-tight">Manage Tier Lists</h1>
        </div>
        <button
          onClick={() => supabase.auth.signOut()}
          className="rounded-md border border-border px-3 py-1.5 text-xs hover:bg-accent"
        >
          Sign out
        </button>
      </div>

      {/* Mode switcher */}
      <div className="inline-flex rounded-lg border border-border bg-card/40 p-1">
        {(["classic", "modern"] as ModeKind[]).map((m) => (
          <button
            key={m}
            onClick={() => setMode(m)}
            className={`px-4 py-1.5 text-sm font-bold rounded-md transition-colors ${
              mode === m
                ? "bg-primary text-primary-foreground"
                : "text-muted-foreground hover:text-foreground"
            }`}
          >
            {m === "classic" ? "Classic PvP" : "Modern PvP"}
          </button>
        ))}
      </div>

      {msg && (
        <div className="rounded-md border border-primary/40 bg-primary/10 px-4 py-2 text-sm">
          {msg}
        </div>
      )}

      <AddPlayerForm
        mode={mode}
        onAdded={(name) => {
          invalidateTiers(mode);
          setMsg(`Added ${name}`);
          load(true);
        }}
        onError={(e) => setMsg(`Error: ${e}`)}
      />




      {canManageTiers && (
        <BulkShiftSection
          mode={mode}
          onDone={(m) => {
            invalidateTiers(mode);
            setMsg(m);
            load(true);
          }}
        />
      )}

      <section className="rounded-2xl border border-border/60 bg-card/40 p-5">
        <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
          <h2 className="text-lg font-bold">
            Ranked players ({filtered.length}) — {mode === "modern" ? "Modern" : "Classic"}
          </h2>
          <div className="flex flex-wrap items-center gap-2">
            <input
              type="search"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search player..."
              className="rounded-md border border-border bg-input px-3 py-1.5 text-sm w-48"
            />
            <select
              value={filter}
              onChange={(e) => setFilter(e.target.value as GamemodeSlug | "all")}
              className="rounded-md border border-border bg-input px-3 py-1.5 text-sm"
            >
              <option value="all">All gamemodes</option>
              {gamemodes.map((g) => (
                <option key={g.slug} value={g.slug}>
                  {g.name}
                </option>
              ))}
            </select>
          </div>
        </div>
        {loading ? (
          <div className="h-40 animate-pulse rounded-md bg-card/40" />
        ) : filtered.length === 0 ? (
          <p className="text-sm text-muted-foreground py-6 text-center">No entries.</p>
        ) : (
          <ul className="divide-y divide-border/40">
            {filtered.map((e) => (
              <PlayerEntryRow
                key={e.id}
                entry={e}
                canManageTiers={canManageTiers}
                onChanged={(msg) => {
                  invalidateTiers(mode);
                  setMsg(msg);
                  load(true);
                }}
              />
            ))}
          </ul>
        )}
      </section>

      {canManageTiers && <TierTaggerModUpload mode={mode} />}

      {isOwner && <UserManager />}
    </div>
  );
}

function AddPlayerForm({
  mode,
  onAdded,
  onError,
}: {
  mode: ModeKind;
  onAdded: (name: string) => void;
  onError: (msg: string) => void;
}) {
  const gamemodes = mode === "modern" ? MODERN_GAMEMODES : GAMEMODES;
  const tiers = mode === "modern" ? MODERN_TIERS : CLASSIC_TIERS;
  const defaultGm = gamemodes[0].slug as GamemodeSlug;
  const defaultTier = (mode === "modern" ? "HT2" : "A+") as Tier;
  const [name, setName] = useState("");
  const [gamemode, setGamemode] = useState<GamemodeSlug>(defaultGm);
  const [tier, setTier] = useState<Tier>(defaultTier);
  const [region, setRegion] = useState<Region>("NA");
  const [points, setPoints] = useState<number | "">("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    setGamemode(defaultGm);
    setTier(defaultTier);
  }, [mode]);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    setBusy(true);
    const { error } = await supabase.from("tier_entries").insert({
      player_name: name.trim(),
      gamemode,
      tier,
      region,
      mode_kind: mode,
      is_manual: true,
      points: points === "" ? TIER_POINTS[tier] : Number(points),
    });
    setBusy(false);
    if (error) onError(error.message);
    else {
      onAdded(name.trim());
      setName("");
      setPoints("");
    }
  };

  return (
    <form
      onSubmit={submit}
      className="rounded-2xl border border-border/60 bg-card/40 p-5 space-y-4"
    >
      <h2 className="text-lg font-bold">Add player</h2>
      <div className="grid gap-3 md:grid-cols-[auto_1fr_1fr_1fr_1fr_1fr_auto] items-end">
        <div className="flex justify-center">
          {name.trim() ? (
            <img
              src={avatarUrl(name.trim(), 96)}
              alt=""
              className="h-12 w-12 rounded-md bg-secondary"
            />
          ) : (
            <div className="h-12 w-12 rounded-md bg-secondary/40" />
          )}
        </div>
        <Field label="Minecraft username">
          <input
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
            placeholder="Notch"
            className="w-full rounded-md border border-border bg-input px-3 py-2 text-sm"
          />
        </Field>
        <Field label="Gamemode">
          <select
            value={gamemode}
            onChange={(e) => setGamemode(e.target.value as GamemodeSlug)}
            className="w-full rounded-md border border-border bg-input px-3 py-2 text-sm"
          >
            {gamemodes.map((g) => (
              <option key={g.slug} value={g.slug}>
                {g.name}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Tier">
          <select
            value={tier}
            onChange={(e) => setTier(e.target.value as Tier)}
            className="w-full rounded-md border border-border bg-input px-3 py-2 text-sm font-bold"
            style={{ color: TIER_COLOR[tier] }}
          >
            {tiers.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Region">
          <select
            value={region}
            onChange={(e) => setRegion(e.target.value as Region)}
            className="w-full rounded-md border border-border bg-input px-3 py-2 text-sm"
          >
            {REGIONS.map((r) => (
              <option key={r} value={r}>
                {r}
              </option>
            ))}
          </select>
        </Field>
        <Field label={`Points (auto: ${TIER_POINTS[tier]})`}>
          <input
            type="number"
            min={0}
            value={points}
            onChange={(e) => setPoints(e.target.value === "" ? "" : Number(e.target.value))}
            placeholder={String(TIER_POINTS[tier])}
            className="w-full rounded-md border border-border bg-input px-3 py-2 text-sm"
          />
        </Field>
        <button
          type="submit"
          disabled={busy}
          className="rounded-md bg-primary px-4 py-2 text-sm font-bold text-primary-foreground hover:opacity-90 disabled:opacity-50"
        >
          {busy ? "..." : "Add"}
        </button>
      </div>
    </form>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="block text-[10px] uppercase tracking-wider text-muted-foreground mb-1">
        {label}
      </span>
      {children}
    </label>
  );
}

function PlayerEntryRow({
  entry,
  canManageTiers,
  onChanged,
}: {
  entry: TierEntry;
  canManageTiers: boolean;
  onChanged: (msg: string) => void;
}) {
  const [busy, setBusy] = useState(false);
  const [glowBusy, setGlowBusy] = useState(false);
  const remove = async () => {
    if (!confirm(`Remove ${entry.player_name} from ${entry.gamemode}?`)) return;
    setBusy(true);
    const { error } = await supabase.from("tier_entries").delete().eq("id", entry.id);
    setBusy(false);
    if (error) alert(error.message);
    else onChanged(`Removed ${entry.player_name} from ${entry.gamemode}`);
  };
  const toggleGlow = async () => {
    setGlowBusy(true);
    const next = !entry.glow;
    // Apply to ALL entries for this player so glow shows everywhere.
    const { error } = await supabase
      .from("tier_entries")
      .update({ glow: next, is_manual: true })
      .ilike("player_name", entry.player_name);
    setGlowBusy(false);
    if (error) alert(error.message);
    else onChanged(`${next ? "Enabled" : "Disabled"} glow for ${entry.player_name}`);
  };
  return (
    <li className="flex items-center gap-3 py-2">
      <img
        src={avatarUrl(entry.player_name, 64)}
        alt=""
        className="h-8 w-8 rounded bg-secondary"
        loading="lazy"
      />
      <div className="flex-1 min-w-0">
        <div className={`font-semibold truncate ${entry.glow ? "owner-name" : ""}`}>
          {entry.glow && <span className="mr-1">♛</span>}
          {entry.player_name}
        </div>
        <div className="text-xs text-muted-foreground">
          {entry.gamemode} · {entry.region} · {entry.points} pts
        </div>
      </div>
      <span
        className="ft-tier-pill rounded px-2 py-0.5 text-xs font-black"
        style={
          {
            ["--tier-color" as any]: TIER_COLOR[entry.tier],
            backgroundColor: TIER_COLOR[entry.tier],
            color: "oklch(0.15 0.02 30)",
          } as any
        }
      >
        {entry.tier}
      </span>
      {entry.is_manual ? (
        canManageTiers ? (
          <>
            <button
              onClick={toggleGlow}
              disabled={glowBusy}
              title="Toggle name glow (applies to this player across all gamemodes)"
              className={`rounded-md border px-2.5 py-1 text-xs font-bold transition-colors disabled:opacity-50 ${
                entry.glow
                  ? "border-primary bg-primary/20 text-primary"
                  : "border-border bg-card hover:bg-accent"
              }`}
            >
              ♛ Glow {entry.glow ? "ON" : "OFF"}
            </button>
            <button
              onClick={remove}
              disabled={busy}
              className="rounded-md border border-destructive/50 bg-destructive/10 px-2.5 py-1 text-xs text-destructive-foreground hover:bg-destructive/20 disabled:opacity-50"
            >
              Remove
            </button>
          </>
        ) : (
          <span className="rounded-md border border-border/60 px-2.5 py-1 text-xs text-muted-foreground">
            Manual
          </span>
        )
      ) : (
        <span className="rounded-md border border-border/60 px-2.5 py-1 text-xs text-muted-foreground">
          Manual
        </span>
      )}
    </li>
  );
}

type ManagedRole = "owner" | "admin" | "tester";

interface UserRow {
  user_id: string;
  email: string;
  roles: ("owner" | "admin" | "tester")[];
  created_at: string;
}

function UserManager() {
  const [users, setUsers] = useState<UserRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [pendingId, setPendingId] = useState<string | null>(null);

  const load = async () => {
    setLoading(true);
    setErr(null);
    const { data, error } = await supabase.rpc("list_users_with_roles");
    if (error) setErr(error.message);
    setUsers((data ?? []) as UserRow[]);
    setLoading(false);
  };

  useEffect(() => {
    load();
  }, []);

  const setRole = async (userId: string, role: ManagedRole | "none") => {
    setPendingId(userId);
    const { error } =
      role === "none"
        ? await supabase.rpc("clear_user_roles", { _user_id: userId })
        : await supabase.rpc("set_user_role", { _user_id: userId, _role: role });
    setPendingId(null);
    if (error) alert(error.message);
    else load();
  };

  const q = search.trim().toLowerCase();
  const filtered = users.filter(
    (u) => q === "" || u.email.toLowerCase().includes(q) || u.user_id.toLowerCase().includes(q),
  );

  return (
    <section className="rounded-2xl border border-primary/30 bg-primary/5 p-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-bold">
            User management{" "}
            <span className="text-xs font-normal text-muted-foreground">· owner only</span>
          </h2>
          <p className="mt-1 text-sm text-muted-foreground">
            All registered accounts. Owners have every permission, admins can add/remove/calibrate
            tiers, and testers can only add tiers.
          </p>
        </div>
        <input
          type="search"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search email or ID..."
          className="rounded-md border border-border bg-input px-3 py-1.5 text-sm w-64"
        />
      </div>

      {err && <div className="mt-3 text-xs text-destructive">{err}</div>}

      <div className="mt-4 overflow-x-auto">
        {loading ? (
          <div className="h-32 animate-pulse rounded bg-card/40" />
        ) : filtered.length === 0 ? (
          <p className="py-6 text-center text-sm text-muted-foreground">No users found.</p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-[10px] uppercase tracking-wider text-muted-foreground border-b border-border/40">
                <th className="py-2 pr-3">Email</th>
                <th className="py-2 pr-3">Current role</th>
                <th className="py-2 pr-3">Joined</th>
                <th className="py-2 pr-3">User ID</th>
                <th className="py-2 pr-3 text-right">Set role</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border/40">
              {filtered.map((u) => {
                const isOwnerRow = u.roles.includes("owner");
                const current: "owner" | "admin" | "tester" | "none" = isOwnerRow
                  ? "owner"
                  : u.roles.includes("admin")
                    ? "admin"
                    : u.roles.includes("tester")
                      ? "tester"
                      : "none";
                return (
                  <tr key={u.user_id} className="align-middle">
                    <td className="py-2 pr-3 font-medium">
                      {u.email || <span className="text-muted-foreground">—</span>}
                    </td>
                    <td className="py-2 pr-3">
                      <RoleBadge role={current} />
                    </td>
                    <td className="py-2 pr-3 text-xs text-muted-foreground whitespace-nowrap">
                      {new Date(u.created_at).toLocaleDateString()}
                    </td>
                    <td className="py-2 pr-3 text-[11px] font-mono text-muted-foreground">
                      {u.user_id.slice(0, 8)}…
                    </td>
                    <td className="py-2 pr-3 text-right">
                      {isOwnerRow ? (
                        <span className="text-xs text-muted-foreground">—</span>
                      ) : (
                        <div className="inline-flex gap-1">
                          {(["owner", "admin", "tester", "none"] as const).map((r) => (
                            <button
                              key={r}
                              disabled={pendingId === u.user_id || current === r}
                              onClick={() => setRole(u.user_id, r)}
                              className={`rounded-md border px-2 py-1 text-[11px] font-bold uppercase transition-colors disabled:opacity-50 ${
                                current === r
                                  ? "border-primary bg-primary/20 text-primary"
                                  : "border-border bg-card hover:bg-accent"
                              }`}
                            >
                              {r === "none" ? "Remove" : r}
                            </button>
                          ))}
                        </div>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
    </section>
  );
}

function RoleBadge({ role }: { role: "owner" | "admin" | "tester" | "none" }) {
  const map: Record<typeof role, { label: string; bg: string; fg: string }> = {
    owner: { label: "Owner", bg: "var(--primary)", fg: "var(--primary-foreground)" },
    admin: { label: "Admin", bg: "var(--accent)", fg: "var(--foreground)" },
    tester: {
      label: "Tester",
      bg: "color-mix(in oklab, var(--primary) 20%, transparent)",
      fg: "var(--foreground)",
    },
    none: { label: "None", bg: "transparent", fg: "var(--muted-foreground)" },
  };
  const m = map[role];
  return (
    <span
      className="inline-block rounded px-2 py-0.5 text-[11px] font-bold uppercase"
      style={{
        backgroundColor: m.bg,
        color: m.fg,
        border: role === "none" ? "1px solid var(--border)" : undefined,
      }}
    >
      {m.label}
    </span>
  );
}

function TierTaggerModUpload({ mode }: { mode: ModeKind }) {
  const [mods, setMods] = useState<
    Array<{
      path: string;
      name: string;
      versionLabel: string;
      sizeBytes: number;
      updatedAt: string | null;
    }>
  >([]);
  const [versionLabel, setVersionLabel] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);
  const [err, setErr] = useState<string | null>(null);

  const load = async () => {
    setLoading(true);
    const { data, error } = await supabase.storage.from("tiertagger-mods").list(mode, {
      limit: 100,
      sortBy: { column: "updated_at", order: "desc" },
    });
    if (error) {
      setErr(error.message);
      setMods([]);
    } else {
      setMods(
        (data ?? [])
          .filter((file) => file.name && file.name !== ".emptyFolderPlaceholder")
          .map((file) => ({
            path: `${mode}/${file.name}`,
            name: file.name.replace(/^\d+-[0-9a-f-]+-/i, ""),
            versionLabel: versionFromFile(file.name),
            sizeBytes: Number(file.metadata?.size ?? 0),
            updatedAt: file.updated_at ?? file.created_at ?? null,
          })),
      );
    }
    setLoading(false);
  };

  useEffect(() => {
    setMsg(null);
    setErr(null);
    load();
  }, [mode]);

  const versionFromFile = (fileName: string) => {
    const withoutPrefix = fileName.replace(/^\d+-[0-9a-f-]+-/i, "");
    return withoutPrefix.replace(/\.[^.]+$/, "").replace(/[_-]+/g, " ").trim() || withoutPrefix;
  };

  const upload = async (files: FileList) => {
    const selectedFiles = Array.from(files);
    if (selectedFiles.length === 0) return;
    setBusy(true);
    setErr(null);
    setMsg(null);
    try {
      const uploaded: string[] = [];
      for (const file of selectedFiles) {
        const safe = file.name.replace(/[^A-Za-z0-9._-]/g, "_");
        const label =
          selectedFiles.length === 1 && versionLabel.trim()
            ? versionLabel.trim()
            : versionFromFile(file.name);
        const safeLabel = label.replace(/[^A-Za-z0-9._-]/g, "_");
        const path = `${mode}/${Date.now()}-${crypto.randomUUID()}-${safeLabel}--${safe}`;
        const { error: upErr } = await supabase.storage
          .from("tiertagger-mods")
          .upload(path, file, {
            upsert: false,
            contentType: file.type || "application/octet-stream",
          });
        if (upErr) throw upErr;
        uploaded.push(label);
      }
      setMsg(`Uploaded ${uploaded.length} version${uploaded.length === 1 ? "" : "s"}`);
      setVersionLabel("");
      await load();
    } catch (e) {
      const message = e instanceof Error ? e.message : "Upload failed";
      setErr(message);
    } finally {
      setBusy(false);
    }
  };

  const remove = async (mod: (typeof mods)[number]) => {
    if (!confirm(`Remove TierTagger version "${mod.versionLabel}"?`)) return;
    setBusy(true);
    setErr(null);
    setMsg(null);
    try {
      const { error } = await supabase.storage.from("tiertagger-mods").remove([mod.path]);
      if (error) throw error;
      setMsg(`Removed ${mod.versionLabel}`);
      await load();
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Remove failed");
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="rounded-2xl border border-border/60 bg-card/40 p-5">
      <h2 className="text-lg font-bold mb-1">
        TierTagger Mod - {mode === "modern" ? "Modern" : "Classic"}
      </h2>
      <p className="text-xs text-muted-foreground mb-4">
        Upload one or more downloadable mod versions shown on the{" "}
        {mode === "modern" ? "Modern" : "Classic"} tier list.
      </p>

      {loading ? (
        <div className="h-16 animate-pulse rounded-md bg-card/40" />
      ) : mods.length > 0 ? (
        <div className="mb-3 divide-y divide-border/40 overflow-hidden rounded-lg border border-border bg-background/40 text-sm">
          {mods.map((mod) => {
            const publicUrl = supabase.storage
              .from("tiertagger-mods")
              .getPublicUrl(mod.path).data.publicUrl;
            return (
              <div key={mod.path} className="flex flex-wrap items-center justify-between gap-2 p-3">
                <div className="min-w-0">
                  <div className="font-bold truncate">{mod.versionLabel}</div>
                  <div className="text-xs text-muted-foreground">
                    {mod.name} - {(mod.sizeBytes / 1024).toFixed(1)} KB
                    {mod.updatedAt ? ` - updated ${new Date(mod.updatedAt).toLocaleString()}` : ""}
                  </div>
                </div>
                <div className="flex gap-2">
                  <a
                    href={publicUrl}
                    download={mod.name}
                    className="rounded-md border border-border px-3 py-1.5 text-xs hover:bg-accent"
                  >
                    Download
                  </a>
                  <button
                    onClick={() => remove(mod)}
                    disabled={busy}
                    className="rounded-md border border-destructive/50 px-3 py-1.5 text-xs text-destructive hover:bg-destructive/10 disabled:opacity-50"
                  >
                    Remove
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        <div className="rounded-lg border border-dashed border-border bg-background/40 p-3 mb-3 text-sm text-muted-foreground">
          No mod file uploaded yet for {mode === "modern" ? "Modern" : "Classic"} PvP.
        </div>
      )}

      <div className="flex flex-wrap items-end gap-2">
        <Field label="Version label (single file)">
          <input
            value={versionLabel}
            onChange={(e) => setVersionLabel(e.target.value)}
            placeholder="Auto from filename"
            className="w-48 rounded-md border border-border bg-input px-3 py-2 text-sm"
          />
        </Field>
        <label className="inline-flex items-center gap-2 rounded-md border border-primary/50 bg-primary/15 px-3 py-2 text-sm font-bold cursor-pointer hover:bg-primary/25">
          <input
            type="file"
            multiple
            className="hidden"
            disabled={busy}
            onChange={(e) => {
              const files = e.target.files;
              if (files) upload(files);
              e.target.value = "";
            }}
          />
          {busy ? "Uploading..." : "Upload file(s)"}
        </label>
      </div>

      {msg && <div className="mt-3 text-xs text-primary">{msg}</div>}
      {err && <div className="mt-3 text-xs text-destructive">{err}</div>}
    </section>
  );
}

function BulkShiftSection({ mode, onDone }: { mode: ModeKind; onDone: (msg: string) => void }) {
  const shift = useServerFn(bulkShiftTier);
  const gamemodes = mode === "modern" ? MODERN_GAMEMODES : GAMEMODES;
  const [busyKey, setBusyKey] = useState<string | null>(null);

  const run = async (gm: GamemodeSlug, delta: 1 | -1) => {
    const verb = delta === 1 ? "upgrade (+1 tier)" : "downgrade (-1 tier)";
    if (!confirm(`${verb} every player in ${gm}?`)) return;
    const key = `${gm}:${delta}`;
    setBusyKey(key);
    try {
      const res = await shift({ data: { mode, gamemode: gm, delta } });
      if (res.ok) onDone(`${verb} → updated ${res.updated} players in ${gm}`);
      else onDone(`Error: ${res.error ?? "unknown"}`);
    } catch (e) {
      onDone(`Error: ${e instanceof Error ? e.message : "shift failed"}`);
    } finally {
      setBusyKey(null);
    }
  };

  return (
    <section className="rounded-2xl border border-border/60 bg-card/40 p-5">
      <h2 className="text-lg font-bold mb-1">
        Calibrate tiers — {mode === "modern" ? "Modern" : "Classic"}
      </h2>
      <p className="text-xs text-muted-foreground mb-4">
        Shift every player in a gamemode up or down by exactly one tier. Useful after a meta change
        or recalibration.
      </p>
      <ul className="divide-y divide-border/40">
        {gamemodes.map((g) => {
          const upKey = `${g.slug}:1`;
          const dnKey = `${g.slug}:-1`;
          return (
            <li key={g.slug} className="flex items-center justify-between gap-3 py-2">
              <div className="font-semibold">{g.name}</div>
              <div className="flex gap-2">
                <button
                  onClick={() => run(g.slug as GamemodeSlug, 1)}
                  disabled={busyKey !== null}
                  className="rounded-md border border-primary/50 bg-primary/15 px-3 py-1.5 text-xs font-bold hover:bg-primary/25 disabled:opacity-50"
                  title="Upgrade everyone in this gamemode by one tier"
                >
                  {busyKey === upKey ? "…" : "+1 Tier"}
                </button>
                <button
                  onClick={() => run(g.slug as GamemodeSlug, -1)}
                  disabled={busyKey !== null}
                  className="rounded-md border border-destructive/50 bg-destructive/10 px-3 py-1.5 text-xs font-bold text-destructive hover:bg-destructive/20 disabled:opacity-50"
                  title="Downgrade everyone in this gamemode by one tier"
                >
                  {busyKey === dnKey ? "…" : "-1 Tier"}
                </button>
              </div>
            </li>
          );
        })}
      </ul>
    </section>
  );
}
