import type { HTMLElementProps } from "@runtime/jsx-runtime";

export interface ImageData {
  url: string;
  width: number;
}

export interface ImageProps extends Omit<
  HTMLElementProps<HTMLImageElement>,
  "src" | "srcset"
> {
  src: ImageData[];
}

export function Image({ src, ...props }: ImageProps) {
  return (
    <img
      srcset={src
        .map((it) =>
          it.width >= 0 ? `${it.url} ${it.width}w` : `${it.url} 1920w`,
        )
        .join(", ")}
      {...props}
    />
  );
}
