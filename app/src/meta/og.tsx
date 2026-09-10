import { type VNode } from "@runtime/jsx-runtime";

export interface OGMetadataBase {
  url?: string;
  title?: string;
  image?: string;
}

export interface OGMetadataMusicSong extends OGMetadataBase {
  type: "music.song";
  duration?: number;
  // TODO
}

export interface OGMetadataMusicAlbum extends OGMetadataBase {
  type: "music.album";
  releaseDate?: string;
  // TODO
}

export interface OGMetadataMusicPlaylist extends OGMetadataBase {
  type: "music.playlist";
  // TODO
}

export interface OGMetadataMusicRadioStation extends OGMetadataBase {
  type: "music.radio_station";
  // TODO
}

export interface OGMetadataVideoMovie extends OGMetadataBase {
  type: "video.movie";
  // TODO
}

export interface OGMetadataVideoEpisode extends OGMetadataBase {
  type: "video.episode";
  // TODO
}

export interface OGMetadataVideoTVShow extends OGMetadataBase {
  type: "video.tv_show";
  // TODO
}

export interface OGMetadataVideoOther extends OGMetadataBase {
  type: "video.other";
  // TODO
}

export interface OGMetadataArticle extends OGMetadataBase {
  type: "article";
  // TODO
}

export interface OGMetadataBook extends OGMetadataBase {
  type: "book";
  // TODO
}

export interface OGMetadataPaymentLink extends OGMetadataBase {
  type: "payment.link";
  // TODO
}

export interface OGMetadataProfile extends OGMetadataBase {
  type: "profile";
  // TODO
}

export interface OGMetadataWebsite extends OGMetadataBase {
  type: "website";
  // TODO
}

export type Metadata =
  | OGMetadataMusicSong
  | OGMetadataMusicAlbum
  | OGMetadataMusicPlaylist
  | OGMetadataMusicRadioStation
  | OGMetadataVideoMovie
  | OGMetadataVideoEpisode
  | OGMetadataVideoTVShow
  | OGMetadataVideoOther
  | OGMetadataArticle
  | OGMetadataBook
  | OGMetadataPaymentLink
  | OGMetadataProfile
  | OGMetadataWebsite;

export function buildMetadata(metadata: Metadata) {
  const items: VNode[] = [];

  items.push(<meta name="og:type" content={metadata.type} />);

  if (metadata.url) {
    items.push(<meta name="og:url" content={metadata.url} />);
  }

  if (metadata.title) {
    items.push(<meta name="og:title" content={metadata.title} />);
  }

  if (metadata.image) {
    items.push(<meta name="og:image" content={metadata.image} />);
  }

  switch (metadata.type) {
    case "music.song":
      break;
    case "music.album":
      break;
    case "music.playlist":
      break;
    case "music.radio_station":
      break;
    case "video.movie":
      break;
    case "video.episode":
      break;
    case "video.tv_show":
      break;
    case "video.other":
      break;
    case "article":
      break;
    case "book":
      break;
    case "payment.link":
      break;
    case "profile":
      break;
    case "website":
      break;
  }

  return items;
}
