import { getAllEntities, getEntityById } from "./api";

export interface Season {
  id: string;
}

export async function getAllSeasons(): Promise<Season[]> {
  return getAllEntities("season");
}

export async function getSeasonById(id: string): Promise<Season> {
  return getEntityById("season", id);
}
