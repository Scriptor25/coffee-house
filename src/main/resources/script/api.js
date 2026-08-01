/**
 * @param {string} resource
 * @param {RequestInit} init
 * @param {boolean} session
 * @returns {Promise<Response>}
 */
export async function fetchAPI(resource, init, session = true) {
    const endpoint = window.location.origin

    if (session) {
        const token = window.localStorage.getItem("session")

        return fetch(`${endpoint}/${resource}`, {
            ...init,
            headers: {
                ...init.headers,
                "authorization": `Bearer ${token}`,
            },
        })
    }

    return fetch(`${endpoint}/${resource}`, init)
}
