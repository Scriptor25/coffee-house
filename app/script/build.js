import { build } from "esbuild";

await build({
  entryPoints: ["src/index.tsx"],

  outdir: "dist",

  bundle: true,
  format: "esm",
  target: "esnext",

  jsx: "automatic",
  jsxImportSource: "@runtime",

  sourcemap: true,
  minify: false,
});
