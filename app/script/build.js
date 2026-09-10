import { build } from "esbuild";

/**
 * @type {import("esbuild").BuildOptions} config
 */
export const config = {
  entryPoints: ["src/index.tsx"],

  outdir: "dist",

  bundle: true,
  format: "esm",
  target: "esnext",

  jsx: "automatic",
  jsxImportSource: "@runtime",

  sourcemap: true,
  minify: true,

  loader: {
    ".scss": "css",
  },
};

await build(config);
