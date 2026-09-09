import { fetchData } from "./api";

export interface Media {
  id: string;
  title: string;
  path: string;
}

export async function getAllMedia(): Promise<Media[] | null> {
  try {
    const response = await fetchData("/media/list", { method: "post", body: JSON.stringify({}) });

    if (!response.ok) {
      return null;
    }

    return response.json();
  } catch (e) {
    console.warn(e);
    return null;
  }
}
