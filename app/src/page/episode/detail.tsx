import { Image } from "../../component/image/image";
import { Suspense } from "../../component/suspense/suspense";
import { createEpisodePlayback, getEpisodeById } from "../../data/episode";
import { sharePlayback } from "../../data/playback";
import styles from "./detail.module.css";

export function EpisodeDetailPage(props: { id: string }) {
  const $item = getEpisodeById(props.id);

  return (
    <main>
      <Suspense
        resource={$item}
        pending={<p>Loading episode...</p>}
        error={<p>Failed to load episode.</p>}
      >
        {(item) => (
          <>
            <section className={styles.header}>
              {item.still.length ? (
                <Image
                  className={styles.still}
                  src={item.still}
                  sizes="(max-width: 768px) 100vw, 30vw"
                />
              ) : undefined}
              <div>
                <h1>{item.title}</h1>
                <p>{item.description}</p>
                <p>
                  <button
                    type="button"
                    onclick={() => {
                      sharePlayback(
                        item.title,
                        () => createEpisodePlayback(props.id),
                        0,
                      );
                    }}
                  >
                    Play
                  </button>
                </p>
              </div>
            </section>
          </>
        )}
      </Suspense>
    </main>
  );
}
