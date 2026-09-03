import { computed } from "@runtime/computed";
import { effect } from "@runtime/effect";
import type { VNode } from "@runtime/jsx-runtime";
import { reference } from "@runtime/reference";
import { Signal, signal } from "@runtime/signal";
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

async function copyUrl(open: Signal<URL | null>, url: URL) {
  if (!!window.navigator.share) {
    window.navigator.share({ url: url.toString() });
    return;
  }

  open.set(url);
}

function StaticItem(props: { href: string; children?: VNode }) {
  return (
    <li>
      <a href={props.href}>{props.children}</a>
    </li>
  );
}

function MediaItem(props: { open: Signal<URL | null>; data: Media }) {
  const copySingleUrl = async (direct: boolean) => {
    const base = await createPlayback(props.data.title, [props.data.id]);
    if (!base) return;

    const pathname = direct ? `${base}/0` : `${base}/0/master.m3u8`;
    const url = new URL(pathname, getOrigin());

    await copyUrl(props.open, url);
  };

  return (
    <li>
      {props.data.title}
      &nbsp;
      <button type="button" onclick={() => copySingleUrl(true)}>
        direct
      </button>
      &nbsp;
      <button type="button" onclick={() => copySingleUrl(false)}>
        hls
      </button>
    </li>
  );
}

function Dialog(props: { open: Signal<URL | null> }) {
  const ref = reference<HTMLDialogElement>();

  effect(() => {
    const dialog = ref.get();
    const url = props.open.get();

    if (!dialog) return;

    if (url) {
      dialog.showModal();
    } else {
      dialog.close();
    }
  });

  return computed(() => {
    const url = props.open.get();

    return (
      <dialog ref={ref} onclose={() => props.open.set(null)}>
        {url && <a href={url.toString()}>Copy Playback URL</a>}
      </dialog>
    );
  });
}

export function Dashboard() {
  document.title = "Dashboard";

  const sFragment = signal(window.location.hash);

  window.addEventListener("hashchange", (event) => {
    event.preventDefault();

    sFragment.set(window.location.hash);
  });

  const sItems = signal<Media[] | null>(null);

  effect(() => {
    getAllMedia().then((data) => {
      sItems.set(data);
    });
  });

  const sOpen = signal<URL | null>(null);

  return computed(() => {
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
        return <MediaItem open={sOpen} data={node.item} />;
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

      const base = await createPlayback(node.name, playlist);
      if (!base) return;

      const pathname = `${base}/playlist.m3u8?direct=${direct}`;
      const url = new URL(pathname, getOrigin());

      await copyUrl(sOpen, url);
    };

    return (
      <>
        <Dialog open={sOpen} />
        <div>
          <span>Playlist</span>
          &nbsp;
          <button type="button" onclick={() => copyPlaylistUrl(true)}>
            direct
          </button>
          &nbsp;
          <button type="button" onclick={() => copyPlaylistUrl(false)}>
            hls
          </button>
        </div>
        <ul>{list}</ul>
      </>
    );
  });
}
