import { defineConfig } from "vite";
import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";

export default defineConfig({
  plugins: [scalaJSPlugin()],
  build: {
    modulePreload: {
      polyfill: false
    },
  },
  base: "/laminar-archipelago/",
});
