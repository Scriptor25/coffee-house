import { render } from "@runtime/jsx-runtime";
import { buildMetadata, type Metadata } from "./og";

export function setMetadata(metadata: Metadata & { description?: string }) {
  const og = buildMetadata(metadata);

  document.title = metadata.title ?? "Document";
  render(
    document.head,
    <>
      {metadata.title && <meta name="title" content={metadata.title} />}
      {metadata.description && (
        <meta name="description" content={metadata.description} />
      )}
      {og}
    </>,
  );
}
