import type { Readable } from "./readable";
import { signal } from "./signal";

export type ResourceState<T> =
  | { status: "none" }
  | { status: "pending" }
  | { status: "success"; data: T }
  | { status: "error"; error: unknown };

export class Resource<T> implements Readable<ResourceState<T>> {
  private readonly state = signal<ResourceState<T>>({ status: "none" });

  private request: Promise<T> | null = null;

  constructor(private readonly fn: () => Promise<T>) {}

  get(): ResourceState<T> {
    return this.state.get();
  }

  subscribe(listener: () => void): () => void {
    return this.state.subscribe(listener);
  }

  async load(): Promise<T> {
    if (this.request) {
      return this.request;
    }

    const current = this.state.get();

    if (current.status === "success") {
      return current.data;
    }

    this.state.set({ status: "pending" });

    this.request = this.fn()
      .then((data) => {
        this.state.set({ status: "success", data });
        return data;
      })
      .catch((error) => {
        this.state.set({ status: "error", error });
        throw error;
      })
      .finally(() => {
        this.request = null;
      });

    return this.request;
  }

  invalidate(): void {
    this.state.set({ status: "none" });
  }
}

export function resource<T>(fn: () => Promise<T>): Resource<T> {
  return new Resource<T>(fn);
}
