import { fetchData } from "./api";

export async function createPlayback(
  token: string,
  name: string,
  items: string[],
): Promise<string | null> {
  try {
    const response = await fetchData(
      "playback",
      {
        method: "post",
        body: JSON.stringify({ name, items }),
      },
      token,
    );

    if (!response.ok) {
      return null;
    }

    return `/playback/${await response.text()}`;
  } catch (e) {
    console.warn(e);
    return null;
  }
}
