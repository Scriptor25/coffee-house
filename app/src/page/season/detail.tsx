import { resource } from "@runtime/resource";
import { Image } from "../../component/image/image";
import {
  MediaListItem,
  type MediaListMode,
} from "../../component/media-item/media-item";
import { MediaListContainer } from "../../component/media-list/media-list";
import { Suspense } from "../../component/suspense/suspense";
import { getEpisodeById } from "../../data/episode";
import { sharePlayback } from "../../data/playback";
import { createSeasonPlayback, getSeasonById } from "../../data/season";
import styles from "./detail.module.css";

export function SeasonEpisode(props: { id: string; mode: MediaListMode }) {
  const $item = resource(() => getEpisodeById(props.id));

  return (
    <Suspense
      resource={$item}
      pending={<>Loading episode...</>}
      error={<>Failed to load episode.</>}
    >
      {(item) => (
        <MediaListItem
          href={`#/episode/${props.id}`}
          title={item.title}
          thumbnail={
            item.still.length
              ? (className, sizes) => (
                  <Image className={className} sizes={sizes} src={item.still} />
                )
              : undefined
          }
          mode={props.mode}
        />
      )}
    </Suspense>
  );
}

export function SeasonDetailPage(props: { id: string }) {
  const $item = resource(() => getSeasonById(props.id));

  return (
    <main>
      <Suspense
        resource={$item}
        pending={<p>Loading season...</p>}
        error={<p>Failed to load season.</p>}
      >
        {(item) => (
          <>
            <section className={styles.header}>
              <Image
                className={styles.poster}
                src={item.poster}
                sizes="(max-width: 768px) 100vw, 30vw"
              />
              <div>
                <h1>{item.title}</h1>
                <p>{item.description}</p>
                <p>
                  <button
                    type="button"
                    onclick={() => {
                      sharePlayback(item.title, item.description, () =>
                        createSeasonPlayback(props.id),
                      );
                    }}
                  >
                    Play all
                  </button>
                </p>
              </div>
            </section>
            <h2>Episodes</h2>
            <MediaListContainer
              items={item.episodes.map((id) => (mode) => (
                <SeasonEpisode id={id} mode={mode} />
              ))}
              mode="list"
            />
          </>
        )}
      </Suspense>
    </main>
  );
}
