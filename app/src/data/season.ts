import type { ImageData } from "../component/image/image";
import { createEntityPlayback, getAllEntities, getEntityById } from "./api";

export interface Season {
  id: string;
  title: string;
  description?: string;
  poster: ImageData[];
  episodes: string[];
}

export async function getAllSeasons(): Promise<Season[]> {
  return getAllEntities("season");
}

export async function getSeasonById(id: string): Promise<Season> {
  return getEntityById("season", id);
}

export async function createSeasonPlayback(id: string): Promise<string> {
  return createEntityPlayback("season", id);
}
