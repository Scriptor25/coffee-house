import { fetchData } from "./api";

export interface Media {
  id: string;
  title: string;
  path: string;
}

export async function getAllMedia(token: string): Promise<Media[] | null> {
  try {
    const response = await fetchData(
      "/media",
      {
        method: "get",
      },
      token,
    );

    if (!response.ok) {
      return null;
    }

    return response.json();
  } catch (e) {
    console.warn(e);
    return null;
  }
}
