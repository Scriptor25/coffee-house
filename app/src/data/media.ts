import { resource } from "@runtime/resource";
import { getAllEntities, getEntityById } from "./api";

export interface Media {
  id: string;
  title: string;
  path: string;
}

export const getAllMedia = resource.cache(async (): Promise<Media[]> => {
  return getAllEntities("media");
});

export const getMediaById = resource.cache(
  async (id: string): Promise<Media> => {
    return getEntityById("media", id);
  },
);
