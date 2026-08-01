import {fetchAPI} from "./api.js"
import {buildTree, DirectoryNode, getCommonBase, MediaNode, segments} from "./tree.js"

async function render() {
    const session = localStorage.getItem("session")

    const loginEl = document.getElementById("login")
    const formEl = document.getElementById("form")

    const mediaEl = document.getElementById("media")
    const playlistEl = document.getElementById("playlist")
    const listEl = document.getElementById("list")

    loginEl.style.display = "none"
    mediaEl.style.display = "none"

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

        let node = buildTree(items, base)
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
                    const listItemEl = document.createElement("li")
                    const anchorEl = document.createElement("a")
                    if (node instanceof MediaNode) {
                        anchorEl.href = encodeURI(`/media/stream/${node.item.id}?token=${session}`)
                    } else {
                        anchorEl.href = encodeURI(`/#${slug.length ? "/" + slug.join("/") : ""}/${node.name}`)
                    }
                    anchorEl.append(node.name)
                    listItemEl.append(anchorEl)
                    return listItemEl
                })

            if (slug.length) {
                const target = slug.slice(0, -1)

                const listItemEl = document.createElement("li")
                const anchorEl = document.createElement("a")
                anchorEl.href = encodeURI(`/#${target.length ? "/" + target.join("/") : ""}`)
                anchorEl.append("..")
                listItemEl.append(anchorEl)
                listItemEls.unshift(listItemEl)
            }

            listEl.replaceChildren(...listItemEls)

            const playlist = sorted
                .map(item => {
                    if (!(item instanceof MediaNode)) return
                    return item.item
                })
                .filter(item => !!item)

            const lines = playlist.flatMap(item => {
                const url = new URL(
                    encodeURI(`/media/stream/${item.id}?token=${session}`),
                    window.location.origin,
                )
                return [`#EXTINF:0,${item.title}`, url.toString()]
            })

            const data = `#EXTM3U\r\n${lines.join("\r\n")}`

            playlistEl.addEventListener("click", async () => {
                const blob = new Blob([data], {type: "application/x-mpegurl"})
                const objectURL = URL.createObjectURL(blob)

                const anchorEl = document.createElement("a")
                anchorEl.href = objectURL
                anchorEl.download = "playlist.m3u8"
                anchorEl.click()
                URL.revokeObjectURL(objectURL)
            })
        }

        mediaEl.style.display = "block"
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

    loginEl.style.display = "block"
}

render().then()
