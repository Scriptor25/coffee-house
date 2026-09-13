import { getAllEntities, getEntityById } from "./api";

export interface Movie {
  id: string;
  title: string;
  poster?: string;
  backdrop?: string;
  description?: string;
}

export async function getAllMovies(): Promise<Movie[]> {
  return getAllEntities("movie");
}

export async function getMovieById(id: string): Promise<Movie> {
  return getEntityById("movie", id);
}
