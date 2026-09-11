import { computed } from "@runtime/computed";
import { setMetadata } from "../../meta/meta";
import { resource } from "@runtime/resource";
import { MediaList } from "../../component/media-list/media-list";

export function MovieListPage() {
  // TODO: get all movies
  const $items = resource(async () => []);

  setMetadata({
    type: "website",
    title: "Movies",
    description: "The Movies Page",
  });

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
          case "success":
            return <MediaList data={items.data} mode="grid" />;
          case "error":
            return <p>Failed to load movies.</p>;
        }
      })}
    </main>
  );
}
