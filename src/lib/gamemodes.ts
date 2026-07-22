export const GAMEMODES = [
  {
    slug: "boxing",
    name: "Boxing",
    description: "Pure fist-fight combat. No armor, no blocks — just timing and aim.",
  },
  {
    slug: "bedfight",
    name: "BedFight",
    description: "Defend your bed and destroy theirs. Fast-paced bridging duels.",
  },
  {
    slug: "fireballfight",
    name: "FireballFight",
    description: "Knock opponents off the map with fireballs and momentum.",
  },
  {
    slug: "sumo",
    name: "Sumo",
    description: "Last one standing on the platform wins. Pure knockback control.",
  },
  {
    slug: "nodebuff",
    name: "NoDebuff",
    description: "Pots, gapples, sword. The classic 1.8.9 PvP test.",
  },
  { slug: "uhc", name: "UHC", description: "No regen, full diamond. Patient, calculated combat." },
  {
    slug: "spleef",
    name: "Spleef",
    description: "Break the floor under your opponents. Last one standing wins.",
  },
] as const;

export type ClassicSlug = (typeof GAMEMODES)[number]["slug"];

export const MODERN_GAMEMODES = [
  {
    slug: "vanilla",
    name: "Vanilla",
    description: "Vanilla PvP — end crystal combos and explosions.",
  },
  { slug: "sword", name: "Sword", description: "Modern sword combat with crits and combos." },
  { slug: "uhc", name: "UHC", description: "Modern UHC. Patient, calculated combat." },
  { slug: "pot", name: "Pot", description: "Modern potion PvP with splash and lingering." },
  { slug: "nethop", name: "NethPot", description: "Netherite armor potion combat." },
  { slug: "smp", name: "SMP", description: "SMP-style combat with enderpearls and tridents." },
  { slug: "axe", name: "Axe", description: "Axe-focused combat with shield breaking." },
  { slug: "mace", name: "Mace", description: "Heavy hitting mace combat." },
] as const;

export type ModernSlug = (typeof MODERN_GAMEMODES)[number]["slug"];

export type GamemodeSlug = ClassicSlug | ModernSlug;

export const CLASSIC_TIERS = [
  "S+",
  "S-",
  "A+",
  "A-",
  "B+",
  "B-",
  "C+",
  "C-",
  "D+",
  "D-",
  "F+",
  "F-",
] as const;

export const MODERN_TIERS = [
  "HT1",
  "LT1",
  "HT2",
  "LT2",
  "HT3",
  "LT3",
  "HT4",
  "LT4",
  "HT5",
  "LT5",
] as const;

export const TIERS = CLASSIC_TIERS;
export type ClassicTier = (typeof CLASSIC_TIERS)[number];
export type ModernTier = (typeof MODERN_TIERS)[number];
export type Tier = ClassicTier | ModernTier;

export const TIER_POINTS: Record<Tier, number> = {
  "S+": 60,
  "S-": 50,
  "A+": 45,
  "A-": 40,
  "B+": 35,
  "B-": 30,
  "C+": 25,
  "C-": 20,
  "D+": 15,
  "D-": 10,
  "F+": 6,
  "F-": 3,
  HT1: 60,
  LT1: 50,
  HT2: 45,
  LT2: 40,
  HT3: 35,
  LT3: 30,
  HT4: 20,
  LT4: 12,
  HT5: 6,
  LT5: 3,
};

export const TIER_COLOR: Record<Tier, string> = {
  "S+": "var(--tier-s)",
  "S-": "var(--tier-s)",
  "A+": "var(--tier-a)",
  "A-": "var(--tier-a)",
  "B+": "var(--tier-b)",
  "B-": "var(--tier-b)",
  "C+": "var(--tier-c)",
  "C-": "var(--tier-c)",
  "D+": "var(--tier-d)",
  "D-": "var(--tier-d)",
  "F+": "var(--tier-f)",
  "F-": "var(--tier-f)",
  HT1: "var(--tier-s)",
  LT1: "var(--tier-s)",
  HT2: "var(--tier-a)",
  LT2: "var(--tier-a)",
  HT3: "var(--tier-b)",
  LT3: "var(--tier-b)",
  HT4: "var(--tier-c)",
  LT4: "var(--tier-c)",
  HT5: "var(--tier-f)",
  LT5: "var(--tier-f)",
};

export const REGIONS = ["NA", "EU", "AS", "SA", "OCE"] as const;
export type Region = (typeof REGIONS)[number];

