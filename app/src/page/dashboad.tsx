import { computed } from "@runtime/computed";
import { resource } from "@runtime/resource";
import { MediaList } from "../component/media-list/media-list";
import { getAllMedia } from "../data/media";
import { setMetadata } from "../meta/meta";

export function DashboardPage() {
  setMetadata({
    type: "website",
    title: "Dashboard",
    description: "The Dashboard Page",
  });

  const sItems = resource(getAllMedia);

  const cList = computed(() => {
    const items = sItems.get();

    switch (items.status) {
      case "none":
        sItems.load();
      case "pending":
        return <p>Loading media items...</p>;
      case "success":
        return <MediaList data={items.data} mode="grid" />;
      case "error":
        return <p>Failed to load media items.</p>;
    }
  });

  return (
    <main>
      <h1>Dashboard</h1>

      {cList}
    </main>
  );
}
