import { Image } from "../../component/image/image";
import { MediaList } from "../../component/media-list/media-list";
import { Suspense } from "../../component/suspense/suspense";
import { getAllMovies } from "../../data/movie";
import { setMetadata } from "../../meta/meta";

export function MovieListPage() {
  const $items = getAllMovies();

  setMetadata({
    type: "website",
    title: "Movies",
    description: "The Movies Page",
  });

  return (
    <main>
      <h1>Movies</h1>

      <Suspense
        resource={$items}
        pending={<p>Loading movies...</p>}
        error={<p>Failed to load movies.</p>}
      >
        {(items) => (
          <MediaList
            items={items.map((item) => ({
              href: `#/movie/${item.id}`,
              title: item.title,
              thumbnail: item.poster.length
                ? (className, sizes) => (
                    <Image
                      className={className}
                      sizes={sizes}
                      src={item.poster}
                    />
                  )
                : undefined,
            }))}
            mode="grid-poster"
          />
        )}
      </Suspense>
    </main>
  );
}
