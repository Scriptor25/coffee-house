package dev.scriptor.rest

import dev.scriptor.JsonNode
import dev.scriptor.component.ImageComponent
import dev.scriptor.component.MediaListComponent
import dev.scriptor.component.MediaListItem
import dev.scriptor.component.MediaListMode
import dev.scriptor.context.SessionContext
import dev.scriptor.jsonArray
import dev.scriptor.jsonObject
import dev.scriptor.jsonOf
import dev.scriptor.model.movie.Movie
import dev.scriptor.model.movie.MovieTable
import dev.scriptor.model.show.*
import dev.scriptor.model.user.User
import dev.scriptor.server.*
import dev.scriptor.server.jvm.annotation.*
import dev.scriptor.server.request.OriginRequestTarget
import dev.scriptor.server.request.Request
import dev.scriptor.server.result.Result
import dev.scriptor.server.result.StreamResult
import dev.scriptor.server.result.StringResult
import dev.scriptor.server.security.Principal
import dev.scriptor.ui.Bundle
import dev.scriptor.ui.bundle
import dev.scriptor.ui.component
import dev.scriptor.ui.css.builder.*
import dev.scriptor.ui.html.builder.HtmlButtonElementType
import dev.scriptor.ui.html.builder.HtmlInputElementType
import dev.scriptor.ui.js.JsExpression
import dev.scriptor.ui.js.JsString
import dev.scriptor.ui.js.builder.JsNodeBuilder
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.net.URLDecoder
import kotlin.time.Clock
import kotlin.uuid.Uuid

@RequireAuth
@Controller("/")
class DashboardRest {

