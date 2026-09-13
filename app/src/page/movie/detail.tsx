import { computed } from "@runtime/computed";
import { resource } from "@runtime/resource";
import { getTmdbImgUrl, TmdbImg } from "../../component/tmdb-img/tmdb-img";
import { getTmdbConfiguration } from "../../data/configuration";
import { getMovieById } from "../../data/movie";
import { setMetadata } from "../../meta/meta";
import styles from "./detail.module.css";

export function MovieDetailPage(props: { id: string }) {
  const $config = resource(getTmdbConfiguration);
  const $item = resource(() => getMovieById(props.id));

  setMetadata({
    type: "website",
    title: "Loading",
  });

  return computed(() => {
    const config = $config.get();

    switch (config.status) {
      case "none":
        $config.load();
      case "pending":
        return <>Loading config...</>;
      case "error":
        return <>Failed to load config.</>;
      case "success":
        break;
    }

    return (
      <main>
        {computed(() => {
          const item = $item.get();

          switch (item.status) {
            case "none":
              $item.load();
            case "pending":
              return <p>Loading movie...</p>;
            case "error":
              return <p>Failed to load movie.</p>;
            case "success":
              setMetadata({
                type: "video.movie",
                title: item.data.title,
                image: item.data.poster
                  ? getTmdbImgUrl(
                      item.data.poster,
                      "poster",
                      config.data,
                      "original",
                    )
                  : undefined,
                description: item.data.description,
              });

              return (
                <>
                  <div className={styles.banner}>
                    {item.data.backdrop && (
                      <TmdbImg
                        className={styles.backdrop}
                        src={item.data.backdrop.slice(5)}
                        type="backdrop"
                        config={config.data}
                      />
                    )}
                    {item.data.poster && (
                      <TmdbImg
                        className={styles.poster}
                        src={item.data.poster.slice(5)}
                        type="poster"
                        config={config.data}
                        sizes="200px"
                      />
                    )}
                  </div>
                  <div className={styles.content}>
                    <h1>{item.data.title}</h1>
                    <p>{item.data.description}</p>
                  </div>
                </>
              );
          }
        })}
      </main>
    );
  });
}
