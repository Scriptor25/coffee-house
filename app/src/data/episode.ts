import type { ImageData } from "../component/image/image";
import { createEntityPlayback, getAllEntities, getEntityById } from "./api";

export interface Episode {
  id: string;
  item: string;
  title: string;
  description?: string;
  still: ImageData[];
}

export async function getAllEpisodes(): Promise<Episode[]> {
  return getAllEntities("episode");
}

export async function getEpisodeById(id: string): Promise<Episode> {
  return getEntityById("episode", id);
}

export async function createEpisodePlayback(id: string): Promise<string> {
  return createEntityPlayback("episode", id);
}
