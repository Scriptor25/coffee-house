package dev.scriptor.rest

import dev.scriptor.server.NoContentSignal
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.ParameterList
import dev.scriptor.server.jvm.annotation.Controller
import dev.scriptor.server.jvm.annotation.Get
import dev.scriptor.server.result.StreamResult

@Suppress("unused")
@Controller("/")
class DashboardRest {

    private fun resource(name: String): StreamResult {
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

    @Get("/favicon.[]", "image/svg+xml")
    fun getFavicon(): StreamResult {
        return resource("favicon.svg")
    }

    @Get("/", "text/html")
    fun getDocument(): StreamResult {
        return resource("index.html")
    }

    @Get("/index.js", "text/javascript")
    fun getScript(): StreamResult {
        return resource("index.js")
    }

    @Get("/index.js.map")
    fun getScriptMap(): StreamResult {
        return resource("index.js.map")
    }

    @Get("/index.css", "text/css")
    fun getStyle(): StreamResult {
        return resource("index.css")
    }

    @Get("/index.css.map")
    fun getStyleMap(): StreamResult {
        return resource("index.css.map")
    }

    @Get("/health")
    fun getHealth(): Unit = throw NoContentSignal()
}
