import { fetchData } from "./api";

export interface Media {
  id: string;
  title: string;
  thumbnail?: string;
  path: string;
}

export async function getAllMedia(): Promise<Media[]> {
  const response = await fetchData("/media/list", { method: "post", body: JSON.stringify({}) });

  if (!response.ok) {
    throw new Error(
      `failed to get all media: ${response.status} ${response.statusText}`,
    );
  }

  return response.json();
}

export async function getMediaById(id: string): Promise<Media> {
  const response = await fetchData(`/media/${id}`, { method: "get" });

  if (!response.ok) {
    throw new Error(
      `failed to get media by id ${id}: ${response.status} ${response.statusText}`,
    );
  }

  return response.json();
}
