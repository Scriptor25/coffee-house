export interface Readable<T = unknown> {
  get(): T;
  subscribe(listener: () => void): () => void;
}

interface Observer {
  on(src: Readable): void;
}

let active: Observer | undefined;

function track(src: Readable): void {
  active?.on(src);
}

class ReactiveObserver implements Observer {
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

    if (this.dirty) {
      this.compute();
    }

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
    this.observer.run(() => {
      this.value = this.fn();
    });

    this.dirty = false;
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

export function effect(fn: () => void): () => void {
  const observer = new ReactiveObserver(() => {
    observer.run(fn);
  });

  observer.run(fn);

  return () => {
    observer.dispose();
  };
}

export function isReadable(value: unknown): value is Readable {
  return value instanceof Signal || value instanceof Computed;
}
