export interface Readable<T = unknown> {
  get(): T;
  subscribe(listener: () => void): () => void;
}

export function isReadable(value: unknown): value is Readable {
  return (
    typeof value === "object" &&
    value !== null &&
    "get" in value &&
    "subscribe" in value &&
    typeof value.get === "function" &&
    typeof value.subscribe === "function" &&
    value.get.length === 0 &&
    value.subscribe.length === 1
  );
}
