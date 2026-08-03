import {fetchAPI} from "./api.js"
import {buildTree, DirectoryNode, getCommonBase, MediaNode, segments} from "./tree.js"

const listItemT = document.getElementById("list-item")

const loginSectionEl = document.getElementById("login")
const formEl = document.getElementById("form")

const mediaSectionEl = document.getElementById("media")
const playlistButtonEl = document.getElementById("playlist")
const listEl = document.getElementById("list")

/**
 * @param {string} content
 * @param {string} href
 * @returns {HTMLElement}
 */
function createListItem(content, href) {
    const listItemEl = document.importNode(listItemT.content, true)

    const anchorEl = listItemEl.querySelector("a")
    anchorEl.href = href
    anchorEl.innerText = content

    return listItemEl
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
                    const uri = (node instanceof MediaNode)
                        ? `/media/stream/${node.item.id}/master.m3u8?token=${session}`
                        : `/#${slug.length ? "/" + slug.join("/") : ""}/${node.name}`
                    return createListItem(node.name, encodeURI(uri))
                })

            if (slug.length) {
                const target = slug.slice(0, -1)

                const uri = `/#${target.length ? "/" + target.join("/") : ""}`
                const listItemEl = createListItem("..", encodeURI(uri))
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

            const lines = playlist.flatMap(item => {
                const url = new URL(
                    encodeURI(`/media/stream/${item.id}/master.m3u8?token=${session}`),
                    window.location.origin,
                )
                return [`#EXTINF:-1,${item.title}`, url.toString()]
            })

            const data = `#EXTM3U\r\n${lines.join("\r\n")}`

            playlistButtonEl.onclick = async () => {
                const blob = new Blob([data], {type: "application/x-mpegurl"})
                const objectURL = URL.createObjectURL(blob)

                const anchorEl = document.createElement("a")
                anchorEl.href = objectURL
                anchorEl.download = "playlist.m3u8"
                anchorEl.click()
                URL.revokeObjectURL(objectURL)
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
