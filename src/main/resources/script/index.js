import {fetchAPI} from "./api.js";
import {buildTree, DirectoryNode, getCommonBase, MediaNode, segments} from "./tree.js"

async function render() {
    const session = localStorage.getItem("session")

    const loginForm = document.getElementById("login-form")
    const mediaList = document.getElementById("media-list")

    loginForm.style.display = "none"
    mediaList.style.display = "none"

    const endpoint = window.location.origin

    if (session !== null) {
        const response = await fetchAPI("media", {method: "GET"})

        if (response.ok) {
            const fragment = window.location.hash
            const slug = fragment.length ? segments(decodeURI(fragment.slice(1))) : []

            const listener = event => {
                event.preventDefault()

                window.removeEventListener("hashchange", listener)

                render().then()
            }

            window.addEventListener("hashchange", listener)

            /**
             * @type {Media[]}
             */
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
                const listItems = node.children.toSorted((a, b) => a.name.localeCompare(b.name)).map(node => {
                    const listItem = document.createElement("li")
                    const anchor = document.createElement("a")
                    if (node instanceof MediaNode) {
                        /**
                         * @type {MediaNode} node
                         */
                        anchor.href = encodeURI(`${endpoint}/media/stream/${node.item.id}?token=${session}`)
                    } else {
                        anchor.href = encodeURI(`${endpoint}/#${slug.length ? "/" + slug.join("/") : ""}/${node.name}`)
                    }
                    anchor.append(node.name)
                    listItem.append(anchor)
                    return listItem
                })

                if (slug.length) {
                    const target = slug.slice(0, -1)

                    const listItem = document.createElement("li")
                    const anchor = document.createElement("a")
                    anchor.href = encodeURI(`${endpoint}/#${target.length ? "/" + target.join("/") : ""}`)
                    anchor.append("..")
                    listItem.append(anchor)
                    listItems.unshift(listItem)
                }

                mediaList.replaceChildren(...listItems)
            }

            loginForm.style.display = "none"
            mediaList.style.display = "block"
        } else {
            console.log(response.status, response.statusText, await response.text())
            localStorage.removeItem("session")

            render().then()
        }
    } else {
        const listener = async (event) => {
            event.preventDefault()

            const data = new FormData(event.currentTarget, event.submitter)

            event.currentTarget.reset()

            const response = await fetchAPI("session", {
                method: "POST",
                body: JSON.stringify(Object.fromEntries(data)),
                headers: {"content-type": "application/json"},
            }, false)

            if (response.ok) {
                const data = await response.json()
                localStorage.setItem("session", data.token)
            } else {
                console.log(response.status, response.statusText, await response.text())
                localStorage.removeItem("session")
            }

            loginForm.removeEventListener("submit", listener)

            render().then()
        }

        loginForm.addEventListener("submit", listener)

        loginForm.style.display = "block"
        mediaList.style.display = "none"
    }
}

render().then()
