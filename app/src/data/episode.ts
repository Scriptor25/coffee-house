import { getAllEntities, getEntityById } from "./api";

export interface Episode {
  id: string;
}

export async function getAllEpisodes(): Promise<Episode[]> {
  return getAllEntities("episode");
}

export async function getEpisodeById(id: string): Promise<Episode> {
  return getEntityById("episode", id);
}
