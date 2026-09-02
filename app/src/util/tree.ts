import type { Media } from "../data/media";

export class FileNode {
  constructor(readonly name: string) {}
}

export class DirectoryNode extends FileNode {
  constructor(
    override readonly name: string,
    readonly children: FileNode[],
  ) {
    super(name);
  }
}

export class MediaNode extends FileNode {
  constructor(readonly item: Media) {
    super(item.title);
  }
}

export function segments(str: string): string[] {
  return str.split("/").filter((item) => !!item);
}

export function normalize(path: string): string[] {
  const stack: string[] = [];

  for (const segment of segments(path)) {
    if (segment === "..") {
      if (stack.length) stack.pop();
    } else {
      stack.push(segment);
    }
  }

  return stack;
}

export function relative(from: string, to: string): string {
  const a = normalize(from);
  const b = normalize(to);

  let i = 0;
  for (; i < a.length && i < b.length && a[i] === b[i]; ++i) {}

  const result = [
    ...Array.from({ length: a.length - i }, () => ".."),
    ...b.slice(i),
  ];

  const absolute = a.length === i;

  return result.length
    ? (absolute ? "/" : "") + result.join("/")
    : absolute
      ? "/"
      : ".";
}

export function getCommonBase(items: Media[]): string {
  const directories = items.map((item) => segments(item.path));

  const common = [];

  for (let i = 0; ; ++i) {
    const segment = directories[0]?.[i];
    if (segment === undefined) break;
    if (!directories.every((item) => item[i] === segment)) break;
    common.push(segment);
  }

  return "/" + common.join("/");
}

export function buildTree(items: Media[], base: string): DirectoryNode {
  const tree = new DirectoryNode("", []);

  for (const item of items) {
    let current = tree;
    const rel = segments(relative(base, item.path));

    for (let i = 0; i < rel.length - 1; ++i) {
      const name = rel[i];

      let child =
        current.children
          .filter((node) => node instanceof DirectoryNode)
          .find((node) => node.name === name) ?? null;

      if (child === null) {
        child = new DirectoryNode(name ?? "", []);
        current.children.push(child);
      }

      current = child;
    }

    current.children.push(new MediaNode(item));
  }

  return tree;
}
