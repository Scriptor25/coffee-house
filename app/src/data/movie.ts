import { resource } from "@runtime/resource";
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

export const getAllMovies = resource.cache(
  async (limit?: number, offset?: number): Promise<Movie[]> => {
    return getAllEntities("movie", limit, offset);
  },
);

export const getMovieById = resource.cache(
  async (id: string): Promise<Movie> => {
    return getEntityById("movie", id);
  },
);

export const createMoviePlayback = async (id: string): Promise<string> => {
  return createEntityPlayback("movie", id);
};
