import { resource } from "@runtime/resource";
import { Image } from "../../component/image/image";
import { MediaList } from "../../component/media-list/media-list";
import { Suspense } from "../../component/suspense/suspense";
import { getAllShows } from "../../data/show";

export function ShowListPage() {
  const $items = resource(getAllShows);

  return (
    <main>
      <Suspense
        resource={$items}
        pending={<p>Loading shows...</p>}
        error={<p>Failed to load shows.</p>}
      >
        {(items) => (
          <MediaList
            items={items.map((item) => ({
              href: `#/show/${item.id}`,
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
