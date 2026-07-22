import { useEffect, useState } from "react";
import { Newspaper, History, Plus, Trash2, AlertTriangle } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { supabase } from "@/integrations/supabase/client";
import { useAuth } from "@/hooks/useAuth";
import { TIER_COLOR, type Tier, type GamemodeSlug } from "@/lib/gamemodes";

interface NewsPost {
  id: string;
  title: string;
  body: string;
  created_at: string;
  is_important: boolean;
}

interface HistoryEntry {
  id: string;
  player_name: string;
  gamemode: GamemodeSlug;
  old_tier: Tier | null;
  new_tier: Tier | null;
  change_type: string;
  changed_at: string;
}

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function NewsHistoryDialog({ open, onOpenChange }: Props) {
  const { session, isStaff } = useAuth();
  const [news, setNews] = useState<NewsPost[] | null>(null);
  const [history, setHistory] = useState<HistoryEntry[] | null>(null);
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [important, setImportant] = useState(false);
  const [posting, setPosting] = useState(false);

  useEffect(() => {
    if (!open) return;
    supabase
      .from("news_posts")
      .select("*")
      .order("created_at", { ascending: false })
      .limit(100)
      .then(({ data }) => setNews((data ?? []) as NewsPost[]));
    if (session) {
      supabase
        .from("tier_history")
        .select("*")
        .order("changed_at", { ascending: false })
        .limit(100)
        .then(({ data }) => setHistory((data ?? []) as HistoryEntry[]));
    }
  }, [open, session]);

  const submitNews = async () => {
    if (!title.trim() || !body.trim()) return;
    setPosting(true);
    const { data, error } = await supabase
      .from("news_posts")
      .insert({
        title: title.trim(),
        body: body.trim(),
        is_important: important,
        created_by: session?.user.id,
      })
      .select()
      .single();
    setPosting(false);
    if (!error && data) {
      setNews((n) => [data as NewsPost, ...(n ?? [])]);
      setTitle("");
      setBody("");
      setImportant(false);
    }
  };

  const deleteNews = async (id: string) => {
    await supabase.from("news_posts").delete().eq("id", id);
    setNews((n) => (n ?? []).filter((p) => p.id !== id));
  };

  const deleteHistory = async (id: string) => {
    await supabase.from("tier_history").delete().eq("id", id);
    setHistory((h) => (h ?? []).filter((e) => e.id !== id));
  };

  const regularNews = (news ?? []).filter((p) => !p.is_important);
  const importantNews = (news ?? []).filter((p) => p.is_important);

  const renderNewsList = (list: NewsPost[], emptyText: string, important: boolean) => {
    if (news === null) return <div className="text-sm text-muted-foreground">Loading…</div>;
    if (list.length === 0)
      return (
        <div className="text-sm text-muted-foreground py-8 text-center">{emptyText}</div>
      );
    return list.map((p) => (
      <div
        key={p.id}
        className={`rounded-lg border p-3 ${
          important
            ? "border-destructive/60 bg-destructive/10"
            : "border-border/60 bg-card/40"
        }`}
      >
        <div className="flex items-start justify-between gap-2">
          <div className="font-semibold flex items-center gap-1.5">
            {important && <AlertTriangle className="h-4 w-4 text-destructive" />}
            {p.title}
          </div>
          {isStaff && (
            <button
              onClick={() => deleteNews(p.id)}
              className="text-muted-foreground hover:text-destructive"
              aria-label="Delete"
            >
              <Trash2 className="h-4 w-4" />
            </button>
          )}
        </div>
        <div className="mt-1 whitespace-pre-wrap text-sm text-muted-foreground">
          {p.body}
        </div>
        <div className="mt-2 text-[10px] uppercase tracking-wider text-muted-foreground/70">
          {new Date(p.created_at).toLocaleString()}
        </div>
      </div>
    ));
  };

  const composer = (asImportant: boolean) =>
    isStaff && (
      <div
        className={`rounded-lg border p-3 space-y-2 ${
          asImportant
            ? "border-destructive/50 bg-destructive/5"
            : "border-border bg-card/50"
        }`}
      >
        <Input
          placeholder="Title"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
        />
        <Textarea
          placeholder={asImportant ? "Important message…" : "What's new?"}
          value={body}
          onChange={(e) => setBody(e.target.value)}
          rows={3}
        />
        <Button
          onClick={() => {
            setImportant(asImportant);
            submitNews();
          }}
          disabled={posting || !title.trim() || !body.trim()}
          size="sm"
          variant={asImportant ? "destructive" : "default"}
          className="gap-1"
        >
          <Plus className="h-4 w-4" />
          {asImportant ? "Post important message" : "Post news"}
        </Button>
      </div>
    );

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl max-h-[85vh] flex flex-col">
        <DialogHeader>
          <DialogTitle>News & Tier History</DialogTitle>
        </DialogHeader>
        <Tabs defaultValue="news" className="flex-1 flex flex-col min-h-0">
          <TabsList className="grid grid-cols-3 w-full">
            <TabsTrigger value="news" className="gap-2">
              <Newspaper className="h-4 w-4" />
              News
            </TabsTrigger>
            <TabsTrigger value="important" className="gap-2">
              <AlertTriangle className="h-4 w-4" />
              Important
            </TabsTrigger>
            <TabsTrigger value="history" className="gap-2">
              <History className="h-4 w-4" />
              History
            </TabsTrigger>
          </TabsList>

          <TabsContent value="news" className="flex-1 overflow-y-auto space-y-3 mt-3 pr-1">
            {composer(false)}
            {renderNewsList(regularNews, "No news yet.", false)}
          </TabsContent>

          <TabsContent
            value="important"
            className="flex-1 overflow-y-auto space-y-3 mt-3 pr-1"
          >
            {composer(true)}
            {renderNewsList(importantNews, "No important messages.", true)}
          </TabsContent>

          <TabsContent value="history" className="flex-1 overflow-y-auto mt-3 pr-1">
            {!session ? (
              <div className="text-sm text-muted-foreground py-8 text-center">
                Sign in to view tier change history.
              </div>
            ) : history === null ? (
              <div className="text-sm text-muted-foreground">Loading…</div>
            ) : history.length === 0 ? (
              <div className="text-sm text-muted-foreground py-8 text-center">
                No tier changes recorded yet.
              </div>
            ) : (
              <ul className="divide-y divide-border/40">
                {history.map((h) => (
                  <li key={h.id} className="py-2 flex items-center gap-3 text-sm">
                    <div className="flex-1 min-w-0">
                      <div className="font-medium truncate">{h.player_name}</div>
                      <div className="text-[11px] text-muted-foreground capitalize">
                        {h.gamemode} · {h.change_type}
                      </div>
                    </div>
                    <div className="flex items-center gap-1.5 text-xs">
                      {h.old_tier && (
                        <span
                          className="ft-tier-pill rounded px-1.5 py-0.5 font-bold"
                          style={{
                            ["--tier-color" as any]: TIER_COLOR[h.old_tier],
                            backgroundColor: TIER_COLOR[h.old_tier],
                            color: "oklch(0.15 0.02 30)",
                          }}
                        >
                          {h.old_tier}
                        </span>
                      )}
                      <span className="text-muted-foreground">→</span>
                      {h.new_tier ? (
                        <span
                          className="ft-tier-pill rounded px-1.5 py-0.5 font-bold"
                          style={{
                            ["--tier-color" as any]: TIER_COLOR[h.new_tier],
                            backgroundColor: TIER_COLOR[h.new_tier],
                            color: "oklch(0.15 0.02 30)",
                          }}
                        >
                          {h.new_tier}
                        </span>
                      ) : (
                        <span className="text-destructive text-xs">removed</span>
                      )}
                    </div>
                    <div className="text-[10px] text-muted-foreground/70 whitespace-nowrap">
                      {new Date(h.changed_at).toLocaleDateString()}
                    </div>
                    {isStaff && (
                      <button
                        onClick={() => deleteHistory(h.id)}
                        className="text-muted-foreground hover:text-destructive"
                        aria-label="Delete"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </TabsContent>
        </Tabs>
      </DialogContent>
    </Dialog>
  );
}
