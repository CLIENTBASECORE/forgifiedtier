import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useEffect, useState, type FormEvent } from "react";
import { SiteHeader } from "@/components/SiteHeader";
import { SiteFooter } from "@/components/SiteFooter";
import { useAuth } from "@/hooks/useAuth";
import { supabase } from "@/integrations/supabase/client";

export const Route = createFileRoute("/auth")({
  head: () => ({
    meta: [
      { title: "Sign in — Forgified Tiers" },
      { name: "description", content: "Sign in or create an account on Forgified Tiers." },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AuthPage,
});

function AuthPage() {
  const auth = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (!auth.loading && auth.session) {
      navigate({ to: "/" });
    }
  }, [auth.loading, auth.session, navigate]);

  return (
    <div className="min-h-screen bg-background text-foreground">
      <SiteHeader />
      <main className="mx-auto max-w-5xl px-4 py-10">
        <AuthForm />
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
  const [info, setInfo] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setErr(null);
    setInfo(null);
    setBusy(true);
    try {
      if (mode === "signup") {
        const { error } = await supabase.auth.signUp({
          email,
          password,
          options: { emailRedirectTo: `${window.location.origin}/` },
        });
        if (error) throw error;
        setInfo("Check your email to confirm your account.");
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
    <div className="mx-auto max-w-md rounded-2xl border border-border/60 bg-card/60 p-8 shadow-[var(--shadow-tier)]">
      <h1 className="text-2xl font-black tracking-tight">
        {mode === "signup" ? "Create account" : "Sign in"}
      </h1>
      <p className="mt-1 text-sm text-muted-foreground">
        {mode === "signup"
          ? "Create a free account to follow the tier lists."
          : "Welcome back to Forgified Tiers."}
      </p>
      <form onSubmit={submit} className="mt-6 space-y-4">
        <div>
          <label className="text-xs uppercase tracking-wider text-muted-foreground">Email</label>
          <input
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="mt-1 w-full rounded-md border border-border bg-input px-3 py-2 text-sm focus:border-primary focus:outline-none"
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
            className="mt-1 w-full rounded-md border border-border bg-input px-3 py-2 text-sm focus:border-primary focus:outline-none"
          />
        </div>
        {err && (
          <div className="rounded-md border border-destructive/50 bg-destructive/10 px-3 py-2 text-sm">
            {err}
          </div>
        )}
        {info && (
          <div className="rounded-md border border-primary/40 bg-primary/10 px-3 py-2 text-sm">
            {info}
          </div>
        )}
        <button
          type="submit"
          disabled={busy}
          className="w-full rounded-md bg-primary px-4 py-2.5 font-bold text-primary-foreground hover:opacity-90 disabled:opacity-50"
        >
          {busy ? "..." : mode === "signup" ? "Create account" : "Sign in"}
        </button>
      </form>
      <button
        onClick={() => {
          setMode(mode === "signup" ? "signin" : "signup");
          setErr(null);
          setInfo(null);
        }}
        className="mt-4 w-full text-sm text-muted-foreground hover:text-foreground"
      >
        {mode === "signup" ? "Already have an account? Sign in" : "First time here? Create an account"}
      </button>
    </div>
  );
}
