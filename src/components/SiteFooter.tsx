export function SiteFooter() {
  return (
    <footer className="mt-16 border-t border-border/60 bg-background/60">
      <div className="mx-auto flex max-w-7xl flex-col items-center justify-between gap-4 px-4 py-6 sm:flex-row">
        <p className="text-xs text-muted-foreground">
          © {new Date().getFullYear()} Forgified Tiers. Minecraft PvP Tier Lists — Classic & Modern.
        </p>
        <div className="flex items-center gap-3">
          <a
            href="https://discord.gg/NyawUnyyVk"
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-2 rounded-md border border-border bg-card/60 px-3 py-1.5 text-xs font-medium text-muted-foreground transition-colors hover:border-primary/50 hover:text-foreground"
            aria-label="Join our Discord"
          >
            <svg viewBox="0 0 24 24" className="h-4 w-4 fill-current" aria-hidden="true">
              <path d="M20.317 4.369A19.79 19.79 0 0 0 16.558 3a14.62 14.62 0 0 0-.69 1.414 18.27 18.27 0 0 0-5.735 0A14.6 14.6 0 0 0 9.443 3a19.79 19.79 0 0 0-3.76 1.369C2.16 9.59 1.21 14.68 1.685 19.7a19.94 19.94 0 0 0 6.06 3.06c.49-.66.926-1.36 1.3-2.1a12.91 12.91 0 0 1-2.05-.99c.172-.126.34-.258.502-.39a14.27 14.27 0 0 0 12.005 0c.164.134.332.266.503.39-.654.39-1.34.722-2.052.99.374.74.81 1.44 1.3 2.1a19.9 19.9 0 0 0 6.06-3.06c.555-5.83-.95-10.87-3.997-15.33ZM8.02 16.51c-1.183 0-2.157-1.085-2.157-2.42 0-1.336.955-2.42 2.157-2.42 1.21 0 2.176 1.094 2.157 2.42 0 1.335-.955 2.42-2.157 2.42Zm7.96 0c-1.183 0-2.157-1.085-2.157-2.42 0-1.336.955-2.42 2.157-2.42 1.21 0 2.176 1.094 2.157 2.42 0 1.335-.946 2.42-2.157 2.42Z" />
            </svg>
            Discord
          </a>
          <a
            href="https://www.youtube.com/@ForgifiedOfficial"
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-2 rounded-md border border-border bg-card/60 px-3 py-1.5 text-xs font-medium text-muted-foreground transition-colors hover:border-primary/50 hover:text-foreground"
            aria-label="Visit our YouTube channel"
          >
            <svg viewBox="0 0 24 24" className="h-4 w-4 fill-current" aria-hidden="true">
              <path d="M23.498 6.186a3.016 3.016 0 0 0-2.122-2.136C19.505 3.545 12 3.545 12 3.545s-7.505 0-9.377.505A3.017 3.017 0 0 0 .502 6.186C0 8.07 0 12 0 12s0 3.93.502 5.814a3.016 3.016 0 0 0 2.122 2.136c1.871.505 9.376.505 9.376.505s7.505 0 9.377-.505a3.015 3.015 0 0 0 2.122-2.136C24 15.93 24 12 24 12s0-3.93-.502-5.814ZM9.545 15.568V8.432L15.818 12l-6.273 3.568Z" />
            </svg>
            YouTube
          </a>
        </div>
      </div>
    </footer>
  );
}
