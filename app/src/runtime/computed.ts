import { ReactiveObserver, track } from "./internal";
import type { Readable } from "./readable";

export class Computed<T> implements Readable<T> {
  private value!: T;
  private dirty = true;

  private readonly subscribers = new Set<() => void>();

  private readonly observer = new ReactiveObserver(() => {
    this.invalidate();
  });

  constructor(private readonly fn: () => T) {}

  get(): T {
    track(this);

    this.compute();

    return this.value;
  }

  subscribe(listener: () => void): () => void {
    if (this.subscribers.size === 0) {
      this.compute();
    }

    this.subscribers.add(listener);

    return () => {
      this.subscribers.delete(listener);
    };
  }

  private compute(): void {
    if (!this.dirty) return;
    this.dirty = false;

    this.observer.run(() => {
      this.value = this.fn();
    });
  }

  private invalidate(): void {
    if (this.dirty) return;

    this.dirty = true;

    for (const subscriber of this.subscribers) {
      subscriber();
    }
  }
}

export function computed<T>(fn: () => T): Computed<T> {
  return new Computed<T>(fn);
}
