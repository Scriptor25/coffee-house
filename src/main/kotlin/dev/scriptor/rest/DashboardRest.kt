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
import dev.scriptor.model.other.Other
import dev.scriptor.model.other.OtherTable
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
            appearance = "none"
            margin = "0"
            padding = "0"
            font = "inherit"
            color = "inherit"
            outline = "none"
        }


        define("body") {
            color = "var(--color-foreground)"
            backgroundColor = "var(--color-background)"
            fontFamily = "Arial, Helvetica, sans-serif"
            fontSize = "var(--font-size-p)"
        }

        define("main") {
            padding = "var(--space-l)"
        }

        define("a") {
            color = "var(--color-foreground)"
            textDecoration = "underline"
            cursor = "pointer"

            define("&:hover, &:focus-visible") {
                color = "var(--color-accent)"
                backgroundColor = "var(--color-foreground)"
            }
        }

        define("button") {
            color = "var(--color-foreground)"
            backgroundColor = "var(--color-panel)"
            border = "1px solid var(--color-panel)"
            padding = "var(--space-s) var(--space-m)"
            cursor = "pointer"

            define("&:hover, &:focus-visible") {
                color = "var(--color-panel)"
                backgroundColor = "var(--color-foreground)"
            }
        }

        define("ol,ul") {
            paddingLeft = "var(--space-l)"
            margin = "0 0 var(--space-xs) 0"
        }

        define("ol") {
            listStyleType = "decimal"
        }

        define("ul") {
            listStyleType = "disc"
        }

        define("h1") {
            fontSize = "var(--font-size-h1)"
            margin = "0 0 var(--space-l) 0"
        }

        define("h2") {
            fontSize = "var(--font-size-h2)"
            margin = "0 0 var(--space-l) 0"
        }

        define("h3") {
            fontSize = "var(--font-size-h3)"
            margin = "0 0 var(--space-m) 0"
        }

        define("h4") {
            fontSize = "var(--font-size-h4)"
            margin = "0 0 var(--space-m) 0"
        }

        define("p") {
            fontSize = "var(--font-size-p)"
            margin = "0 0 var(--space-m) 0"
        }

        define(".container") {
            width = "100%"
            marginLeft = "auto"
            marginRight = "auto"

            define("@media(max-width:719px)") {
                paddingLeft = "var(--space-s)"
                paddingRight = "var(--space-s)"
            }

            define("@media(min-width:720px)") {
                width = "700px"
            }

            define("@media(min-width:960px)") {
                width = "900px"
            }

            define("@media(min-width:1200px)") {
                width = "1000px"
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

        val others = transaction(database) {
            Other
                .all()
                .orderBy(OtherTable.title to SortOrder.ASC)
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

                        if (movies.isNotEmpty()) {
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
                        }

                        if (shows.isNotEmpty()) {
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

                        if (others.isNotEmpty()) {
                            section({ htmlClass = "section" }) {
                                h3 { +"Others" }

                                +component(::MediaListComponent) {
                                    items = others.map {
                                        MediaListItem(
                                            href = "/other/${it.id}",
                                            title = it.title,
                                        )
                                    }
                                    end = MediaListItem(
                                        href = "/other",
                                        title = "All others",
                                    )
                                    mode = MediaListMode.GRID
                                }
                            }
                        }
                    }
                }
            }

            style {
                globalStyle()

                define(".section") {
                    marginBottom = "var(--space-xl)"
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
                        border = "none"
                        padding = "var(--space-xs)"
                        backgroundColor = "var(--color-panel)"
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
                    gridTemplate = """
                            "a b"
                            "a b"
                            "c c"
                        """.trimIndent()
                    gap = "10px"
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
                    position = "relative"
                    width = "100%"

                    define(".backdrop") {
                        display = CssDisplay.BLOCK

                        width = "100%"
                        height = "400px"

                        objectFit = "cover"
                    }

                    define(".poster") {
                        display = CssDisplay.BLOCK

                        position = "absolute"
                        bottom = "0"
                        left = "100px"

                        transform = "translateY(50%)"

                        width = "200px"
                        height = "300px"

                        objectFit = "cover"
                    }
                }

                define(".content") {
                    marginLeft = "300px"
                }

                define("@media(max-width:768px)") {
                    define(".banner") {
                        define(".poster") {
                            display = CssDisplay.NONE
                        }
                    }

                    define(".content") {
                        marginLeft = "0"
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
                    position = "relative"
                    width = "100%"

                    define(".backdrop") {
                        display = CssDisplay.BLOCK

                        width = "100%"
                        height = "400px"

                        objectFit = "cover"
                    }

                    define(".poster") {
                        display = CssDisplay.BLOCK

                        position = "absolute"
                        bottom = "0"
                        left = "100px"

                        transform = "translateY(50%)"

                        width = "200px"
                        height = "300px"

                        objectFit = "cover"
                    }
                }

                define(".content") {
                    marginLeft = "300px"
                }

                define("@media(max-width:768px)") {
                    define(".banner") {
                        define(".poster") {
                            display = CssDisplay.NONE
                        }
                    }

                    define(".content") {
                        marginLeft = "0"
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
                    gap = "var(--space-l)"

                    marginBottom = "var(--space-l)"

                    define(".poster") {
                        width = "30vw"
                        height = "auto"
                        maxHeight = "400px"

                        objectFit = "contain"
                    }
                }

                define("@media(max-width:768px)") {
                    define(".header") {
                        flexDirection = CssFlexDirection.COLUMN
                        flexWrap = CssFlexWrap.NOWRAP

                        define(".poster") {
                            width = "100%"
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
                    gap = "var(--space-l)"

                    marginBottom = "var(--space-l)"

                    define(".still") {
                        width = "30vw"
                        height = "auto"
                        maxHeight = "400px"

                        objectFit = "contain"
                    }
                }

                define("@media(max-width:768px)") {
                    define(".header") {
                        flexDirection = CssFlexDirection.COLUMN
                        flexWrap = CssFlexWrap.NOWRAP

                        define(".still") {
                            width = "100%"
                        }
                    }
                }
            }
        }.cache()
    }

    @Get("/other", "text/html")
    context(database: Database)
    fun getOtherListPage(): Result {
        val others = transaction(database) {
            Other
                .all()
                .orderBy(OtherTable.title to SortOrder.ASC)
                .toList()
        }

        return bundle {
            html {
                head {
                    meta(charset = "utf-8")
                    meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
                    link(rel = "manifest", href = "/manifest.json")
                    title("Others")
                }

                body {
                    main {
                        h1 { +"Others" }

                        +component(::MediaListComponent) {
                            items = others.map {
                                MediaListItem(
                                    href = "/other/${it.id}",
                                    title = it.title,
                                )
                            }
                            mode = MediaListMode.LIST
                        }
                    }
                }
            }

            style {
                globalStyle()
            }
        }.cache()
    }

    @Get("/other/[id]", "text/html")
    context(database: Database)
    fun getOtherDetailPage(@PathParameter id: Uuid): Result {
        val other = transaction(database) { Other.findById(id) }
            ?: throw NotFoundSignal()

        return bundle {
            html {
                body {
                    main {
                        h1 { +other.title }


                        p {
                            button({ type = HtmlButtonElementType.BUTTON }) {
                                +"Play"

                                on("click") {
                                    val response = window.fetch(
                                        "/resource/other/${other.id}/playback",
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
                                        emitShareUrl(JsString(other.title), url)
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
