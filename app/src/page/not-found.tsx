import { setMetadata } from "../meta/meta";

export function NotFoundPage() {
  setMetadata({
    type: "website",
    title: "Not Found",
    description: "Page Not Found",
  });

  return (
    <main>
      <h1>404</h1>
      <h2>Not Found</h2>
    </main>
  );
}
