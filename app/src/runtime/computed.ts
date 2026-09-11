import { ReactiveObserver, track, untracked } from "./internal";
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

    return this.compute();
  }

  subscribe(listener: () => void): () => void {
    this.subscribers.add(listener);

    return () => {
      this.subscribers.delete(listener);
    };
  }

  dispose(): void {
    this.observer.dispose();
  }

  private compute(): T {
    if (!this.dirty) {
      return this.value;
    }

    this.dirty = false;

    this.observer.run(() => {
      this.value = this.fn();
    });

    return this.value;
  }

  private invalidate(): void {
    if (this.dirty) return;

    this.dirty = true;

    untracked(() => {
      for (const subscriber of this.subscribers) {
        subscriber();
      }
    });
  }
}

export function computed<T>(fn: () => T): Computed<T> {
  return new Computed<T>(fn);
}
