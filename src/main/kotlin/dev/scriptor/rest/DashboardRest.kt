package dev.scriptor.rest

import dev.scriptor.server.ParameterList
import dev.scriptor.server.annotation.Endpoint
import dev.scriptor.server.annotation.PathParameter
import dev.scriptor.server.annotation.Resource
import dev.scriptor.server.result.StreamResult

@Endpoint("/")
class DashboardRest {

    @Resource("/favicon.[]", result = "image/svg+xml")
    fun getFavicon(): StreamResult {
        val stream = ClassLoader.getSystemResourceAsStream("favicon.svg")
            ?: throw NullPointerException()

        val headers = ParameterList()
        headers["cache-control"] = "public, max-age=604800, immutable"

        return StreamResult(
            headers = headers,
            value = stream,
        )
    }

    @Resource("/", result = "text/html")
    fun getDashboard(): StreamResult {
        val stream = ClassLoader.getSystemResourceAsStream("dashboard.html")
            ?: throw NullPointerException()

        val headers = ParameterList()
        headers["cache-control"] = "public, max-age=604800, immutable"

        return StreamResult(
            headers = headers,
            value = stream,
        )
    }

    @Resource("/script/[slug+]", result = "text/javascript")
    fun getScript(@PathParameter slug: Array<String>): StreamResult {
        val stream = ClassLoader.getSystemResourceAsStream("script/${slug.joinToString("/")}")
            ?: throw NullPointerException()

        val headers = ParameterList()
        headers["cache-control"] = "public, max-age=604800, immutable"

        return StreamResult(
            headers = headers,
            value = stream,
        )
    }
}
