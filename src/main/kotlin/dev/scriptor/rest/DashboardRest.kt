package dev.scriptor.rest

import dev.scriptor.server.NoContentSignal
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.ParameterList
import dev.scriptor.server.jvm.annotation.Controller
import dev.scriptor.server.jvm.annotation.Get
import dev.scriptor.server.result.Result
import dev.scriptor.server.result.StreamResult

@Suppress("unused")
@Controller("/")
class DashboardRest {

    private fun getResource(name: String): Result {
        val stream = ClassLoader.getSystemResourceAsStream(name)
            ?: throw NotFoundSignal()

        val headers = ParameterList(
            "cache-control" to "public, max-age=604800, immutable",
        )

        return StreamResult(
            headers = headers,
            value = stream,
        )
    }

    @Get("/favicon.[]", result = "image/svg+xml")
    fun getFavicon(): Result {
        return getResource("favicon.svg")
    }

    @Get("/", result = "text/html")
    fun getDocument(): Result {
        return getResource("index.html")
    }

    @Get("/index.js", result = "text/javascript")
    fun getScript(): Result {
        return getResource("index.js")
    }

    @Get("/index.js.map", result = "text/javascript")
    fun getScriptMap(): Result {
        return getResource("index.js.map")
    }

    @Get("/index.css", result = "text/css")
    fun getStyle(): Result {
        return getResource("index.css")
    }

    @Get("/index.css.map", result = "text/css")
    fun getStyleMap(): Result {
        return getResource("index.css.map")
    }

    @Get("/health")
    fun getHealth(): Unit = throw NoContentSignal()
}
