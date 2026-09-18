import { untracked } from "./internal";
import type { Readable } from "./readable";
import { isReadable } from "./readable";
import type { Signal } from "./signal";

export type Key = number | bigint | string | symbol;

export type VNodeBase =
  | undefined
  | null
  | boolean
  | number
  | bigint
  | string
  | Node
  | Readable<VNode>;
export type VNode = VNodeBase | VNode[];

export type Component<P extends {}> = (props: P) => VNode;

type Dispose = () => void;

interface State {
  connected: boolean;

  connect(): void;
  disconnect(): void;
}

const context = new WeakMap<Node, State>();

function connectTree(node: Node) {
  const state = context.get(node);

  if (state && !state.connected) {
    state.connected = true;
    state.connect();
  }

  for (const child of node.childNodes) {
    connectTree(child);
  }
}

function disconnectTree(node: Node) {
  const state = context.get(node);

  if (state && state.connected) {
    state.connected = false;
    state.disconnect();
  }

  for (const child of node.childNodes) {
    disconnectTree(child);
  }
}

function isArray(x: unknown): x is unknown[] {
  return Array.isArray(x);
}

function insertReadable(
  node: Node,
  children: Readable<VNode>,
  before: Node | null,
): Dispose {
  const beg = document.createComment("");
  const end = document.createComment("");

  node.insertBefore(beg, before);
  node.insertBefore(end, before);

  let disposers: Dispose[] = [];

  const clear = () => {
    for (const dispose of disposers) {
      dispose();
    }

    disposers = [];

    let ptr = beg.nextSibling;
    while (ptr && ptr !== end) {
      const next = ptr.nextSibling;
      ptr.remove();
      ptr = next;
    }
  };

  const update = () => {
    clear();

    const dispose = insert(node, children.get(), end);

    if (dispose) {
      disposers.push(dispose);
    }
  };

  untracked(() => update());

  const unsubscribe = children.subscribe(update);

  return () => {
    unsubscribe();
    clear();

    beg.remove();
    end.remove();
  };
}

function insert(
  node: Node,
  children: VNode,
  before: Node | null = null,
): Dispose {
  if (isArray(children)) {
    const disposers: Dispose[] = [];

    for (const item of children) {
      const dispose = insert(node, item, before);

      if (dispose) {
        disposers.push(dispose);
      }
    }

    return () => {
      for (const dispose of disposers) {
        dispose();
      }
    };
  }

  if (
    children === undefined ||
    children === null ||
    children === false ||
    children === true ||
    children === ""
  ) {
    return () => {};
  }

  if (children instanceof Node) {
    node.insertBefore(children, before);
    connectTree(children);
    return () => {
      disconnectTree(children);
    };
  }

  if (isReadable(children)) {
    return insertReadable(node, children, before);
  }

  const text = document.createTextNode(String(children));
  node.insertBefore(text, before);
  return () => {};
}

function setReadableProperty(
  node: Element,
  key: string,
  value: Readable,
): Dispose {
  const update = () => {
    setStaticProperty(node, key, value.get());
  };

  untracked(() => update());

  return value.subscribe(update);
}

function setStaticProperty(node: Element, key: string, value: unknown) {
  if (key in node) {
    (node as Element & { [key]: unknown })[key] = value;
    return;
  }

  if (value === undefined || value === null) {
    node.removeAttribute(key);
    return;
  }

  switch (typeof value) {
    case "string":
    case "number":
    case "bigint":
      node.setAttribute(key, String(value));
      break;
    case "boolean":
      node.toggleAttribute(key, value);
      break;
    default:
      console.warn(`can't handle attribute '${key}' of type '${typeof value}'`);
      break;
  }
}

function setProperty(node: Element, key: string, value: unknown): Dispose {
  if (isReadable(value)) {
    return setReadableProperty(node, key, value);
  }

  setStaticProperty(node, key, value);
  return () => {};
}

export function jsx<
  P extends { ref?: Signal<HTMLElement | null>; children?: VNode },
>(tag: string | Component<P>, props?: P, _key?: Key): VNode {
  props ??= {} as P;

  if (typeof tag === "string") {
    const node = document.createElement(tag);

    const { ref, children, ...rest } = props;

    const disposers: Dispose[] = [];

    for (const [key, value] of Object.entries(rest)) {
      disposers.push(setProperty(node, key, value));
    }

    disposers.push(insert(node, children));

    const connect = () => {
      ref?.set(node);
    };

    const disconnect = () => {
      ref?.set(null);

      for (const dispose of disposers) {
        dispose();
      }
    };

    context.set(node, {
      connected: false,
      connect,
      disconnect,
    });

    return node;
  }

  return tag(props);
}

export const jsxs = jsx;

export function Fragment(props: { children?: VNode }): VNode {
  return props.children;
}

export const render = (root: Node, children: VNode) => insert(root, children);

export type HTMLElementProps<T extends HTMLElement> = {
  [K in keyof Omit<T, "key" | "children">]?: T[K] | Readable<T[K]>;
} & {
  key?: Key;
  ref?: Signal<T | null>;
  children?: VNode;
};

export namespace JSX {
  export type IntrinsicElements = {
    [K in keyof HTMLElementTagNameMap]: HTMLElementProps<
      HTMLElementTagNameMap[K]
    >;
  };
}
