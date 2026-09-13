import { computed } from "@runtime/computed";
import { effect } from "@runtime/effect";
import type { Component } from "@runtime/jsx-runtime";
import { signal } from "@runtime/signal";
import { getSessionToken } from "../../data/session";
import { DashboardPage } from "../../page/dashboad";
import { EpisodeDetailPage } from "../../page/episode/detail";
import { LoginPage } from "../../page/login";
import { MovieDetailPage } from "../../page/movie/detail";
import { MovieListPage } from "../../page/movie/list";
import { NotFoundPage } from "../../page/not-found";
import { SeasonDetailPage } from "../../page/season/detail";
import { ShowDetailPage } from "../../page/show/detail";
import { ShowListPage } from "../../page/show/list";

type RouteNode =
  | Component<any>
  | ({ [segment: string]: RouteNode } & { "/"?: Component<any> });

const routes: RouteNode = {
  "/": DashboardPage,
  login: LoginPage,
  movie: {
    "/": MovieListPage,
    "[id]": MovieDetailPage,
  },
  show: {
    "/": ShowListPage,
    "[id]": ShowDetailPage,
  },
  season: {
    "[id]": SeasonDetailPage,
  },
  episode: {
    "[id]": EpisodeDetailPage,
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

    let node: RouteNode | null = routes;

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
