import { context } from "esbuild";
import { existsSync } from "node:fs";
import { cp, readFile, rm, watch } from "node:fs/promises";
import { createServer, request } from "node:http";
import { extname, join, resolve } from "node:path";
import { config } from "./build.js";

const distDir = "dist";
const publicDir = "public";

const server = createServer(async (req, res) => {
  const url = new URL(req.url, `http://${req.headers.host}`);

  let pathname = url.pathname;
  if (pathname === "/") {
    pathname = "/index.html";
  }

  console.log(`${req.method} ${url.pathname}`);

  switch (pathname) {
    case "/index.html":
    case "/index.js":
    case "/index.js.map":
    case "/index.css":
    case "/index.css.map":
      const file = join(distDir, pathname);
      const data = await readFile(file);

      const type = (() => {
        switch (extname(file)) {
          case ".html":
            return "text/html";
          case ".js":
            return "text/javascript";
          case ".css":
            return "text/css";
          default:
            return "application/octet-stream";
        }
      })();

      res.writeHead(200, {
        "content-type": type,
        "cache-control": "public, max-age=604800, immutable",
      });
      res.end(data);
      break;

    default:
      const proxy = request(
        {
          hostname: "localhost",
          port: 8090,
          path: `${url.pathname}${url.hash}${url.search}`,
          method: req.method,
          headers: {
            ...req.headers,
            host: "localhost:8090",
          },
        },
        (proxyRes) => {
          res.writeHead(proxyRes.statusCode ?? 502, proxyRes.headers);

          proxyRes.pipe(res);
        },
      );

      proxy.on("error", (err) => {
        console.error("proxy error:", err);

        if (!res.headersSent) {
          res.writeHead(502);
        }

        res.end("Bad Gateway");
      });

      req.pipe(proxy);
      break;
  }
});

(async () => {
  await cp(publicDir, distDir, { recursive: true });

  const watcher = watch(publicDir, { recursive: true });

  for await (const event of watcher) {
    const src = resolve(publicDir, event.filename);
    const dst = resolve(distDir, event.filename);

    switch (event.eventType) {
      case "rename":
        console.log("rename", event.filename);
        if (existsSync(src)) {
          await cp(src, dst, { recursive: true });
        } else {
          await rm(dst, { recursive: true });
        }
        break;
      case "change":
        console.log("change", event.filename);
        await cp(src, dst, { recursive: true });
        break;
    }
  }
})();

const ctx = await context(config);

await ctx.watch();

server.listen(8080, "0.0.0.0", () => {
  console.log("listening on port 8080");
});
