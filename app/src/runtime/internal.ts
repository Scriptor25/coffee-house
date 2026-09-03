import type { Readable } from "./readable";

export interface Observer {
  on(src: Readable): void;
}

export class ReactiveObserver implements Observer {
  private readonly dependencies = new Map<Readable, () => void>();
  private readonly next = new Set<Readable>();
  private disposed = false;

  constructor(private readonly invalidate: () => void) {}

  on(src: Readable): void {
    this.next.add(src);
  }

  run(fn: () => void): void {
    if (this.disposed) return;

    this.next.clear();

    const previous = active;
    active = this;

    try {
      fn();
    } finally {
      active = previous;
    }

    this.update();
  }

  dispose(): void {
    if (this.disposed) return;

    this.disposed = true;

    for (const unsubscribe of this.dependencies.values()) {
      unsubscribe();
    }

    this.dependencies.clear();
    this.next.clear();
  }

  private update(): void {
    for (const [src, unsubscribe] of this.dependencies) {
      if (!this.next.has(src)) {
        unsubscribe();
        this.dependencies.delete(src);
      }
    }

    for (const src of this.next) {
      if (this.dependencies.has(src)) {
        continue;
      }

      const unsubscribe = src.subscribe(() => {
        this.invalidate();
      });

      this.dependencies.set(src, unsubscribe);
    }
  }
}

let active: Observer | undefined;

export function track(src: Readable): void {
  active?.on(src);
}
