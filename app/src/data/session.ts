import { fetchData } from "./api";

export function getSessionToken(): string | null {
  return window.localStorage.getItem("session");
}

export function setSessionToken(token: string | null) {
  if (token) {
    window.localStorage.setItem("session", token);
  } else {
    window.localStorage.removeItem("session");
  }
}

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
