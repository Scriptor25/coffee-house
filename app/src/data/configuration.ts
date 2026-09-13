import { fetchData } from "./api";

export interface TmdbConfiguration {
  change_keys: string[];
  images: {
    base_url: string;
    secure_base_url: string;
    backdrop_sizes: string[];
    logo_sizes: string[];
    poster_sizes: string[];
    profile_sizes: string[];
    still_sizes: string[];
  };
}

export async function getTmdbConfiguration(): Promise<TmdbConfiguration> {
  const response = await fetchData("/configuration/tmdb", { method: "get" });

  if (!response.ok) {
    throw new Error(
      `failed to get tmdb configuration: ${response.status} ${response.statusText}`,
    );
  }

  return response.json();
}
