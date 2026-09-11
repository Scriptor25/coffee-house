import { computed } from "@runtime/computed";
import type { Component } from "@runtime/jsx-runtime";
import { signal } from "@runtime/signal";
import { getSessionToken } from "../../data/session";
import { DashboardPage } from "../../page/dashboad";
import { LoginPage } from "../../page/login";
import { NotFoundPage } from "../../page/not-found";
import { effect } from "@runtime/effect";
import { MovieListPage } from "../../page/movie/list";
import { MovieDetailPage } from "../../page/movie/detail";

type RouteNode =
  | Component<any>
  | ({ [segment: string]: RouteNode } & { "/"?: Component<any> });

const config: RouteNode = {
  "/": DashboardPage,
  login: LoginPage,
  movie: {
    "/": MovieListPage,
    "[id]": MovieDetailPage,
  },
};

export function App() {
  const $fragment = signal(window.location.hash);

  effect(() => {
    $fragment.get();

    const token = getSessionToken();
    if (!token) {
      window.location.hash = "#/login";
    }
  });

  window.addEventListener("hashchange", (event) => {
    event.preventDefault();

    $fragment.set(window.location.hash);
  });

  return computed(() => {
    const fragment = $fragment.get();
    const segments = fragment.length
      ? decodeURIComponent(fragment.slice(1))
          .split("/")
          .filter((s) => !!s)
      : [];

    const named: Record<string, string> = {};

    let node: RouteNode | null = config;

    for (const segment of segments) {
      if (typeof node === "function" || !node) {
        node = null;
        break;
      }
      if (segment in node) {
        node = node[segment]!;
      } else {
        let found = false;
        for (const key in node) {
          if (key.startsWith("[") && key.endsWith("]")) {
            found = true;
            named[key.slice(1, -1)] = segment;
            node = node[key]!;
            break;
          }
        }
        if (!found) {
          node = null;
          break;
        }
      }
    }

    if (node) {
      if (typeof node === "function") {
        const N = node;
        return <N {...named} />;
      }

      if ("/" in node) {
        const N = node["/"]!;
        return <N {...named} />;
      }
    }

    return <NotFoundPage />;
  });
}
