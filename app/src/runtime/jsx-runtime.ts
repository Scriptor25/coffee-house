import { isReadable, type Readable } from "./readable";
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

export type Dispose = () => void;

function isArray(x: unknown): x is unknown[] {
  return Array.isArray(x);
}

function insertReadable(
  node: Node,
  child: Readable<VNode>,
  before: Node | null,
): Dispose {
  const beg = document.createComment("beg");
  const end = document.createComment("end");

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

    const dispose = insert(node, child.get(), end);

    if (dispose) {
      disposers.push(dispose);
    }
  };

  update();

  const unsubscribe = child.subscribe(update);

  return () => {
    unsubscribe();
    clear();

    beg.remove();
    end.remove();
  };
}

function insert(
  node: Node,
  child: VNode,
  before: Node | null = null,
): Dispose | undefined {
  if (isArray(child)) {
    const disposers: Dispose[] = [];

    for (const item of child) {
      const dispose = insert(node, item, before);

      if (dispose) {
        disposers.push(dispose);
      }
    }

    if (!disposers.length) {
      return;
    }

    return () => {
      for (const dispose of disposers) {
        dispose();
      }
    };
  }

  if (
    child === undefined ||
    child === null ||
    child === false ||
    child === true
  ) {
    return;
  }

  if (child instanceof Node) {
    node.insertBefore(child, before);
    return;
  }

  if (isReadable(child)) {
    return insertReadable(node, child, before);
  }

  const text = document.createTextNode(String(child));
  node.insertBefore(text, before);
  return;
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

function setReadableProperty(node: Element, key: string, value: Readable) {
  const update = () => {
    setStaticProperty(node, key, value.get());
  };

  update();

  value.subscribe(update);
}

function setProperty(node: Element, key: string, value: unknown) {
  if (isReadable(value)) {
    setReadableProperty(node, key, value);
    return;
  }

  setStaticProperty(node, key, value);
}

export function jsx<
  P extends { ref?: Signal<HTMLElement | null>; children?: VNode },
>(tag: string | Component<P>, props?: P, _key?: Key): VNode {
  props ??= {} as P;

  if (typeof tag === "string") {
    const node = document.createElement(tag);

    const { ref, children, ...rest } = props;

    if (ref) {
      const observer = new MutationObserver(() => {
        const connected = node.isConnected;
        ref.set(connected ? node : null);

        if (!connected) {
          observer.disconnect();
        }
      });

      observer.observe(document, {
        childList: true,
        subtree: true,
      });

      if (node.isConnected) {
        ref.set(node);
      }
    }

    for (const [key, value] of Object.entries(rest)) {
      setProperty(node, key, value);
    }

    insert(node, children);
    return node;
  }

  return tag(props);
}

export const jsxs = jsx;

export function Fragment(props: { children?: VNode }): VNode[] {
  return isArray(props.children) ? props.children : [props.children];
}

export const render = insert;

export type HTMLElementProps<T extends HTMLElement> = {
  [K in keyof Omit<T, "key" | "children">]?: T[K];
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
