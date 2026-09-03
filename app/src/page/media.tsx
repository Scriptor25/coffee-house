import { computed } from "@runtime/computed";
import { resource } from "@runtime/resource";
import { getMediaById } from "../data/media";
import { setMetadata } from "../meta/meta";

export function MediaPage(props: { id: string }) {
  setMetadata({
    type: "website",
    title: "Media",
  });

  const sItem = resource(() => getMediaById(props.id));

  const cContent = computed(() => {
    const item = sItem.get();

    switch (item.status) {
      case "none":
        sItem.load();
      case "pending":
        return <p>Loading media item...</p>;
      case "error":
        return <p>Failed to load media item.</p>;
      case "success":
        return <h1>{item.data.title}</h1>;
    }
  });

  return <main>{cContent}</main>;
}
