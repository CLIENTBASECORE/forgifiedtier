import { cloudflare } from "@cloudflare/vite-plugin";
import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { tanstackStart } from "@tanstack/react-start/plugin/vite";
import { defineConfig } from "vite";
import tsConfigPaths from "vite-tsconfig-paths";

const allowedHosts = true;

export default defineConfig({
  plugins: [
    cloudflare({ viteEnvironment: { name: "ssr" } }), // MUST be first for Cloudflare environment
    tanstackStart(), // MUST be before react
    react(), // MUST come after tanstackStart
    tailwindcss(),
    tsConfigPaths(),
  ],

  server: {
    allowedHosts,
    host: true,
  },

  preview: {
    allowedHosts,
    host: true,
  },
});
