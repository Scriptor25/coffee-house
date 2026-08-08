import {fetchAPI} from "./api.js"
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
 * @param {string} content
 * @param {string} primaryText
 * @param {string} primaryHref
 * @param {string=} secondaryText
 * @param {string=} secondaryHref
 * @returns {HTMLElement}
 */
function createListItem(
    content,
    primaryText,
    primaryHref,
    secondaryText,
    secondaryHref,
) {
    const listItemEl = document.createElement("li")

    const spanEl = document.createElement("span")
    spanEl.innerText = content

    listItemEl.appendChild(spanEl)
    listItemEl.appendChild(document.createTextNode(" ("))

    const primaryAnchorEl = document.createElement("a")
    primaryAnchorEl.innerText = primaryText
    primaryAnchorEl.href = primaryHref

    listItemEl.appendChild(primaryAnchorEl)

    if (secondaryText && secondaryHref) {
        const secondaryAnchorEl = document.createElement("a")
        secondaryAnchorEl.innerText = secondaryText
        secondaryAnchorEl.href = secondaryHref

        listItemEl.appendChild(document.createTextNode(", "))
        listItemEl.appendChild(secondaryAnchorEl)
    }

    listItemEl.appendChild(document.createTextNode(")"))

    return listItemEl
}

/**
 * @param {Blob} blob
 * @param {string} name
 */
function downloadBlob(blob, name) {
    const objectURL = URL.createObjectURL(blob)

    const anchorEl = document.createElement("a")
    anchorEl.href = objectURL
    anchorEl.download = name
    anchorEl.click()

    URL.revokeObjectURL(objectURL)
}

async function render() {
    const session = localStorage.getItem("session")

    loginSectionEl.style.display = "none"
    mediaSectionEl.style.display = "none"

    if (session !== null) {
        const response = await fetchAPI("media", {method: "GET"})

        if (!response.ok) {
            console.log(response.status, response.statusText, await response.text())
            localStorage.removeItem("session")

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

            const listItemEls = sorted
                .map(node => {
                    if (node instanceof MediaNode) {
                        const directUri = `/media/stream/${node.item.id}?token=${session}`
                        const hlsUri = `/media/stream/${node.item.id}/master.m3u8?token=${session}`
                        return createListItem(
                            node.name,
                            "Direct",
                            encodeURI(directUri),
                            "HLS",
                            encodeURI(hlsUri),
                        )
                    } else {
                        const uri = `/#${slug.length ? "/" + slug.join("/") : ""}/${node.name}`
                        return createSimpleListItem(node.name, encodeURI(uri))
                    }
                })

            if (slug.length) {
                const target = slug.slice(0, -1)

                const uri = `/#${target.length ? "/" + target.join("/") : ""}`
                const listItemEl = createSimpleListItem("..", encodeURI(uri))
                listItemEls.unshift(listItemEl)
            }

            listEl.replaceChildren(...listItemEls)

            const playlist = sorted
                .filter(item => item instanceof MediaNode)
                .map(
                    /**
                     * @param {MediaNode} item
                     * @return {Media}
                     */
                    item => item.item)

            const directLines = playlist.flatMap(item => {
                const url = new URL(
                    encodeURI(`/media/stream/${item.id}?token=${session}`),
                    window.location.origin,
                )
                return [`#EXTINF:${item.duration},${item.title}`, url.toString()]
            })

            const hlsLines = playlist.flatMap(item => {
                const url = new URL(
                    encodeURI(`/media/stream/${item.id}/master.m3u8?token=${session}`),
                    window.location.origin,
                )
                return [`#EXTINF:${item.duration},${item.title}`, url.toString()]
            })

            const directPlaylist = `#EXTM3U\r\n#PLAYLIST:${node.name}\r\n${directLines.join("\r\n")}`
            const hlsPlaylist = `#EXTM3U\r\n#PLAYLIST:${node.name}\r\n${hlsLines.join("\r\n")}`

            playlistDirectButtonEl.onclick = async () => {
                const blob = new Blob([directPlaylist], {type: "application/x-mpegurl"})

                downloadBlob(blob, `${node.name}.m3u8`)
            }

            playlistHLSButtonEl.onclick = async () => {
                const blob = new Blob([hlsPlaylist], {type: "application/x-mpegurl"})

                downloadBlob(blob, `${node.name}.m3u8`)
            }
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
            console.log(response.status, response.statusText, await response.text())
            localStorage.removeItem("session")
        } else {
            /** @type {{token: string}} */
            const data = await response.json()
            localStorage.setItem("session", data.token)
        }

        render().then()
    }, {once: true})

    loginSectionEl.style.display = "block"
}

render().then()
