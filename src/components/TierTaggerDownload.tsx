import { useEffect, useRef, useState } from "react";
import { ChevronDown, Download } from "lucide-react";
import { supabase } from "@/integrations/supabase/client";
import type { PvpMode } from "@/hooks/usePvpMode";

interface ModFile {
  path: string;
  name: string;
  versionLabel: string;
  sizeBytes: number;
}

function formatSize(bytes: number) {
  if (!bytes) return "";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

function versionLabelFromStorageName(name: string) {
  const withoutPrefix = name.replace(/^\d+-[0-9a-f-]+-/i, "");
  const [storedLabel] = withoutPrefix.split("--");
  // Only strip a *real* file extension (e.g. ".jar"), not the last part of a dotted version like "1.21.11".
  const withoutExtension = storedLabel.replace(/\.[a-z][a-z0-9]{0,4}$/i, "");
  const label = withoutExtension.replace(/[_-]+/g, " ").trim() || withoutPrefix;
  const words = label.split(/\s+/);
  const half = words.length / 2;
  if (Number.isInteger(half) && words.slice(0, half).join(" ") === words.slice(half).join(" ")) {
    return words.slice(0, half).join(" ");
  }
  return label;
}

export function TierTaggerDownload({ mode }: { mode: PvpMode }) {
  const [files, setFiles] = useState<ModFile[]>([]);
  const [selectedPath, setSelectedPath] = useState("");
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setFiles([]);
    setSelectedPath("");

    supabase.storage
      .from("tiertagger-mods")
      .list(mode, {
        limit: 100,
        sortBy: { column: "updated_at", order: "desc" },
      })
      .then(({ data }) => {
        if (!active) return;
        const nextFiles =
          data
            ?.filter((file) => file.name && file.name !== ".emptyFolderPlaceholder")
            .map((file) => ({
              path: `${mode}/${file.name}`,
              name: file.name.replace(/^\d+-[0-9a-f-]+-/i, ""),
              versionLabel: versionLabelFromStorageName(file.name),
              sizeBytes: Number(file.metadata?.size ?? 0),
            })) ?? [];
        setFiles(nextFiles);
        setSelectedPath(nextFiles[0]?.path ?? "");
        setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [mode]);

  useEffect(() => {
    const onPointerDown = (event: PointerEvent) => {
      if (!menuRef.current?.contains(event.target as Node)) setOpen(false);
    };
    window.addEventListener("pointerdown", onPointerDown);
    return () => window.removeEventListener("pointerdown", onPointerDown);
  }, []);

  const file = files.find((f) => f.path === selectedPath) ?? files[0] ?? null;
  const url = file
    ? supabase.storage.from("tiertagger-mods").getPublicUrl(file.path).data.publicUrl
    : null;

  const label = "Download TierTagger Mod";
  const subtitle = mode === "modern" ? "Modern PvP" : "Classic PvP (1.8.9)";

  if (loading || !file || !url) {
    return (
      <button
        type="button"
        disabled
        className="inline-flex items-center gap-1.5 rounded-md border border-border/70 bg-card/45 px-2.5 py-1.5 text-[11px] font-bold text-muted-foreground cursor-not-allowed shadow-[0_14px_24px_-24px_oklch(0_0_0_/_0.9)]"
        title="No mod uploaded yet"
      >
        <Download className="h-3.5 w-3.5" />
        <span className="flex flex-col items-start leading-tight">
          <span>{label}</span>
          <span className="text-[10px] font-normal text-muted-foreground/70">
            {loading ? "Loading..." : `${subtitle} - not available`}
          </span>
        </span>
      </button>
    );
  }

  if (files.length > 1) {
    return (
      <div
        ref={menuRef}
        className="ft-3d ft-bubble relative z-40 inline-flex items-stretch overflow-visible rounded-md border border-primary/30 bg-card/60 text-[10px] font-bold text-foreground shadow-[0_0_10px_-9px_var(--primary)]"
      >
        <button
          type="button"
          onClick={() => setOpen((v) => !v)}
          className="inline-flex max-w-[132px] items-center gap-1.5 px-2 py-1.5 text-left transition-colors duration-700 ease-[cubic-bezier(0.16,1,0.3,1)] hover:bg-primary/10"
          aria-haspopup="listbox"
          aria-expanded={open}
        >
          <span className="truncate">{file.versionLabel}</span>
          <ChevronDown className={`h-3 w-3 shrink-0 text-primary transition-transform duration-700 ease-[cubic-bezier(0.16,1,0.3,1)] ${open ? "rotate-180" : ""}`} />
        </button>
        {open && (
          <div
            role="listbox"
            className="compact-version-menu absolute left-0 top-[calc(100%+5px)] z-[100] max-h-40 w-52 overflow-y-auto rounded-md border border-primary/25 bg-[oklch(0.075_0.01_35_/_0.98)] p-1 shadow-[0_18px_44px_-18px_oklch(0_0_0_/_0.96)] backdrop-blur"
          >
            {files.map((f) => (
              <button
                key={f.path}
                type="button"
                role="option"
                aria-selected={f.path === file.path}
                onClick={() => {
                  setSelectedPath(f.path);
                  setOpen(false);
                }}
                className={`block w-full truncate rounded px-2 py-1.5 text-left text-[10px] transition-colors duration-700 ease-[cubic-bezier(0.16,1,0.3,1)] ${
                  f.path === file.path
                    ? "bg-primary/16 text-foreground"
                    : "text-muted-foreground hover:bg-primary/10 hover:text-foreground"
                }`}
              >
                {f.versionLabel}
              </button>
            ))}
          </div>
        )}
        <a
          href={url}
          download={file.name}
          className="group inline-flex items-center gap-1.5 border-l border-primary/20 px-2 py-1.5 transition-colors duration-700 ease-[cubic-bezier(0.16,1,0.3,1)] hover:bg-primary/10"
        >
          <Download className="h-3 w-3 text-primary transition-transform duration-700 ease-[cubic-bezier(0.16,1,0.3,1)] group-hover:translate-y-0.5" />
          <span className="flex flex-col items-start leading-tight">
            <span>Download</span>
            <span className="text-[9px] font-normal text-muted-foreground">
              {subtitle} - {formatSize(file.sizeBytes)}
            </span>
          </span>
        </a>
      </div>
    );
  }

  return (
    <a
      href={url}
      download={file.name}
      className="ft-3d ft-bubble group inline-flex items-center gap-1.5 rounded-md border border-primary/30 bg-card/60 px-2 py-1.5 text-[10px] font-bold text-foreground shadow-[0_0_10px_-9px_var(--primary)] transition-colors duration-700 ease-[cubic-bezier(0.16,1,0.3,1)] hover:bg-primary/10"
    >
      <Download className="h-3 w-3 text-primary transition-transform duration-700 ease-[cubic-bezier(0.16,1,0.3,1)] group-hover:translate-y-0.5" />
      <span className="flex flex-col items-start leading-tight">
        <span>{label}</span>
        <span className="text-[9px] font-normal text-muted-foreground">
          {subtitle} - {file.name}
          {file.sizeBytes ? ` - ${formatSize(file.sizeBytes)}` : ""}
        </span>
      </span>
    </a>
  );
}
