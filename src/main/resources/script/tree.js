/**
 * @interface  Media
 * @property {string} id
 * @property {string} path
 * @property {number} size
 * @property {string} title
 * @property {string} created_at
 * @property {string} modified_at
 * @property {number} duration
 */

/**
 * @property {string} name
 */
export class FileNode {

    /**
     * @param {string} name
     */
    constructor(name) {
        this.name = name
    }
}

export class DirectoryNode extends FileNode {

    /**
     * @param {string} name
     * @param {FileNode[]} children
     */
    constructor(name, children) {
        super(name)
        this.children = children
    }
}

/**
 * @property {Media} item
 */
export class MediaNode extends FileNode {

    /**
     * @param {Media} item
     */
    constructor(item) {
        super(item.title)
        this.item = item
    }
}

/**
 * @param {string} str
 * @returns {string[]}
 */
export function segments(str) {
    return str.split("/").filter(item => !!item)
}

/**
 * @param {string} path
 * @returns {string[]}
 */
export function normalize(path) {
    /** @type {string[]} */
    const stack = []

    for (const segment of segments(path)) {
        if (segment === "..") {
            if (stack.length) stack.pop()
        } else {
            stack.push(segment)
        }
    }

    return stack
}

/**
 * @param {string} from
 * @param {string} to
 * @returns {string}
 */
export function relative(from, to) {
    const a = normalize(from)
    const b = normalize(to)

    let i = 0
    for (; i < a.length && i < b.length && a[i] === b[i]; ++i) {
    }

    const result = [
        ...Array.from({length: a.length - i}, () => ".."),
        ...b.slice(i),
    ]

    const absolute = a.length === i

    return result.length ? ((absolute ? "/" : "") + result.join("/")) : (absolute ? "/" : ".")
}

/**
 * @param {Media[]} items
 * @returns {string}
 */
export function getCommonBase(items) {
    const directories = items.map(item => segments(item.path))

    const common = []

    for (let i = 0; ; ++i) {
        const segment = directories[0][i]
        if (segment === undefined) break
        if (!directories.every(item => item[i] === segment)) break
        common.push(segment)
    }

    return "/" + common.join("/")
}

/**
 * @param {Media[]} items
 * @param {string} base
 * @returns {DirectoryNode}
 */
export function buildTree(items, base) {
    const tree = new DirectoryNode("", [])

    for (const item of items) {
        let current = tree
        const rel = segments(relative(base, item.path))

        for (let i = 0; i < rel.length - 1; ++i) {
            const name = rel[i]

            let child = current.children
                .filter((node) => node instanceof DirectoryNode)
                .find((node) => node.name === name) ?? null

            if (child === null) {
                child = new DirectoryNode(name, [])
                current.children.push(child)
            }

            current = child
        }

        current.children.push(new MediaNode(item))
    }

    return tree
}
