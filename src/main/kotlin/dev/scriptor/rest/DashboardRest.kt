package dev.scriptor.rest

import dev.scriptor.server.ParameterList
import dev.scriptor.server.annotation.Controller
import dev.scriptor.server.annotation.Get
import dev.scriptor.server.annotation.PathParameter
import dev.scriptor.server.result.StreamResult
import dev.scriptor.server.result.UnitResult

@Controller("/")
class DashboardRest {

    @Get("/favicon.[]", result = "image/svg+xml")
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

    @Get("/", result = "text/html")
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

    @Get("/script/[slug+].js", result = "text/javascript")
    fun getScript(@PathParameter slug: Array<String>): StreamResult {
        val stream = ClassLoader.getSystemResourceAsStream("script/${slug.joinToString("/")}.js")
            ?: throw NullPointerException()

        val headers = ParameterList()
        headers["cache-control"] = "public, max-age=604800, immutable"

        return StreamResult(
            headers = headers,
            value = stream,
        )
    }

    @Get("/health")
    fun getHealth(): UnitResult = UnitResult(204, "No Content")
}
