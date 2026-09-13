import type { HTMLElementProps } from "@runtime/jsx-runtime";
import type { TmdbConfiguration } from "../../data/configuration";

export type TmdbImgType = "backdrop" | "logo" | "poster" | "profile" | "still";

export function TmdbImg({
  src,
  type,
  config,
  ...props
}: {
  src: string;
  type: TmdbImgType;
  config: TmdbConfiguration;
} & Omit<
  HTMLElementProps<HTMLImageElement>,
  "src" | "srcset" | "type" | "config"
>) {
  const base = config.images.secure_base_url;
  const sizes = config.images[`${type}_sizes`];

  const srcs = sizes
    .filter((size) => size !== "original")
    .map((size) => {
      const width = parseInt(size.slice(1));
      return `${base}${size}${src} ${width}w`;
    });

  const srcset = [...srcs, `${base}original${src} 1920w`].join(", ");

  return <img srcset={srcset} {...props} />;
}

export function getTmdbImgUrl(
  src: string,
  type: TmdbImgType,
  config: TmdbConfiguration,
  size: "original" | number,
) {
  const base = config.images.secure_base_url;
  const sizes = config.images[`${type}_sizes`];

  if (size === "original") {
    return `${base}${size}${src}`;
  }

  if (sizes.includes(`w${size}`)) {
    return `${base}w${size}${src}`;
  }

  return undefined;
}
