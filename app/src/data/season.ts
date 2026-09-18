import { resource } from "@runtime/resource";
import type { ImageData } from "../component/image/image";
import { createEntityPlayback, getAllEntities, getEntityById } from "./api";

export interface Season {
  id: string;
  title: string;
  description?: string;
  poster: ImageData[];
  episodes: string[];
}

export const getAllSeasons = resource.cache(async (): Promise<Season[]> => {
  return getAllEntities("season");
});

export const getSeasonById = resource.cache(
  async (id: string): Promise<Season> => {
    return getEntityById("season", id);
  },
);

export const createSeasonPlayback = async (id: string): Promise<string> => {
  return createEntityPlayback("season", id);
};
