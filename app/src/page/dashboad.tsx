import { resource } from "@runtime/resource";
import { Image } from "../component/image/image";
import { MediaList } from "../component/media-list/media-list";
import { Suspense } from "../component/suspense/suspense";
import { getAllMovies } from "../data/movie";
import { getAllShows } from "../data/show";
import { setMetadata } from "../meta/meta";
import styles from "./dashboard.module.css";

export function DashboardPage() {
  const $movies = resource(() => getAllMovies(4));
  const $shows = resource(() => getAllShows(4));

  setMetadata({
    type: "website",
    title: "Dashboard",
    description: "The Dashboard Page",
  });

  return (
    <main>
      <h1>Dashboard</h1>

      <section className={styles.section}>
        <h2>Movies</h2>

        <Suspense
          resource={$movies}
          pending={<p>Loading movies...</p>}
          error={<p>Failed to load movies.</p>}
        >
          {(movies) => (
            <MediaList
              items={movies.map((movie) => ({
                href: `#/movie/${movie.id}`,
                title: movie.title,
                thumbnail: (className) => (
                  <Image
                    src={movie.poster}
                    sizes="(max-width: 600px) 50vw, 300px"
                    className={className}
                  />
                ),
              }))}
              end={{
                href: "#/movie",
                title: "All movies",
              }}
              mode="grid"
            />
          )}
        </Suspense>
      </section>

      <section className={styles.section}>
        <h2>Shows</h2>

        <Suspense
          resource={$shows}
          pending={<p>Loading shows...</p>}
          error={<p>Failed to load shows.</p>}
        >
          {(shows) => (
            <MediaList
              items={shows.map((show) => ({
                href: `#/show/${show.id}`,
                title: show.title,
                thumbnail: (className) => (
                  <Image
                    src={show.poster}
                    sizes="(max-width: 600px) 50vw, 300px"
                    className={className}
                  />
                ),
              }))}
              end={{
                href: "#/show",
                title: "All shows",
              }}
              mode="grid"
            />
          )}
        </Suspense>
      </section>
    </main>
  );
}
