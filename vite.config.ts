import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { tanstackStart } from "@tanstack/react-start/plugin/vite";
import { defineConfig } from "vite";
import tsConfigPaths from "vite-tsconfig-paths";

const allowedHosts = ["https://fc8d5aec.forgifiedtier.pages.dev", "forgified.club"];

export default defineConfig({
  plugins: [
    tanstackStart(), // MUST be first
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
