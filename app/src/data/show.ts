import { resource } from "@runtime/resource";
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

export const getAllShows = resource.cache(
  async (limit?: number, offset?: number): Promise<Show[]> => {
    return getAllEntities("show", limit, offset);
  },
);

export const getShowById = resource.cache(async (id: string): Promise<Show> => {
  return getEntityById("show", id);
});
