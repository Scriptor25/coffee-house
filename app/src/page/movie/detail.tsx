import { resource } from "@runtime/resource";
import { Image } from "../../component/image/image";
import { Suspense } from "../../component/suspense/suspense";
import { createMoviePlayback, getMovieById } from "../../data/movie";
import { sharePlayback } from "../../data/playback";
import { setMetadata } from "../../meta/meta";
import styles from "./detail.module.css";

export function MovieDetailPage(props: { id: string }) {
  const $item = resource(() => getMovieById(props.id));

  setMetadata({
    type: "website",
    title: "Loading",
  });

  return (
    <Suspense
      resource={$item}
      pending={<p>Loading movie...</p>}
      error={<p>Failed to load movie.</p>}
    >
      {(item) => {
        setMetadata({
          type: "video.movie",
          title: item.title,
          image: item.poster
            .filter((data) => data.width < 0)
            .map((data) => data.url)[0],
          description: item.description,
        });

        return (
          <>
            <div className={styles.banner}>
              <Image
                className={styles.backdrop}
                src={item.backdrop}
                sizes="100vw"
              />
              <Image
                className={styles.poster}
                src={item.poster}
                sizes="200px"
              />
            </div>
            <main className={styles.content}>
              <h1>{item.title}</h1>
              <p>{item.description}</p>
              <p>
                <button
                  type="button"
                  onclick={() => {
                    sharePlayback(item.title, item.description, () =>
                      createMoviePlayback(props.id),
                    );
                  }}
                >
                  Play
                </button>
              </p>
            </main>
          </>
        );
      }}
    </Suspense>
  );
}
