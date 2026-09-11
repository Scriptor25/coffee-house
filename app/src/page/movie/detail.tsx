import { resource } from "@runtime/resource";
import { setMetadata } from "../../meta/meta";

export function MovieDetailPage(props: { id: string }) {
  // TODO: get movie by id
  const $item = resource(async () => ({}));

  setMetadata({
    type: "website",
    title: "Movie TODO",
    description: "The Movie Page",
  });

  return (
    <main>
      <h1>Movie TODO</h1>
    </main>
  );
}