    private fun resource(name: String): Result {
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

    private fun Bundle.cache(): Result {
        val headers = ParameterList(
            "cache-control" to "public, max-age=120, immutable",
        )

        return StringResult(
            headers = headers,
            value = toString(),
        )
    }

    @Public
    @Get("/favicon.[]", "image/svg+xml")
    fun getFavicon(): Result {
        return resource("favicon.svg")
    }

    @Public
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

    fun <T> JsNodeBuilder<T>.emitShareUrl(title: JsExpression, url: JsExpression) {
        emit(
            jsIfElse(
                window.navigator.share,
                thenBlock = {
                    emit(
                        window.navigator.share(
                            jsObject {
                                this["title"] = title
                                this["url"] = url
                            },
                        ),
                    )
                },
                elseBlock = {
                    emit(
                        jsIfElse(
                            window.navigator.clipboard,
                            thenBlock = {
                                emit(window.navigator.clipboard.writeText(url))
                            },
                            elseBlock = {
                                emit(window.open(url))
                            },
                        ),
                    )
                },
            )
        )
    }

    @Public
    @Get("/manifest.json", "application/json")
    fun getManifest(): JsonNode {
        return jsonObject {
            this["name"] = jsonOf("Coffee House")
            this["icons"] = jsonArray {
                this.add(jsonObject {
                    this["src"] = jsonOf("/favicon.svg")
                    this["type"] = jsonOf("image/svg+xml")
                    this["sizes"] = jsonOf("any")
                })
            }
            this["start_url"] = jsonOf("/")
            this["display"] = jsonOf("standalone")
            this["display_override"] = jsonArray {
                add(jsonOf("fullscreen"))
                add(jsonOf("minimal-ui"))
            }
        }
    }

    @Get("/", "text/html")
    context(principal: Principal, database: Database)
    fun getDashboardPage(): Result {
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

        val user = transaction(database) { User.findById(principal.id) }

        return bundle {
            html {
                head {
                    meta(charset = "utf-8")
                    meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
                    link(rel = "manifest", href = "/manifest.json")
                    title("Dashboard")
                }
                body {
                    main {
                        h1 { +"Dashboard" }
                        h2 { +"Welcome back${if (user != null) ", ${user.name}" else ""}" }

                        section({ htmlClass = "section" }) {
                            h3 { +"Movies" }

                            +component(::MediaListComponent) {
                                items = movies.map {
                                    MediaListItem(
                                        href = "/movie/${it.id}",
                                        title = it.title,
                                        thumbnail = { className, sizes ->
                                            component(::ImageComponent) {
                                                this.className = className
                                                this.sizes = sizes
                                                this.src = it.poster
                                            }
                                        },
                                    )
                                }
                                end = MediaListItem(
                                    href = "/movie",
                                    title = "All movies",
                                )
                                mode = MediaListMode.GRID
                            }
                        }

                        section({ htmlClass = "section" }) {
                            h3 { +"Shows" }

                            +component(::MediaListComponent) {
                                items = shows.map {
                                    MediaListItem(
                                        href = "/show/${it.id}",
                                        title = it.title,
                                        thumbnail = { className, sizes ->
                                            component(::ImageComponent) {
                                                this.className = className
                                                this.sizes = sizes
                                                this.src = it.poster
                                            }
                                        },
                                    )
                                }
                                end = MediaListItem(
                                    href = "/show",
                                    title = "All shows",
                                )
                                mode = MediaListMode.GRID
                            }
                        }
                    }
                }
            }

            style {
                globalStyle()

                define(".section") {
                    this["margin-bottom"] = "var(--space-xl)"
                }
            }
        }.cache()
    }

    @Public
    @Post("/login", "application/x-www-form-urlencoded")
    context(
        _: Provider,
        _: Database,
        sessions: SessionContext,
    )
    fun login(@QueryParameter next: String = "/", @Body body: String): Result {
        val parameters = body
            .split("&")
            .map { it.trim().split("=", limit = 2) }
            .associate { URLDecoder.decode(it[0], Charsets.UTF_8) to URLDecoder.decode(it[1], Charsets.UTF_8) }

        val username = parameters["username"]
            ?: throw BadRequestSignal()
        val password = parameters["password"]
            ?: throw BadRequestSignal()

        val instant = Clock.System.now()

        val token = sessions.createSession(username, password, instant)
            ?: throw UnauthorizedSignal()

        val maxAge = when (val exp = token.payload.exp) {
            null -> null
            else -> {
                val delta = exp - instant
                if (delta.isNegative())
                    null
                else
                    delta.inWholeSeconds
            }
        }

        return FoundSignal(
            ParameterList(
                "set-cookie" to "token=$token; Path=/; HttpOnly; ${if (maxAge != null) "Max-Age=$maxAge; " else ""}SameSite=Strict",
                "location" to if (next.startsWith("/login")) "/" else next,
            ),
        ).generate()
    }

    @Public
    @Get("/login", "text/html")
    context(principal: Principal?)
    fun getLoginPage(@QueryParameter next: String = "/"): Result {
        if (principal != null) {
            throw FoundSignal(
                ParameterList(
                    "location" to if (next.startsWith("/login")) "/" else next,
                ),
            )
        }

        return bundle {
            html {
                head {
                    meta(charset = "utf-8")
                    meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
                    link(rel = "manifest", href = "/manifest.json")
                    title("Login")
                }

                body {
                    main {
                        h1 { +"Login" }

                        form({
                            htmlClass = "form"
                            encType = "application/x-www-form-urlencoded"
                            method = "post"
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
        }.cache()
    }

    @Get("/movie", "text/html")
    context(database: Database)
    fun getMovieListPage(): Result {
        val movies = transaction(database) {
            Movie
                .all()
                .orderBy(MovieTable.title to SortOrder.ASC)
                .toList()
        }

        return bundle {
            html {
                head {
                    meta(charset = "utf-8")
                    meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
                    link(rel = "manifest", href = "/manifest.json")
                    title("Movies")
                }

                body {
                    main {
                        h1 { +"Movies" }

                        +component(::MediaListComponent) {
                            items = movies.map {
                                MediaListItem(
                                    href = "/movie/${it.id}",
                                    title = it.title,
                                    thumbnail = { className, sizes ->
                                        component(::ImageComponent) {
                                            this.className = className
                                            this.sizes = sizes
                                            this.src = it.poster
                                        }
                                    },
                                )
                            }
                            mode = MediaListMode.GRID_POSTER
                        }
                    }
                }
            }

            style {
                globalStyle()
            }
        }.cache()
    }

    @Get("/movie/[id]", "text/html")
    context(database: Database)
    fun getMovieDetailPage(@PathParameter id: Uuid): Result {
        val movie = transaction(database) { Movie.findById(id) }
            ?: throw NotFoundSignal()

        return bundle {
            html {
                head {
                    meta(charset = "utf-8")
                    meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
                    link(rel = "manifest", href = "/manifest.json")
                    title("${movie.title} | Movies")
                }

                body {
                    div({ htmlClass = "banner" }) {
                        +component(::ImageComponent) {
                            className = "backdrop"
                            sizes = "100vw"
                            src = movie.backdrop
                        }

                        +component(::ImageComponent) {
                            className = "poster"
                            sizes = "200px"
                            src = movie.poster
                            noFallback = true
                        }
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
                                        jsObject {
                                            this["method"] = jsString("post")
                                        },
                                    )

                                    val responseToText = jsFunction("response") { (response) ->
                                        emit(jsReturn(response["text"]()))
                                    }

                                    val text = response["then"](responseToText)

                                    val textToUrl = jsFunction("text") { (text) ->
                                        val origin = window.location.origin
                                        val url = origin +
                                                JsString("/resource/playback/") +
                                                text +
                                                JsString("/playlist.m3u8")

                                        emit(jsReturn(url))
                                    }

                                    val url = text["then"](textToUrl)

                                    val callback = jsFunction("url") { (url) ->
                                        emitShareUrl(JsString(movie.title), url)
                                    }

                                    emit(url["then"](callback))
                                }
                            }
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
        }.cache()
    }

    @Get("/show", "text/html")
    context(database: Database)
    fun getShowListPage(): Result {
        val shows = transaction(database) {
            Show
                .all()
                .orderBy(ShowTable.title to SortOrder.ASC)
                .toList()
        }

        return bundle {
            html {
                head {
                    meta(charset = "utf-8")
                    meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
                    link(rel = "manifest", href = "/manifest.json")
                    title("Shows")
                }

                body {
                    main {
                        h1 { +"Shows" }

                        +component(::MediaListComponent) {
                            items = shows.map {
                                MediaListItem(
                                    href = "/show/${it.id}",
                                    title = it.title,
                                    thumbnail = { className, sizes ->
                                        component(::ImageComponent) {
                                            this.className = className
                                            this.sizes = sizes
                                            this.src = it.poster
                                        }
                                    },
                                )
                            }
                            mode = MediaListMode.GRID_POSTER
                        }
                    }
                }
            }

            style {
                globalStyle()
            }
        }.cache()
    }

    @Get("/show/[id]", "text/html")
    context(database: Database)
    fun getShowDetailPage(@PathParameter id: Uuid): Result {
        val show = transaction(database) { Show.findById(id) }
            ?: throw NotFoundSignal()

        val seasons = transaction(database) {
            show.seasons
                .orderBy(SeasonTable.index to SortOrder.ASC)
                .toList()
        }

        return bundle {
            html {
                head {
                    meta(charset = "utf-8")
                    meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
                    link(rel = "manifest", href = "/manifest.json")
                    title("${show.title} | Shows")
                }

                body {
                    div({ htmlClass = "banner" }) {
                        +component(::ImageComponent) {
                            className = "backdrop"
                            sizes = "100vw"
                            src = show.backdrop
                        }

                        +component(::ImageComponent) {
                            className = "poster"
                            sizes = "200px"
                            src = show.poster
                            noFallback = true
                        }
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

                        +component(::MediaListComponent) {
                            items = seasons.map {
                                MediaListItem(
                                    href = "/season/${it.id}",
                                    title = it.title,
                                    thumbnail = { className, sizes ->
                                        component(::ImageComponent) {
                                            this.className = className
                                            this.sizes = sizes
                                            this.src = it.poster
                                        }
                                    },
                                )
                            }
                            mode = MediaListMode.GRID_POSTER
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
        }.cache()
    }

    @Get("/season/[id]", "text/html")
    context(database: Database)
    fun getSeasonDetailPage(@PathParameter id: Uuid): Result {
        val season = transaction(database) { Season.findById(id) }
            ?: throw NotFoundSignal()

        val show = transaction(database) { season.show }
        val episodes = transaction(database) { season.episodes.toList() }

        return bundle {
            html {
                head {
                    meta(charset = "utf-8")
                    meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
                    link(rel = "manifest", href = "/manifest.json")
                    title("${show.title} - ${season.title} | Shows")
                }

                body {
                    main {
                        section({ htmlClass = "header" }) {
                            +component(::ImageComponent) {
                                className = "poster"
                                sizes = "(max-width: 768px) 100vw, 30vw"
                                src = season.poster
                                noFallback = true
                            }

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
                                                jsObject {
                                                    this["method"] = jsString("post")
                                                },
                                            )

                                            val responseToText = jsFunction("response") { (response) ->
                                                emit(jsReturn(response["text"]()))
                                            }

                                            val text = response["then"](responseToText)

                                            val textToUrl = jsFunction("text") { (text) ->
                                                val origin = window.location.origin
                                                val url = origin +
                                                        JsString("/resource/playback/") +
                                                        text +
                                                        JsString("/playlist.m3u8")

                                                emit(jsReturn(url))
                                            }

                                            val url = text["then"](textToUrl)

                                            val callback = jsFunction("url") { (url) ->
                                                emitShareUrl(JsString(season.title), url)
                                            }

                                            emit(url["then"](callback))
                                        }
                                    }
                                }
                            }
                        }

                        h2 { +"Episodes" }

                        +component(::MediaListComponent) {
                            items = episodes.map {
                                MediaListItem(
                                    href = "/episode/${it.id}",
                                    title = it.title,
                                    thumbnail = { className, sizes ->
                                        component(::ImageComponent) {
                                            this.className = className
                                            this.sizes = sizes
                                            this.src = it.still
                                        }
                                    },
                                )
                            }
                            mode = MediaListMode.LIST
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
        }.cache()
    }

    @Get("/episode/[id]", "text/html")
    context(database: Database)
    fun getEpisodeDetailPage(@PathParameter id: Uuid): Result {
        val episode = transaction(database) { Episode.findById(id) }
            ?: throw NotFoundSignal()

        val season = transaction(database) { episode.season }
        val show = transaction(database) { season.show }

        return bundle {
            html {
                head {
                    meta(charset = "utf-8")
                    meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
                    link(rel = "manifest", href = "/manifest.json")
                    title("${show.title} - ${season.title} - ${episode.title} | Shows")
                }

                body {
                    main {
                        section({ htmlClass = "header" }) {
                            +component(::ImageComponent) {
                                className = "still"
                                sizes = "(max-width: 768px) 100vw, 30vw"
                                src = episode.still
                                noFallback = true
                            }

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
                                                jsObject {
                                                    this["method"] = JsString("post")
                                                },
                                            )

                                            val responseToText = jsFunction("response") { (response) ->
                                                emit(jsReturn(response["text"]()))
                                            }

                                            val text = response["then"](responseToText)

                                            val textToUrl = jsFunction("text") { (text) ->
                                                val origin = window.location.origin
                                                val url = origin +
                                                        JsString("/resource/playback/") +
                                                        text +
                                                        JsString("/0/master.m3u8")

                                                emit(jsReturn(url))
                                            }

                                            val url = text["then"](textToUrl)

                                            val callback = jsFunction("url") { (url) ->
                                                emitShareUrl(JsString(episode.title), url)
                                            }

                                            emit(url["then"](callback))
                                        }
                                    }
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
        }.cache()
    }

    @Handle(UnauthorizedSignal::class)
    fun handleUnauthorizedSignal(request: Request, signal: UnauthorizedSignal): Result {
        val next = when (val target = request.target) {
            is OriginRequestTarget -> target.path
            else -> null
        }

        val location = if (next == null) "/login" else "/login?next=$next"

        return FoundSignal(ParameterList("location" to location)).generate()
    }
}
