import { track } from "./internal";
import type { Readable } from "./readable";

type NonFunction<T> = T extends (...args: any[]) => any ? never : T;

type GeneratorFn<T> = (_: T) => T;
type Generator<T> = T | GeneratorFn<T>;

export class Signal<T> implements Readable<T> {
  private readonly subscribers = new Set<() => void>();

  constructor(private value: NonFunction<T>) {}

  get(): T {
    track(this);

    return this.value;
  }

  set(next: Generator<NonFunction<T>>): void {
    const value =
      typeof next === "function"
        ? (next as GeneratorFn<NonFunction<T>>)(this.value)
        : next;

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

export function signal<T>(init: NonFunction<T>): Signal<T> {
  return new Signal<T>(init);
}
