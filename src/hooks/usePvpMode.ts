import { useEffect, useState } from "react";

export type PvpMode = "classic" | "modern";

const KEY = "ft-pvp-mode";

function read(): PvpMode {
  if (typeof window === "undefined") return "classic";
  const v = localStorage.getItem(KEY);
  return v === "modern" ? "modern" : "classic";
}

let listeners = new Set<(m: PvpMode) => void>();

export function usePvpMode(): [PvpMode, (m: PvpMode) => void] {
  const [mode, setMode] = useState<PvpMode>(() => read());
  useEffect(() => {
    setMode(read());
    const fn = (m: PvpMode) => setMode(m);
    listeners.add(fn);
    return () => {
      listeners.delete(fn);
    };
  }, []);
  const update = (m: PvpMode) => {
    localStorage.setItem(KEY, m);
    listeners.forEach((l) => l(m));
  };
  return [mode, update];
}
