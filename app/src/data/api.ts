import { getOrigin } from "../util/origin";

export async function fetchData(
  resource: string,
  init?: RequestInit,
  token?: string,
) {
  return fetch(new URL(resource, getOrigin()), {
    ...init,
    headers: token
      ? {
          ...init?.headers,
          authorization: `Bearer ${token}`,
        }
      : init?.headers,
  });
}
