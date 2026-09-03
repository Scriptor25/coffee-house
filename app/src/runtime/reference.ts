import { signal } from "./signal";

export function reference<T extends HTMLElement>() {
  return signal<T | null>(null);
}
