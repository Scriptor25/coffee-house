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
    this.request = null;
    this.state.set({ status: "none" });
  }
}

export function resource<T>(fn: () => Promise<T>): Resource<T> {
  return new Resource<T>(fn);
}

interface CacheNode {
  primitive: Map<unknown, CacheNode>;
  object: WeakMap<object, CacheNode>;
  resource?: Resource<any>;
}

function createCacheNode(): CacheNode {
  return {
    primitive: new Map(),
    object: new WeakMap(),
  };
}

function isObject(value: unknown): value is object {
  return (
    (typeof value === "object" && value !== null) || typeof value === "function"
  );
}

function getChild(node: CacheNode, key: unknown): CacheNode | undefined {
  return isObject(key) ? node.object.get(key) : node.primitive.get(key);
}

function setChild(node: CacheNode, key: unknown, child: CacheNode) {
  if (isObject(key)) {
    node.object.set(key, child);
  } else {
    node.primitive.set(key, child);
  }
}

class ResourceCache {
  private readonly factories = new WeakMap<Function, CacheNode>();

  get<A extends unknown[], T>(
    factory: (...args: A) => Promise<T>,
    args: A,
  ): Resource<T> {
    let root = this.factories.get(factory);

    if (!root) {
      root = createCacheNode();
      this.factories.set(factory, root);
    }

    let node = root;

    for (const key of args) {
      let child = getChild(node, key);

      if (!child) {
        child = createCacheNode();
        setChild(node, key, child);
      }

      node = child;
    }

    if (!node.resource) {
      node.resource = new Resource(() => factory(...args));
    }

    return node.resource;
  }
}

let globalCache = new ResourceCache();

resource.cache = function cache<A extends unknown[], T>(
  fn: (...args: A) => Promise<T>,
): (...args: A) => Resource<T> {
  return (...args: A) => {
    return globalCache.get(fn, args);
  };
};
