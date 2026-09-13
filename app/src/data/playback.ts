import { fetchData } from "./api";

export async function createPlayback(
  name: string,
  items: string[],
): Promise<string | null> {
  try {
    const response = await fetchData("/playback", {
      method: "post",
      body: JSON.stringify({ name, items }),
    });

    if (!response.ok) {
      return null;
    }

    return `/playback/${await response.text()}`;
  } catch (e) {
    console.warn(e);
    return null;
  }
}

export async function sharePlayback(
  title: string,
  create: () => Promise<string>,
  index?: number,
) {
  const id = await create();
  const url =
    typeof index === "number"
      ? `${window.location.origin}/playback/${id}/${index}/master.m3u8`
      : `${window.location.origin}/playback/${id}/playlist.m3u8`;

  if (window.navigator.share) {
    await window.navigator.share({
      title,
      url,
    });
  } else if (window.navigator.clipboard) {
    await window.navigator.clipboard.writeText(url);
  } else {
    console.warn(
      "no method for sharing the playlist url available, opening in new window",
    );
    window.open(url);
  }
}
