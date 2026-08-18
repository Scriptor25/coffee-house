package dev.scriptor.rest

import dev.scriptor.server.NoContentSignal
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.ParameterList
import dev.scriptor.server.annotation.Controller
import dev.scriptor.server.annotation.Get
import dev.scriptor.server.annotation.PathParameter
import dev.scriptor.server.result.StreamResult

@Controller("/")
class DashboardRest {

    @Get("/favicon.[]", result = "image/svg+xml")
    fun getFavicon(): StreamResult {
        val stream = ClassLoader.getSystemResourceAsStream("favicon.svg")
            ?: throw NotFoundSignal()

        val headers = ParameterList(
            "cache-control" to "public, max-age=604800, immutable",
        )

        return StreamResult(
            headers = headers,
            value = stream,
        )
    }

    @Get("/", result = "text/html")
    fun getDashboard(): StreamResult {
        val stream = ClassLoader.getSystemResourceAsStream("dashboard.html")
            ?: throw NotFoundSignal()

        val headers = ParameterList(
            "cache-control" to "public, max-age=604800, immutable",
        )

        return StreamResult(
            headers = headers,
            value = stream,
        )
    }

    @Get("/script/[slug+].js", result = "text/javascript")
    fun getScript(@PathParameter slug: Array<String>): StreamResult {
        val stream = ClassLoader.getSystemResourceAsStream("script/${slug.joinToString("/")}.js")
            ?: throw NotFoundSignal()

        val headers = ParameterList(
            "cache-control" to "public, max-age=604800, immutable",
        )

        return StreamResult(
            headers = headers,
            value = stream,
        )
    }

    @Get("/health")
    fun getHealth(): Unit = throw NoContentSignal()
}
