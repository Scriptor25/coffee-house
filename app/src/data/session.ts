import { fetchData } from "./api";

export async function createSession(
  username: string,
  password: string,
): Promise<string | null> {
  try {
    const response = await fetchData("/session", {
      method: "post",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ username, password }),
    });

    if (!response.ok) {
      return null;
    }

    return response.text();
  } catch (e) {
    console.warn(e);
    return null;
  }
}
