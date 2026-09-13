import type { ImageData } from "../component/image/image";
import { getAllEntities, getEntityById } from "./api";

export interface Show {
  id: string;
  title: string;
  description?: string;
  poster: ImageData[];
  backdrop: ImageData[];
  seasons: string[];
}

export async function getAllShows(
  limit?: number,
  offset?: number,
): Promise<Show[]> {
  return getAllEntities("show", limit, offset);
}

export async function getShowById(id: string): Promise<Show> {
  return getEntityById("show", id);
}
