import { getOrigin } from "../util/origin";
import { getSessionToken, setSessionToken } from "./session";

export async function fetchData(resource: string, init?: RequestInit) {
  const token = getSessionToken();

  const response = await fetch(new URL(resource, getOrigin()), {
    ...init,
    headers: token
      ? {
          ...init?.headers,
          authorization: `Bearer ${token}`,
        }
      : init?.headers,
  });

  if (response.status === 401) {
    setSessionToken(null);
  }

  return response;
}
