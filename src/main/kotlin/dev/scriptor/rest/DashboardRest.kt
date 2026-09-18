package dev.scriptor.rest

import dev.scriptor.MultipartParser
import dev.scriptor.context.AuthContext
import dev.scriptor.context.SessionContext
import dev.scriptor.model.CookieHeader
import dev.scriptor.model.movie.ImageData
import dev.scriptor.model.movie.Movie
import dev.scriptor.model.movie.MovieTable
import dev.scriptor.model.show.*
import dev.scriptor.server.*
import dev.scriptor.server.jvm.annotation.*
import dev.scriptor.server.result.Result
import dev.scriptor.server.result.StreamResult
import dev.scriptor.server.result.UnitResult
import dev.scriptor.ui.Bundle
import dev.scriptor.ui.css.builder.*
import dev.scriptor.ui.dom.Attribute
import dev.scriptor.ui.dom.Comment
import dev.scriptor.ui.dom.Node
import dev.scriptor.ui.dom.Text
import dev.scriptor.ui.html.HtmlElement
import dev.scriptor.ui.html.builder.HtmlButtonElementType
import dev.scriptor.ui.html.builder.HtmlInputElementType
import dev.scriptor.ui.js.JsExpression
import dev.scriptor.ui.js.JsObject
import dev.scriptor.ui.js.JsString
import dev.scriptor.ui.js.builder.JsBuilder
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.logging.Logger
import kotlin.uuid.Uuid

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

    fun <T> CssBuilder<T>.globalStyle() {
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
    }

    fun <T> CssBuilder<T>.mediaListStyle() {
        define(".list") {
            display = CssDisplay.GRID
            this["gap"] = "var(--space-m)"

            this["padding"] = "0"
            this["margin"] = "0"

            this["list-style"] = "none"

            define("&[data-mode='grid'],&[data-mode='grid-poster']") {
                this["grid-template-columns"] = "repeat(auto-fill, minmax(300px, 1fr))"
            }

            define("&[data-mode='list']") {
                this["grid-template-columns"] = "1fr"
            }
        }
    }

    fun <T> CssBuilder<T>.mediaListItemStyle() {
        define(".item") {
            this["position"] = "relative"

            display = CssDisplay.FLEX
            flexWrap = CssFlexWrap.NOWRAP

            this["background-color"] = "var(--color-panel)"

            this["border-radius"] = "var(--border-radius)"
            this["overflow"] = "hidden"

            define(".thumbnail") {
                display = CssDisplay.BLOCK
                this["position"] = "relative"

                this["background-color"] = "#111"

                this["object-fit"] = "cover"
            }

            define(".title") {
                this["margin"] = "var(--space-m)"

                this["overflow"] = "hidden"

                this["text-overflow"] = "ellipsis"

                this["display"] = "-webkit-box"
                this["-webkit-box-orient"] = "vertical"
                this["-webkit-line-clamp"] = "1"

                this["line-clamp"] = "1"

                this["font-size"] = "var(--font-size-small)"

                this["text-decoration"] = "none"
                this["color"] = "var(--color-foreground)"
                this["background-color"] = "transparent"

                define("&::after") {
                    this["content"] = "''"
                    this["position"] = "absolute"
                    this["inset"] = "0"
                }
            }

            define("&[data-mode='grid']") {
                flexDirection = CssFlexDirection.COLUMN

                define(".thumbnail") {
                    this["max-width"] = "100%"

                    this["width"] = "100%"
                    this["height"] = "auto"

                    this["aspect-ratio"] = "5 / 3"
                }

                define(".title") {
                    this["text-align"] = "center"
                }
            }

            define("&[data-mode='grid-poster']") {
                flexDirection = CssFlexDirection.COLUMN

                define(".thumbnail") {
                    this["max-width"] = "100%"

                    this["width"] = "100%"
                    this["height"] = "auto"

                    this["aspect-ratio"] = "3 / 4"
                }

                define(".title") {
                    this["text-align"] = "center"
                }
            }

            define("&[data-mode='list']") {
                flexDirection = CssFlexDirection.ROW

                this["align-items"] = "center"

                define(".thumbnail") {
                    this["width"] = "30vw"
                    this["height"] = "100%"

                    this["aspect-ratio"] = "2 / 1"
                }
            }

            define("&:has(.title:is(:hover,:focus-visible))") {
                this["background-color"] = "var(--color-panel-active)"
                this["box-shadow"] = "5px 5px 10px #111"

                define(".title") {
                    this["text-decoration"] = "underline"
                }
            }
        }
    }

    fun <T> JsBuilder<T>.emitShareUrl(title: JsExpression, url: JsExpression) {
        emitIf(
            window.navigator.share,
            thenBlock = {
                window.navigator.share(
                    JsObject(
                        "title" to title,
                        "url" to url,
                    ),
                ).emit()
            },
            elseBlock = {
                emitIf(
                    window.navigator.clipboard,
                    thenBlock = {
                        window.navigator.clipboard.writeText(url).emit()
                    },
                    elseBlock = {
                        window.open(url).emit()
                    },
                )
            },
        )
    }

    data class MediaListItem(
        val href: String,
        val title: String,
        val thumbnail: ((className: String?, sizes: String?) -> Node) = { className, _ ->
            HtmlElement(
                false,
                "div",
                listOf(
                    Attribute("class", className),
                ),
                listOf()
            )
        },
    )

    enum class MediaListMode(val value: String) {
        LIST("list"),
        GRID("grid"),
        GRID_POSTER("grid-poster"),
    }

    fun mediaListItemComponent(item: MediaListItem, mode: MediaListMode): Node {
        val children = mutableListOf<Node>()

        children += item.thumbnail(
            "thumbnail",
            when (mode) {
                MediaListMode.LIST -> "30vw"
                else -> "(max-width: 600px) 50vw, 300px"
            },
        )

        children += HtmlElement(
            false,
            "a",
            listOf(
                Attribute("class", "title"),
                Attribute("href", item.href),
            ),
            listOf(
                Text(item.title),
            ),
        )

        return HtmlElement(
            false,
            "li",
            listOf(
                Attribute("class", "item"),
                Attribute("data-mode", mode.value),
            ),
            children,
        )
    }

    fun mediaListComponent(
        items: List<MediaListItem>,
        end: MediaListItem? = null,
        mode: MediaListMode = MediaListMode.GRID,
    ): Node {
        val children = mutableListOf<Node>()
        for (item in items) {
            children += mediaListItemComponent(item, mode)
        }
        if (end != null) {
            children += mediaListItemComponent(end, mode)
        }

        return HtmlElement(
            false,
            "ul",
            listOf(
                Attribute("class", "list"),
                Attribute("data-mode", mode.value),
            ),
            children,
        )
    }

    fun imageComponent(
        className: String? = null,
        sizes: String? = null,
        src: List<ImageData>,
        noFallback: Boolean = false,
    ): Node {
        val attributes = mutableListOf<Attribute>()

        if (className != null) {
            attributes += Attribute("class", className)
        }

        if (sizes != null) {
            attributes += Attribute("sizes", sizes)
        }

        if (src.isEmpty()) {
            if (noFallback) {
                return Comment("empty image src set")
            }

            return HtmlElement(
                false,
                "div",
                attributes,
                listOf(),
            )
        }

        val srcset = src.joinToString(", ") {
            if (it.width >= 0)
                "${it.url} ${it.width}w"
            else
                "${it.url} 1920w"
        }

        attributes += Attribute("srcset", srcset)

        return HtmlElement(
            true,
            "img",
            attributes,
            listOf(),
        )
    }

    @Get("/", "text/html")
    context(
        _: Logger,
        database: Database,
        auth: AuthContext,
    )
    fun getDashboardPage(@Header cookie: CookieHeader = CookieHeader()): String {
        val token = cookie["token"]

        val session = auth.auth(token)
            ?: throw FoundSignal(ParameterList("location" to "/login"))

        val user = session.user

        val movies = transaction(database) {
            Movie
                .all()
                .orderBy(MovieTable.title to SortOrder.ASC)
                .limit(4)
                .toList()
        }

        val shows = transaction(database) {
            Show
                .all()
                .orderBy(ShowTable.title to SortOrder.ASC)
                .limit(4)
                .toList()
        }

        return Bundle().html {
            head {
                meta(charset = "utf-8")
                title("Dashboard")
            }
            body {
                main {
                    h1 { +"Dashboard" }
                    h2 { +"Welcome back${if (user != null) ", ${user.name}" else ""}" }

                    section({ htmlClass = "section" }) {
                        h3 { +"Movies" }

                        +mediaListComponent(
                            items = movies.map {
                                MediaListItem(
                                    href = "/movie/${it.id}",
                                    title = it.title,
                                    thumbnail = { className, sizes ->
                                        imageComponent(
                                            className = className,
                                            sizes = sizes,
                                            src = it.poster,
                                        )
                                    },
                                )
                            },
                            end = MediaListItem(
                                href = "/movie",
                                title = "All movies",
                            ),
                            mode = MediaListMode.GRID,
                        )
                    }

                    section({ htmlClass = "section" }) {
                        h3 { +"Shows" }

                        +mediaListComponent(
                            items = shows.map {
                                MediaListItem(
                                    href = "/show/${it.id}",
                                    title = it.title,
                                    thumbnail = { className, sizes ->
                                        imageComponent(
                                            className = className,
                                            sizes = sizes,
                                            src = it.poster,
                                        )
                                    },
                                )
                            },
                            end = MediaListItem(
                                href = "/show",
                                title = "All shows",
                            ),
                            mode = MediaListMode.GRID,
                        )
                    }
                }

                style {
                    globalStyle()
                    mediaListStyle()
                    mediaListItemStyle()

                    define(".section") {
                        this["margin-bottom"] = "var(--space-xl)"
                    }
                }
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
                    globalStyle()

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

    @Get("/movie", "text/html")
    context(
        _: Logger,
        database: Database,
        auth: AuthContext,
    )
    fun getMovieListPage(@Header cookie: CookieHeader = CookieHeader()): String {
        val token = cookie["token"]

        val session = auth.auth(token)
            ?: throw FoundSignal(ParameterList("location" to "/login"))

        val movies = transaction(database) {
            Movie
                .all()
                .orderBy(MovieTable.title to SortOrder.ASC)
                .toList()
        }

        return Bundle().html {
            head {
                meta(charset = "utf-8")
                title("Movies")
            }

            body {
                main {
                    h1 { +"Movies" }

                    +mediaListComponent(
                        items = movies.map {
                            MediaListItem(
                                href = "/movie/${it.id}",
                                title = it.title,
                                thumbnail = { className, sizes ->
                                    imageComponent(
                                        className = className,
                                        sizes = sizes,
                                        src = it.poster,
                                    )
                                },
                            )
                        },
                        mode = MediaListMode.GRID_POSTER,
                    )
                }

                style {
                    globalStyle()
                    mediaListStyle()
                    mediaListItemStyle()
                }
            }
        }.toXmlString()
    }

    @Get("/movie/[id]", "text/html")
    context(
        _: Logger,
        database: Database,
        auth: AuthContext,
    )
    fun getMovieDetailPage(
        @PathParameter id: Uuid,
        @Header cookie: CookieHeader = CookieHeader(),
    ): String {
        val token = cookie["token"]

        val session = auth.auth(token)
            ?: throw FoundSignal(ParameterList("location" to "/login"))

        val movie = transaction(database) { Movie.findById(id) }
            ?: throw NotFoundSignal()

        return Bundle().html {
            head {
                meta(charset = "utf-8")
                title("${movie.title} | Movies")
            }

            body {
                div({ htmlClass = "banner" }) {
                    +imageComponent(
                        className = "backdrop",
                        sizes = "100vw",
                        src = movie.backdrop,
                    )

                    +imageComponent(
                        className = "poster",
                        sizes = "200px",
                        src = movie.poster,
                        noFallback = true,
                    )
                }

                main({ htmlClass = "content" }) {
                    h1 { +movie.title }

                    when (val description = movie.description) {
                        null -> {}
                        else -> {
                            p { +description }
                        }
                    }

                    p {
                        button({ type = HtmlButtonElementType.BUTTON }) {
                            +"Play"

                            on("click") {
                                val response = window.fetch(
                                    "/resource/movie/${movie.id}/playback",
                                    JsObject(
                                        "method" to JsString("post"),
                                    ),
                                )

                                val responseToText = function("response") { (response) ->
                                    emitReturn(response["text"]())
                                }

                                val text = response["then"](responseToText)

                                val textToUrl = function("text") { (text) ->
                                    val origin = window.location.origin
                                    val url = origin +
                                            JsString("/resource/playback/") +
                                            text +
                                            JsString("/playlist.m3u8")

                                    emitReturn(url)
                                }

                                val url = text["then"](textToUrl)

                                val callback = function("url") { (url) ->
                                    emitShareUrl(JsString(movie.title), url)
                                }

                                url["then"](callback).emit()
                            }
                        }
                    }
                }

                style {
                    globalStyle()

                    define(".banner") {
                        display = CssDisplay.BLOCK
                        this["position"] = "relative"
                        this["width"] = "100%"

                        define(".backdrop") {
                            display = CssDisplay.BLOCK

                            this["width"] = "100%"
                            this["height"] = "400px"

                            this["object-fit"] = "cover"
                        }

                        define(".poster") {
                            display = CssDisplay.BLOCK

                            this["position"] = "absolute"
                            this["bottom"] = "0"
                            this["left"] = "100px"

                            this["transform"] = "translateY(50%)"

                            this["width"] = "200px"
                            this["height"] = "300px"

                            this["object-fit"] = "cover"
                        }
                    }

                    define(".content") {
                        this["margin-left"] = "300px"
                    }

                    define("@media(max-width:768px)") {
                        define(".banner") {
                            define(".poster") {
                                display = CssDisplay.NONE
                            }
                        }

                        define(".content") {
                            this["margin-left"] = "0"
                        }
                    }
                }
            }
        }.toXmlString()
    }

    @Get("/show", "text/html")
    context(
        _: Logger,
        database: Database,
        auth: AuthContext,
    )
    fun getShowListPage(@Header cookie: CookieHeader = CookieHeader()): String {
        val token = cookie["token"]

        val session = auth.auth(token)
            ?: throw FoundSignal(ParameterList("location" to "/login"))

        val shows = transaction(database) {
            Show
                .all()
                .orderBy(ShowTable.title to SortOrder.ASC)
                .toList()
        }

        return Bundle().html {
            head {
                meta(charset = "utf-8")
                title("Shows")
            }

            body {
                main {
                    h1 { +"Shows" }

                    +mediaListComponent(
                        items = shows.map {
                            MediaListItem(
                                href = "/show/${it.id}",
                                title = it.title,
                                thumbnail = { className, sizes ->
                                    imageComponent(
                                        className = className,
                                        sizes = sizes,
                                        src = it.poster,
                                    )
                                },
                            )
                        },
                        mode = MediaListMode.GRID_POSTER,
                    )
                }

                style {
                    globalStyle()
                    mediaListStyle()
                    mediaListItemStyle()
                }
            }
        }.toXmlString()
    }

    @Get("/show/[id]", "text/html")
    context(
        _: Logger,
        database: Database,
        auth: AuthContext,
    )
    fun getShowDetailPage(
        @PathParameter id: Uuid,
        @Header cookie: CookieHeader = CookieHeader(),
    ): String {
        val token = cookie["token"]

        val session = auth.auth(token)
            ?: throw FoundSignal(ParameterList("location" to "/login"))

        val show = transaction(database) { Show.findById(id) }
            ?: throw NotFoundSignal()

        val seasons = transaction(database) {
            show.seasons
                .orderBy(SeasonTable.index to SortOrder.ASC)
                .toList()
        }

        return Bundle().html {
            head {
                meta(charset = "utf-8")
                title("${show.title} | Shows")
            }

            body {
                div({ htmlClass = "banner" }) {
                    +imageComponent(
                        className = "backdrop",
                        sizes = "100vw",
                        src = show.backdrop,
                    )

                    +imageComponent(
                        className = "poster",
                        sizes = "200px",
                        src = show.poster,
                        noFallback = true,
                    )
                }

                main({ htmlClass = "content" }) {
                    h1 { +show.title }

                    when (val description = show.description) {
                        null -> {}
                        else -> {
                            p { +description }
                        }
                    }

                    h2 { +"Seasons" }

                    +mediaListComponent(
                        items = seasons.map {
                            MediaListItem(
                                href = "/season/${it.id}",
                                title = it.title,
                                thumbnail = { className, sizes ->
                                    imageComponent(
                                        className = className,
                                        sizes = sizes,
                                        src = it.poster,
                                    )
                                },
                            )
                        },
                        mode = MediaListMode.GRID_POSTER,
                    )
                }

                style {
                    globalStyle()
                    mediaListStyle()
                    mediaListItemStyle()

                    define(".banner") {
                        display = CssDisplay.BLOCK
                        this["position"] = "relative"
                        this["width"] = "100%"

                        define(".backdrop") {
                            display = CssDisplay.BLOCK

                            this["width"] = "100%"
                            this["height"] = "400px"

                            this["object-fit"] = "cover"
                        }

                        define(".poster") {
                            display = CssDisplay.BLOCK

                            this["position"] = "absolute"
                            this["bottom"] = "0"
                            this["left"] = "100px"

                            this["transform"] = "translateY(50%)"

                            this["width"] = "200px"
                            this["height"] = "300px"

                            this["object-fit"] = "cover"
                        }
                    }

                    define(".content") {
                        this["margin-left"] = "300px"
                    }

                    define("@media(max-width:768px)") {
                        define(".banner") {
                            define(".poster") {
                                display = CssDisplay.NONE
                            }
                        }

                        define(".content") {
                            this["margin-left"] = "0"
                        }
                    }
                }
            }
        }.toXmlString()
    }

    @Get("/season/[id]", "text/html")
    context(
        _: Logger,
        database: Database,
        auth: AuthContext,
    )
    fun getSeasonDetailPage(
        @PathParameter id: Uuid,
        @Header cookie: CookieHeader = CookieHeader(),
    ): String {
        val token = cookie["token"]

        val session = auth.auth(token)
            ?: throw FoundSignal(ParameterList("location" to "/login"))

        val season = transaction(database) { Season.findById(id) }
            ?: throw NotFoundSignal()

        val show = transaction(database) { season.show }
        val episodes = transaction(database) { season.episodes.toList() }

        return Bundle().html {
            head {
                meta(charset = "utf-8")
                title("${show.title} - ${season.title} | Shows")
            }

            body {
                main {
                    section({ htmlClass = "header" }) {
                        +imageComponent(
                            className = "poster",
                            sizes = "(max-width: 768px) 100vw, 30vw",
                            src = season.poster,
                            noFallback = true,
                        )

                        div {
                            h1 { +season.title }

                            when (val description = season.description) {
                                null -> {}
                                else -> {
                                    p { +description }
                                }
                            }

                            p {
                                button({ type = HtmlButtonElementType.BUTTON }) {
                                    +"Play all"

                                    on("click") {
                                        val response = window.fetch(
                                            "/resource/season/${season.id}/playback",
                                            JsObject(
                                                "method" to JsString("post"),
                                            ),
                                        )

                                        val responseToText = function("response") { (response) ->
                                            emitReturn(response["text"]())
                                        }

                                        val text = response["then"](responseToText)

                                        val textToUrl = function("text") { (text) ->
                                            val origin = window.location.origin
                                            val url = origin +
                                                    JsString("/resource/playback/") +
                                                    text +
                                                    JsString("/playlist.m3u8")

                                            emitReturn(url)
                                        }

                                        val url = text["then"](textToUrl)

                                        val callback = function("url") { (url) ->
                                            emitShareUrl(JsString(season.title), url)
                                        }

                                        url["then"](callback).emit()
                                    }
                                }
                            }
                        }
                    }

                    h2 { +"Episodes" }

                    +mediaListComponent(
                        items = episodes.map {
                            MediaListItem(
                                href = "/episode/${it.id}",
                                title = it.title,
                                thumbnail = { className, sizes ->
                                    imageComponent(
                                        className = className,
                                        sizes = sizes,
                                        src = it.still,
                                    )
                                },
                            )
                        },
                        mode = MediaListMode.LIST,
                    )
                }

                style {
                    globalStyle()
                    mediaListStyle()
                    mediaListItemStyle()

                    define(".header") {
                        display = CssDisplay.FLEX
                        flexDirection = CssFlexDirection.ROW
                        flexWrap = CssFlexWrap.NOWRAP
                        this["gap"] = "var(--space-l)"

                        this["margin-bottom"] = "var(--space-l)"

                        define(".poster") {
                            this["width"] = "30vw"
                            this["height"] = "auto"
                            this["max-height"] = "400px"

                            this["object-fit"] = "contain"
                        }
                    }

                    define("@media(max-width:768px)") {
                        define(".header") {
                            flexDirection = CssFlexDirection.COLUMN
                            flexWrap = CssFlexWrap.NOWRAP

                            define(".poster") {
                                this["width"] = "100%"
                            }
                        }
                    }
                }
            }
        }.toXmlString()
    }

    @Get("/episode/[id]", "text/html")
    context(
        _: Logger,
        database: Database,
        auth: AuthContext,
    )
    fun getEpisodeDetailPage(
        @PathParameter id: Uuid,
        @Header cookie: CookieHeader = CookieHeader(),
    ): String {
        val token = cookie["token"]

        val session = auth.auth(token)
            ?: throw FoundSignal(ParameterList("location" to "/login"))

        val episode = transaction(database) { Episode.findById(id) }
            ?: throw NotFoundSignal()

        val season = transaction(database) { episode.season }
        val show = transaction(database) { season.show }

        return Bundle().html {
            head {
                meta(charset = "utf-8")
                title("${show.title} - ${season.title} - ${episode.title} | Shows")
            }

            body {
                main {
                    section({ htmlClass = "header" }) {
                        +imageComponent(
                            className = "still",
                            sizes = "(max-width: 768px) 100vw, 30vw",
                            src = episode.still,
                            noFallback = true,
                        )

                        div {
                            h1 { +episode.title }

                            when (val description = episode.description) {
                                null -> {}
                                else -> {
                                    p { +description }
                                }
                            }

                            p {
                                button({ type = HtmlButtonElementType.BUTTON }) {
                                    +"Play"

                                    on("click") {
                                        val response = window.fetch(
                                            "/resource/episode/${episode.id}/playback",
                                            JsObject(
                                                "method" to JsString("post"),
                                            ),
                                        )

                                        val responseToText = function("response") { (response) ->
                                            emitReturn(response["text"]())
                                        }

                                        val text = response["then"](responseToText)

                                        val textToUrl = function("text") { (text) ->
                                            val origin = window.location.origin
                                            val url = origin +
                                                    JsString("/resource/playback/") +
                                                    text +
                                                    JsString("/0/master.m3u8")

                                            emitReturn(url)
                                        }

                                        val url = text["then"](textToUrl)

                                        val callback = function("url") { (url) ->
                                            emitShareUrl(JsString(episode.title), url)
                                        }

                                        url["then"](callback).emit()
                                    }
                                }
                            }
                        }
                    }
                }

                style {
                    globalStyle()

                    define(".header") {
                        display = CssDisplay.FLEX
                        flexDirection = CssFlexDirection.ROW
                        flexWrap = CssFlexWrap.NOWRAP
                        this["gap"] = "var(--space-l)"

                        this["margin-bottom"] = "var(--space-l)"

                        define(".still") {
                            this["width"] = "30vw"
                            this["height"] = "auto"
                            this["max-height"] = "400px"

                            this["object-fit"] = "contain"
                        }
                    }

                    define("@media(max-width:768px)") {
                        define(".header") {
                            flexDirection = CssFlexDirection.COLUMN
                            flexWrap = CssFlexWrap.NOWRAP

                            define(".still") {
                                this["width"] = "100%"
                            }
                        }
                    }
                }
            }
        }.toXmlString()
    }
}
