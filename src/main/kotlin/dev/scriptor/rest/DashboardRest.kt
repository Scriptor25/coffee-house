package dev.scriptor.rest

import dev.scriptor.MultipartParser
import dev.scriptor.context.AuthContext
import dev.scriptor.context.SessionContext
import dev.scriptor.model.CookieHeader
import dev.scriptor.server.*
import dev.scriptor.server.jvm.annotation.*
import dev.scriptor.server.result.Result
import dev.scriptor.server.result.StreamResult
import dev.scriptor.server.result.UnitResult
import dev.scriptor.ui.Bundle
import dev.scriptor.ui.css.builder.*
import dev.scriptor.ui.html.builder.HtmlButtonElementType
import dev.scriptor.ui.html.builder.HtmlInputElementType
import org.jetbrains.exposed.v1.jdbc.Database
import java.util.logging.Logger

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

    @Get("/health")
    fun getHealth(): Unit = throw NoContentSignal()

    @Get("/", "text/html")
    context(
        _: Logger,
        _: Database,
        auth: AuthContext,
    )
    fun getDashboardPage(@Header cookie: CookieHeader = CookieHeader()): String {
        val token = cookie["token"]

        val session = auth.auth(token)
            ?: throw FoundSignal(ParameterList("location" to "/login"))

        val user = session.user

        return Bundle().html {
            head {
                meta(charset = "utf-8")
                title("Dashboard")
            }
            body {
                h1 { +"Dashboard" }
                h2 { +"Welcome back${if (user != null) ", ${user.name}" else ""}" }

                h3 { +"Movies" }

                h3 { +"Shows" }
            }
        }.toXmlString()
    }

    @Post("/login", "multipart/form-data")
    context(
        _: Logger,
        _: Provider,
        _: Database,
        sessions: SessionContext,
    )
    fun login(@Header("content-type") contentType: String, @Body body: ByteArray): Result {
        val message = MultipartParser(body).parse(contentType)

        var username: String? = null
        var password: String? = null

        for (part in message.parts) {
            val contentDisposition = part.headers["content-disposition"]
                ?: continue

            val parameters = contentDisposition
                .split(";")
                .map(String::trim)

            for (parameter in parameters) {
                val segments = parameter.split("=", limit = 2)
                if (segments.size != 2) continue

                val (key, raw) = segments

                if (key == "name") {
                    val value = raw
                        .substringAfter('"')
                        .substringBeforeLast('"')

                    when (value) {
                        "username" -> username = part.body.decodeToString()
                        "password" -> password = part.body.decodeToString()
                        else -> continue
                    }

                    break
                }
            }
        }

        if (username == null || password == null) {
            throw BadRequestSignal()
        }

        val token = sessions.createSession(username, password)

        val maxAge = when (val exp = token.payload.exp) {
            null -> 2592000L
            else -> when (val iat = token.payload.iat) {
                null -> 2592000L
                else -> (exp - iat).inWholeSeconds
            }
        }

        return UnitResult(
            statusCode = 302,
            statusText = "Found",
            headers = ParameterList(
                "set-cookie" to "token=$token; Path=/; HttpOnly; Max-Age=$maxAge; SameSite=Strict",
                "location" to "/",
            ),
        )
    }

    @Get("/login", "text/html")
    context(
        _: Logger,
        _: Database,
        auth: AuthContext,
    )
    fun getLoginPage(@Header cookie: CookieHeader = CookieHeader()): String {
        val token = cookie["token"]

        val session = auth.auth(token)
        if (session != null) {
            throw FoundSignal(ParameterList("location" to "/"))
        }

        return Bundle().html {
            head {
                meta(charset = "utf-8")
                title("Login")
            }

            body {
                main {
                    h1 { +"Login" }

                    form({
                        htmlClass = "form"
                        encType = "multipart/form-data"
                        method = "POST"
                    }) {
                        div({ htmlClass = "set" }) {
                            label {
                                span { +"Username" }
                                input {
                                    type = HtmlInputElementType.TEXT
                                    name = "username"
                                    autoComplete = "username"
                                    required = true
                                }
                            }
                            label {
                                span { +"Password" }
                                input {
                                    type = HtmlInputElementType.PASSWORD
                                    name = "password"
                                    autoComplete = "current-password"
                                    required = true
                                }
                            }
                        }
                        button({ type = HtmlButtonElementType.SUBMIT }) {
                            +"Login"
                        }
                    }
                }

                style {
                    define(":root") {
                        this["--color-foreground"] = "#eee"
                        this["--color-background"] = "#333"
                        this["--color-panel"] = "#444"
                        this["--color-panel-active"] = "#393939"
                        this["--color-accent"] = "#7e1a97"
                        this["--color-accent-faded"] = "#c679da"

                        this["--border-radius"] = "8px"

                        this["--space-xxs"] = "2px"
                        this["--space-xs"] = "4px"
                        this["--space-s"] = "8px"
                        this["--space-m"] = "16px"
                        this["--space-l"] = "32px"
                        this["--space-xl"] = "64px"
                        this["--space-xxl"] = "128px"

                        this["--font-size-h1"] = "48pt"
                        this["--font-size-h2"] = "32pt"
                        this["--font-size-h3"] = "24pt"
                        this["--font-size-h4"] = "20pt"
                        this["--font-size-p"] = "16pt"
                        this["--font-size-small"] = "12pt"
                        this["--font-size-smaller"] = "8pt"
                    }

                    define("*") {
                        this["appearance"] = "none"
                        this["margin"] = "0"
                        this["padding"] = "0"
                        this["font"] = "inherit"
                        this["color"] = "inherit"
                        this["outline"] = "none"
                    }


                    define("body") {
                        this["color"] = "var(--color-foreground)"
                        this["background-color"] = "var(--color-background)"
                        this["font-family"] = "Arial, Helvetica, sans-serif"
                        this["font-size"] = "var(--font-size-p)"
                    }

                    define("main") {
                        this["padding"] = "var(--space-l)"
                    }

                    define("a") {
                        this["color"] = "var(--color-foreground)"
                        this["text-decoration"] = "underline"
                        this["cursor"] = "pointer"

                        define("&:hover,&:focus-visible") {
                            this["color"] = "var(--color-accent)"
                            this["background-color"] = "var(--color-foreground)"
                        }
                    }

                    define("button") {
                        this["color"] = "var(--color-foreground)"
                        this["background-color"] = "var(--color-panel)"
                        this["border"] = "1px solid var(--color-panel)"
                        this["padding"] = "var(--space-s) var(--space-m)"
                        this["cursor"] = "pointer"

                        define("&:hover,&:focus-visible") {
                            this["color"] = "var(--color-panel)"
                            this["background-color"] = "var(--color-foreground)"
                        }
                    }

                    define("ol,ul") {
                        this["padding-left"] = "var(--space-l)"
                        this["margin"] = "0 0 var(--space-xs) 0"
                    }

                    define("ol") {
                        this["list-style-type"] = "decimal"
                    }

                    define("ul") {
                        this["list-style-type"] = "disc"
                    }

                    define("h1") {
                        this["font-size"] = "var(--font-size-h1)"
                        this["margin"] = "0 0 var(--space-l) 0"
                    }

                    define("h2") {
                        this["font-size"] = "var(--font-size-h2)"
                        this["margin"] = "0 0 var(--space-l) 0"
                    }

                    define("h3") {
                        this["font-size"] = "var(--font-size-h3)"
                        this["margin"] = "0 0 var(--space-m) 0"
                    }

                    define("h4") {
                        this["font-size"] = "var(--font-size-h4)"
                        this["margin"] = "0 0 var(--space-m) 0"
                    }

                    define("p") {
                        this["font-size"] = "var(--font-size-p)"
                        this["margin"] = "0 0 var(--space-m) 0"
                    }

                    define(".container") {
                        this["width"] = "100%"
                        this["margin-left"] = "auto"
                        this["margin-right"] = "auto"

                        define("@media(max-width:719px)") {
                            this["padding-left"] = "var(--space-s)"
                            this["padding-right"] = "var(--space-s)"
                        }

                        define("@media(min-width:720px)") {
                            this["width"] = "700px"
                        }

                        define("@media(min-width:960px)") {
                            this["width"] = "900px"
                        }

                        define("@media(min-width:1200px)") {
                            this["width"] = "1000px"
                        }
                    }

                    define("form") {
                        define("input") {
                            this["border"] = "none"
                            this["padding"] = "var(--space-xs)"
                            this["background-color"] = "var(--color-panel)"
                        }
                    }

                    define(".form") {
                        display = CssDisplay.FLEX
                        flexDirection = CssFlexDirection.COLUMN
                        flexWrap = CssFlexWrap.NOWRAP
                        alignItems = CssAlignItems.FLEX_START
                        justifyContent = CssJustifyContent.FLEX_START
                    }

                    define(".set") {
                        display = CssDisplay.GRID
                        this["grid-template"] = """
                            "a b"
                            "a b"
                            "c c"
                        """.trimIndent()
                        this["gap"] = "10px"
                        alignItems = CssAlignItems.CENTER

                        define("label") {
                            display = CssDisplay.CONTENTS
                        }
                    }
                }
            }
        }.toXmlString()
    }
}
