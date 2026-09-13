import { resource } from "@runtime/resource";
import { Image } from "../../component/image/image";
import { Suspense } from "../../component/suspense/suspense";
import { createEpisodePlayback, getEpisodeById } from "../../data/episode";
import { sharePlayback } from "../../data/playback";

export function EpisodeDetailPage(props: { id: string }) {
  const $item = resource(() => getEpisodeById(props.id));

  return (
    <main>
      <Suspense
        resource={$item}
        pending={<p>Loading episode...</p>}
        error={<p>Failed to load episode.</p>}
      >
        {(item) => (
          <>
            <h1>{item.title}</h1>
            <p>{item.description}</p>
            <Image src={item.still} />
            <button
              onclick={() => {
                sharePlayback(item.title, item.description, () =>
                  createEpisodePlayback(props.id),
                );
              }}
            >
              Play
            </button>
          </>
        )}
      </Suspense>
    </main>
  );
}
