package dev.scriptor.rest

import dev.scriptor.server.annotation.Endpoint
import dev.scriptor.server.annotation.Resource
import java.io.InputStream

@Endpoint("/")
class DashboardRest {

    @Resource("/favicon.[]")
    fun getFavicon() {
    }

    @Resource("/[slug+]", result = "text/html")
    fun getDashboard(): InputStream =
        ClassLoader.getSystemResourceAsStream("dashboard.html")
            ?: throw NullPointerException()
}