export const REGION_COLOR: Record<string, string> = {
  NA: "oklch(0.55 0.22 25)",
  EU: "oklch(0.55 0.18 145)",
  AS: "oklch(0.55 0.2 280)",
  SA: "oklch(0.6 0.2 60)",
  OCE: "oklch(0.55 0.18 200)",
};

export function avatarUrl(name: string, size = 96) {
  const clean = name.trim().replace(/[^A-Za-z0-9_]/g, "") || "MHF_Steve";
  return `https://crafthead.net/helm/${clean}/${size}`;
}

export const FALLBACK_AVATAR = "https://crafthead.net/helm/MHF_Steve/96";

export function bodyUrl(name: string, size = 128) {
  const clean = name.trim().replace(/[^A-Za-z0-9_]/g, "") || "MHF_Steve";
  return `https://mc-heads.net/body/${clean}/${size}`;
}

export function bustUrl(name: string, size = 160) {
  const clean = name.trim().replace(/[^A-Za-z0-9_]/g, "") || "MHF_Steve";
  return `https://visage.surgeplay.com/bust/${size}/${clean}`;
}

export function fullPlayerUrl(name: string, size = 256) {
  const clean = name.trim().replace(/[^A-Za-z0-9_]/g, "") || "MHF_Steve";
  return `https://visage.surgeplay.com/full/${size}/${clean}`;
}

export const REGION_FULL_NAME: Record<string, string> = {
  NA: "North America",
  EU: "Europe",
  AS: "Asia",
  SA: "South America",
  OCE: "Oceania",
};

export function regionFullName(code: string): string {
  return REGION_FULL_NAME[code] ?? code;
}

export function titleForPoints(points: number): string {
  if (points >= 200) return "Combat Grandmaster";
  if (points >= 140) return "Combat Master";
  if (points >= 90) return "Combat Ace";
  if (points >= 50) return "Combat Specialist";
  if (points >= 35) return "Combat Cadet";
  return "Combat Rookie";
}

// Colors for combat titles (used for subtle glows + badges)
export function titleColorForPoints(points: number): string {
  if (points >= 200) return "var(--tier-s)";
  if (points >= 140) return "var(--tier-a)";
  if (points >= 90) return "var(--tier-b)";
  if (points >= 50) return "var(--tier-c)";
  if (points >= 35) return "var(--tier-d)";
  return "var(--tier-f)";
}

import boxingIcon from "@/assets/icons/boxing.png";
import bedfightIcon from "@/assets/icons/bedfight.png";
import fireballfightIcon from "@/assets/icons/fireballfight.png";
import sumoIcon from "@/assets/icons/sumo.png";
import nodebuffIcon from "@/assets/icons/nodebuff.png";
import uhcIcon from "@/assets/icons/uhc.png";
import spleefIcon from "@/assets/icons/spleef.png";

import swordIcon from "@/assets/icons/modern/sword.png";
import nethopIcon from "@/assets/icons/modern/nethop.png";
import axeIcon from "@/assets/icons/modern/axe.png";
import potIcon from "@/assets/icons/modern/pot.png";
import vanillaIcon from "@/assets/icons/modern/vanilla.png";
import smpIcon from "@/assets/icons/modern/smp.png";
import maceIcon from "@/assets/icons/modern/mace.png";
import modernUhcIcon from "@/assets/icons/modern/uhc.png";

export const GAMEMODE_ICON: Record<ClassicSlug, string> = {
  boxing: boxingIcon,
  bedfight: bedfightIcon,
  fireballfight: fireballfightIcon,
  sumo: sumoIcon,
  nodebuff: nodebuffIcon,
  uhc: uhcIcon,
  spleef: spleefIcon,
};

export const MODERN_GAMEMODE_ICON: Record<ModernSlug, string> = {
  sword: swordIcon,
  nethop: nethopIcon,
  axe: axeIcon,
  pot: potIcon,
  vanilla: vanillaIcon,
  smp: smpIcon,
  mace: maceIcon,
  uhc: modernUhcIcon,
};

import type { PvpMode } from "@/hooks/usePvpMode";

export function gamemodesFor(mode: PvpMode) {
  return mode === "modern" ? MODERN_GAMEMODES : GAMEMODES;
}

export function tiersFor(mode: PvpMode) {
  return mode === "modern" ? MODERN_TIERS : CLASSIC_TIERS;
}

export function iconFor(mode: PvpMode, slug: string): string | undefined {
  if (mode === "modern") return MODERN_GAMEMODE_ICON[slug as ModernSlug];
  return GAMEMODE_ICON[slug as ClassicSlug];
}
