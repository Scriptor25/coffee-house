import type { VNode } from "@runtime/jsx-runtime";
import { computed, effect, signal, type Signal } from "@runtime/state";
import { getAllMedia, type Media } from "../../data/media";
import { createPlayback } from "../../data/playback";
import { getOrigin } from "../../util/origin";
import {
  buildTree,
  DirectoryNode,
  getCommonBase,
  MediaNode,
  segments,
} from "../../util/tree";

async function copyUrl(url: URL) {
  await window.navigator.clipboard.writeText(url.toString());
}

function StaticItem(props: { href: string; children?: VNode }) {
  return (
    <li>
      <a href={props.href}>{props.children}</a>
    </li>
  );
}

function MediaItem(props: { token: string; data: Media }) {
  const copySingleUrl = async (direct: boolean) => {
    const base = await createPlayback(props.token, props.data.title, [
      props.data.id,
    ]);
    if (!base) return;

    const pathname = direct ? `${base}/0` : `${base}/0/master.m3u8`;
    const url = new URL(pathname, getOrigin());

    await copyUrl(url);
  };

  return (
    <li>
      {props.data.title}
      <button type="button" onclick={() => copySingleUrl(true)}>
        direct
      </button>
      <button type="button" onclick={() => copySingleUrl(false)}>
        hls
      </button>
    </li>
  );
}

export function Dashboard(props: { session: Signal<string | null> }) {
  document.title = "Dashboard";

  const sFragment = signal(window.location.hash);

  window.addEventListener("hashchange", (event) => {
    event.preventDefault();

    sFragment.set(window.location.hash);
  });

  const sItems = signal<Media[] | null>(null);

  effect(() => {
    const token = props.session.get();

    if (token) {
      getAllMedia(token).then((data) => {
        sItems.set(data);
      });
    }
  });

  return computed(() => {
    const token = props.session.get();
    if (!token) return;

    const items = sItems.get();
    if (!items) return <>loading...</>;

    const fragment = sFragment.get();
    const slug = fragment.length ? segments(decodeURI(fragment.slice(1))) : [];

    const base = getCommonBase(items);
    const tree = buildTree(items, base);

    let node: DirectoryNode | null = tree;
    for (const segment of slug) {
      if (node === null || node.name === segment) break;
      node =
        node.children
          .filter((node) => node instanceof DirectoryNode)
          .find((node) => node.name === segment) ?? null;
    }

    if (!node) return;

    const sorted = node.children.toSorted((a, b) =>
      a.name.localeCompare(b.name),
    );

    const list = sorted.map((node) => {
      if (node instanceof MediaNode) {
        return <MediaItem token={token} data={node.item} />;
      } else {
        const href = `/#${slug.length ? "/" + slug.join("/") : ""}/${node.name}`;
        return <StaticItem href={encodeURI(href)}>{node.name}</StaticItem>;
      }
    });

    if (slug.length) {
      const target = slug.slice(0, -1);

      const href = `/#/${target.join("/")}`;
      list.unshift(<StaticItem href={encodeURI(href)}>..</StaticItem>);
    }

    const copyPlaylistUrl = async (direct: boolean) => {
      const playlist = sorted
        .filter((item) => item instanceof MediaNode)
        .map((item) => item.item.id);

      const base = await createPlayback(token, node.name, playlist);
      if (!base) return;

      const pathname = `${base}/playlist.m3u8?direct=${direct}`;
      const url = new URL(pathname, getOrigin());

      await copyUrl(url);
    };

    return (
      <>
        <div>
          <span>Playlist</span>
          <button type="button" onclick={() => copyPlaylistUrl(true)}>
            direct
          </button>
          <button type="button" onclick={() => copyPlaylistUrl(false)}>
            hls
          </button>
        </div>
        <ul>{list}</ul>
      </>
    );
  });
}
