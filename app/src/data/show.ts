import { getAllEntities, getEntityById } from "./api";

export interface Show {
  id: string;
}

export async function getAllShows(): Promise<Show[]> {
  return getAllEntities("show");
}

export async function getShowById(id: string): Promise<Show> {
  return getEntityById("show", id);
}
