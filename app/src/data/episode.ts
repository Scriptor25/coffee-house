import { resource } from "@runtime/resource";
import type { ImageData } from "../component/image/image";
import { createEntityPlayback, getAllEntities, getEntityById } from "./api";

export interface Episode {
  id: string;
  item: string;
  title: string;
  description?: string;
  still: ImageData[];
}

export const getAllEpisodes = resource.cache(async (): Promise<Episode[]> => {
  return getAllEntities("episode");
});

export const getEpisodeById = resource.cache(
  async (id: string): Promise<Episode> => {
    return getEntityById("episode", id);
  },
);

export const createEpisodePlayback = async (id: string): Promise<string> => {
  return createEntityPlayback("episode", id);
};
