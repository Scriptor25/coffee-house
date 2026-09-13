import { getAllEntities, getEntityById } from "./api";

export interface Media {
  id: string;
  title: string;
  path: string;
}

export async function getAllMedia(): Promise<Media[]> {
  return getAllEntities("media");
}

export async function getMediaById(id: string): Promise<Media> {
  return getEntityById("media", id);
}
