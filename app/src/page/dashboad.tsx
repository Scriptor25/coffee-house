import { computed } from "@runtime/computed";
import { resource } from "@runtime/resource";
import { MediaList } from "../component/media-list/media-list";
import { getAllMovies } from "../data/movie";
import { setMetadata } from "../meta/meta";

export function DashboardPage() {
  setMetadata({
    type: "website",
    title: "Dashboard",
    description: "The Dashboard Page",
  });

  const $movies = resource(getAllMovies);

  return (
    <main>
      <h1>Dashboard</h1>

      {computed(() => {
        const movies = $movies.get();

        switch (movies.status) {
          case "none":
            $movies.load();
          case "pending":
            return <p>Loading movies...</p>;
          case "success":
            return (
              <MediaList
                data={movies.data.map((item) => ({
                  href: `#/movie/${item.id}`,
                  title: item.title,
                  thumbnail: item.poster,
                }))}
                mode="grid"
              />
            );
          case "error":
            return <p>Failed to load movies.</p>;
        }
      })}
    </main>
  );
}
