import { computed } from "@runtime/computed";
import { resource } from "@runtime/resource";
import { MediaList } from "../../component/media-list/media-list";
import { TmdbImg } from "../../component/tmdb-img/tmdb-img";
import { getTmdbConfiguration } from "../../data/configuration";
import { getAllMovies } from "../../data/movie";
import { setMetadata } from "../../meta/meta";

export function MovieListPage() {
  const $config = resource(getTmdbConfiguration);
  const $items = resource(getAllMovies);

  setMetadata({
    type: "website",
    title: "Movies",
    description: "The Movies Page",
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
        <h1>Movies</h1>

        {computed(() => {
          const items = $items.get();

          switch (items.status) {
            case "none":
              $items.load();
            case "pending":
              return <p>Loading movies...</p>;
            case "error":
              return <p>Failed to load movies.</p>;
            case "success":
              return (
                <MediaList
                  data={items.data
                    .sort((a, b) => a.title.localeCompare(b.title))
                    .map((item) => ({
                      href: `#/movie/${item.id}`,
                      title: item.title,
                      thumbnail: item.poster
                        ? (className) => (
                            <TmdbImg
                              className={className}
                              sizes="(max-width: 600px) 50vw, 300px"
                              src={item.poster!.slice(5)}
                              type="poster"
                              config={config.data}
                            />
                          )
                        : undefined,
                    }))}
                  mode="grid-poster"
                />
              );
          }
        })}
      </main>
    );
  });
}
