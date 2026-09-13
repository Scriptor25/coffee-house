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

export async function getAllEntities<T>(resource: string): Promise<T[]> {
  const response = await fetchData(`/${resource}/list`, {
    method: "post",
    body: JSON.stringify({}),
  });

  if (!response.ok) {
    throw new Error(
      `failed to get all ${resource}: ${response.status} ${response.statusText}`,
    );
  }

  return response.json();
}

export async function getEntityById<T>(
  resource: string,
  id: string,
): Promise<T> {
  const response = await fetchData(`/${resource}/${id}`, { method: "get" });

  if (!response.ok) {
    throw new Error(
      `failed to get ${resource} by id ${id}: ${response.status} ${response.statusText}`,
    );
  }

  return response.json();
}
