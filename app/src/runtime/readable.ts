import { Computed } from "./computed";
import { Resource } from "./resource";
import { Signal } from "./signal";

export interface Readable<T = unknown> {
  get(): T;
  subscribe(listener: () => void): () => void;
}

export function isReadable(value: unknown): value is Readable {
  return (
    value instanceof Signal ||
    value instanceof Computed ||
    value instanceof Resource
  );
}
