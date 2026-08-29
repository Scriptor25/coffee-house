import {createPlayback, fetchAPI} from "./api.js"
import {buildTree, DirectoryNode, getCommonBase, MediaNode, segments} from "./tree.js"

const loginSectionEl = document.getElementById("login")
const formEl = document.getElementById("form")

const mediaSectionEl = document.getElementById("media")
const playlistDirectButtonEl = document.getElementById("playlist-direct")
const playlistHLSButtonEl = document.getElementById("playlist-hls")
const listEl = document.getElementById("list")

/**
 * @param {string} content
 * @param {string} href
 * @returns {HTMLElement}
 */
function createSimpleListItem(
    content,
    href,
) {
    const listItemEl = document.createElement("li")

    const anchorEl = document.createElement("a")
    anchorEl.innerText = content
    anchorEl.href = href

    listItemEl.appendChild(anchorEl)

    return listItemEl
}

/**
 * @param {MediaNode} node
 * @returns {HTMLElement}
 */
function createListItem(node) {

    /**
     * @param {boolean} direct
     * @returns {Promise<void>}
     */
    const copySingleUrl = async (direct) => {
        const base = await createPlayback(node.item.title, [node.item.id])
        if (!base) return null

        const pathname = direct ? `${base}/0` : `${base}/0/master.m3u8`
        const url = new URL(pathname, window.location.origin)

        await copyUrl(url)
    }

    const li = document.createElement("li")

    {
        const div = document.createElement("div")
        div.style = "display: inline-flex; flex-flow: row wrap; align-items: center; gap: 10px;"

        {
            const span = document.createElement("span")
            span.innerText = node.name

            div.appendChild(span)
        }

        {
            const button = document.createElement("button")
            button.innerText = "direct"
            button.onclick = () => copySingleUrl(true)

            div.appendChild(button)
        }

        {
            const button = document.createElement("button")
            button.innerText = "hls"
            button.onclick = () => copySingleUrl(false)

            div.appendChild(button)
        }

        li.appendChild(div)
    }

    return li
}

/**
 * @param {URL} url
 * @returns {Promise<void>}
 */
async function copyUrl(url) {
    return window.navigator.clipboard.writeText(url.toString())
}

async function render() {
    const token = window.localStorage.getItem("session")

    loginSectionEl.style.display = "none"
    mediaSectionEl.style.display = "none"

    if (token !== null) {
        const response = await fetchAPI("media", {method: "GET"})

        if (!response.ok) {
            console.log(response.status, response.statusText)
            window.localStorage.removeItem("session")

            render().then()
            return
        }

        const fragment = window.location.hash
        const slug = fragment.length ? segments(decodeURI(fragment.slice(1))) : []

        window.addEventListener("hashchange", event => {
            event.preventDefault()

            render().then()
        }, {once: true})

        /** @type {Media[]} */
        const items = await response.json()
        const base = getCommonBase(items)
        const tree = buildTree(items, base)

        let node = tree
        for (const segment of slug) {
            if (node === null || node.name === segment) break
            node = node.children
                .filter(node => node instanceof DirectoryNode)
                .find(node => node.name === segment) ?? null
        }

        if (node !== null) {
            const sorted = node.children
                .toSorted((a, b) => a.name.localeCompare(b.name))

            const listItems = sorted
                .map(node => {
                    if (node instanceof MediaNode) {
                        return createListItem(node)
                    } else {
                        const uri = `/#${slug.length ? "/" + slug.join("/") : ""}/${node.name}`
                        return createSimpleListItem(node.name, encodeURI(uri))
                    }
                })

            if (slug.length) {
                const target = slug.slice(0, -1)

                const uri = `/#${target.length ? "/" + target.join("/") : ""}`
                const listItem = createSimpleListItem("..", encodeURI(uri))
                listItems.unshift(listItem)
            }

            listEl.replaceChildren(...listItems)

            /**
             * @param {boolean} direct
             * @returns {Promise<void>}
             */
            const copyPlaylistUrl = async (direct) => {
                const playlist = sorted
                    .filter(item => item instanceof MediaNode)
                    .map(/** @param {MediaNode} item */item => item.item.id)

                const base = await createPlayback(node.name, playlist)
                if (!base) return null

                const pathname = `${base}/playlist.m3u8?direct=${direct}`
                const url = new URL(pathname, window.location.origin)

                await copyUrl(url)
            }

            playlistDirectButtonEl.onclick = () => copyPlaylistUrl(true)
            playlistHLSButtonEl.onclick = () => copyPlaylistUrl(false)
        }

        mediaSectionEl.style.display = "block"
        return
    }

    formEl.addEventListener("submit", async event => {
        event.preventDefault()

        const data = new FormData(event.currentTarget, event.submitter)

        event.currentTarget.reset()

        const response = await fetchAPI("session", {
            method: "POST",
            body: JSON.stringify(Object.fromEntries(data)),
            headers: {"content-type": "application/json"},
        }, false)

        if (!response.ok) {
            console.log(response.status, response.statusText)
            window.localStorage.removeItem("session")
        } else {
            const data = await response.text()
            window.localStorage.setItem("session", data)
        }

        render().then()
    }, {once: true})

    loginSectionEl.style.display = "block"
}

render().then()
