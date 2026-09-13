import type { ImageData } from "../component/image/image";
import { createEntityPlayback, getAllEntities, getEntityById } from "./api";

export interface Movie {
  id: string;
  title: string;
  description?: string;
  poster: ImageData[];
  backdrop: ImageData[];
  items: string[];
}

export async function getAllMovies(
  limit?: number,
  offset?: number,
): Promise<Movie[]> {
  return getAllEntities("movie", limit, offset);
}

export async function getMovieById(id: string): Promise<Movie> {
  return getEntityById("movie", id);
}

export async function createMoviePlayback(id: string): Promise<string> {
  return createEntityPlayback("movie", id);
}
