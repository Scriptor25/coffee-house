import { track } from "./internal";
import type { Readable } from "./readable";

export class Signal<T> implements Readable<T> {
  private readonly subscribers = new Set<() => void>();

  constructor(private value: T) {}

  get(): T {
    track(this);

    return this.value;
  }

  set(next: T | ((prev: T) => T)): void {
    const value =
      typeof next === "function" ? (next as (prev: T) => T)(this.value) : next;

    if (Object.is(this.value, value)) {
      return;
    }

    this.value = value;

    for (const subscriber of this.subscribers) {
      subscriber();
    }
  }

  subscribe(listener: () => void): () => void {
    this.subscribers.add(listener);

    return () => {
      this.subscribers.delete(listener);
    };
  }
}

export function signal<T>(init: T): Signal<T> {
  return new Signal<T>(init);
}
