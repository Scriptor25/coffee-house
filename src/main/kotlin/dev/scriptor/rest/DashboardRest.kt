package dev.scriptor.rest

import dev.scriptor.component.*
import dev.scriptor.context.SessionContext
import dev.scriptor.db
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
import dev.scriptor.ui.js.JsNull
import dev.scriptor.ui.js.JsNull.minus
import dev.scriptor.ui.js.JsSymbol
import dev.scriptor.ui.js.builder.JsNodeBuilder
import org.jetbrains.exposed.v1.core.SortOrder
import java.net.URLDecoder
import kotlin.math.cos
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

    private fun Bundle.cache(statusCode: Int = 200, statusText: String = "OK"): Result {
        val headers = ParameterList(
            "cache-control" to "public, max-age=86400, immutable",
        )

        return StringResult(
            statusCode = statusCode,
            statusText = statusText,
            contentType = "text/html",
            headers = headers,
            value = toString(),
        )
    }

    private fun Bundle.nocache(statusCode: Int = 200, statusText: String = "OK"): Result {
        return StringResult(
            statusCode = statusCode,
            statusText = statusText,
            contentType = "text/html",
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

        define("#gamepad-overlay") {
            display = CssDisplay.NONE
            position = "fixed"
            inset = "0"
            zIndex = "2147483647"
            cursor = "none"
            pointerEvents = "auto"
        }

        define("html.gamepad-active") {
            cursor = "none !important"

            define("*") {
                cursor = "none !important"
            }

            define("#gamepad-overlay") {
                display = CssDisplay.BLOCK
            }
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

        define("ol, ul") {
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
            margin = "0 0 var(--space-m) 0"
        }

        define(".container") {
            width = "100%"
            marginLeft = "auto"
            marginRight = "auto"

            define("@media (max-width:719px)") {
                paddingLeft = "var(--space-s)"
                paddingRight = "var(--space-s)"
            }

            define("@media (min-width:720px)") {
                width = "700px"
            }

            define("@media (min-width:960px)") {
                width = "900px"
            }

            define("@media (min-width:1200px)") {
                width = "1000px"
            }
        }

        define("input, select") {
            border = "none"
            padding = "var(--space-xs)"
            backgroundColor = "var(--color-panel)"

            define("&:hover, &:focus-visible") {
                backgroundColor = "var(--color-panel-active)"
            }
        }

        define("select") {
            cursor = "pointer"

            define("&.active") {
                outline = "2px solid var(--color-foreground)"
            }
        }
    }

    fun <T> JsNodeBuilder<T>.emitGamepadNavigation() {
        val selector = jsConst("selector", "[data-gamepad]")
        val initialRepeatDelay = jsConst("initialRepeatDelay", 400)
        val repeatInterval = jsConst("repeatInterval", 120)

        val activeGamepad = jsLet("activeGamepad", JsNull)

        val direction = jsLet("direction", JsNull)
        val directionStarted = jsLet("directionStarted", 0)
        val lastRepeat = jsLet("lastRepeat", 0)

        val previousButtons = jsLet("previousButtons", jsArray())

        val stickStart = jsConst("stickStart", 0.45)
        val stickRelease = jsConst("stickRelease", 0.20)

        val stickDirection = jsLet("stickDirection", JsNull)

        val inputLocked = jsLet("inputLocked", true)

        val selectEditing = jsLet("selectEditing", false)
        val selectOriginalIndex = jsLet("selectOriginalIndex", -1)

        val scrollDeadzone = jsConst("scrollDeadzone", 0.15)
        val scrollSpeed = jsConst("scrollSpeed", 20)

        val hideCursor = jsFunction(name = "hideCursor") {
            emit(window.sessionStorage.setItem("gamepad-active", jsString("1")))
            emit(document.documentElement.classList["add"](jsString("gamepad-active")))
        }

        val showCursor = jsFunction(name = "showCursor") {
            emit(window.sessionStorage.removeItem("gamepad-active"))
            emit(document.documentElement.classList["remove"](jsString("gamepad-active")))
        }

        jsIf(window.sessionStorage.getItem("gamepad-active") seq "1") {
            emit(document.documentElement.classList["add"](jsString("gamepad-active")))
        }

        val getElements = jsFunction(name = "getElements") {
            val elements = jsConst("elements", jsArray(document.querySelectorAll(selector).spread()))

            jsReturn(
                elements["filter"](
                    jsFunction("element") { (element) ->
                        jsIf(element["disabled"]) {
                            jsReturn(false)
                        }

                        val style = jsConst("style", window["getComputedStyle"](element))

                        jsIf(style["display"] seq "none") {
                            jsReturn(false)
                        }

                        jsIf(style["visibility"] seq "hidden") {
                            jsReturn(false)
                        }

                        jsIf(element["offsetParent"] seq JsNull) {
                            jsReturn(false)
                        }

                        jsReturn(true)
                    }
                )
            )
        }

        val center = jsFunction("rect", name = "center") { (rect) ->
            jsReturn(
                jsObject {
                    this["x"] = rect["left"] + (rect["width"] / 2)
                    this["y"] = rect["top"] + (rect["height"] / 2)
                },
            )
        }

        val changeSelect = jsFunction("select", "direction", name = "changeSelect") { (select, direction) ->
            val options = jsConst("options", jsArray(select["options"].spread()))
            val index = jsLet("index", select["selectedIndex"])

            jsIf(index lt 0) {
                emit(
                    index assign jsTernary(
                        direction gt 0,
                        jsNumber(-1),
                        options["length"],
                    ),
                )
            }

            jsWhile(jsBoolean(true)) {
                emit(index assign index + direction)

                jsIf((index lt 0) or (index gte options["length"])) {
                    jsReturn()
                }

                jsIf(!options[index]["disabled"]) {
                    jsBreak()
                }
            }

            emit(select["selectedIndex"] assign index)
        }

        val enterSelect = jsFunction("select", name = "enterSelect") { (select) ->
            emit(selectEditing assign true)
            emit(selectOriginalIndex assign select["selectedIndex"])

            emit(select["classList"]["add"](jsString("active")))
        }

        val submitSelect = jsFunction("select", name = "submitSelect") { (select) ->
            emit(
                select["dispatchEvent"](
                    JsSymbol("Event").new(
                        jsString("change"),
                        jsObject {
                            this["bubbles"] = jsBoolean(true)
                        },
                    ),
                ),
            )
            emit(selectEditing assign false)
            emit(selectOriginalIndex assign -1)

            emit(select["classList"]["remove"](jsString("active")))
        }

        val cancelSelect = jsFunction("select", name = "cancelSelect") { (select) ->
            emit(select["selectedIndex"] assign selectOriginalIndex)
            emit(selectEditing assign false)
            emit(selectOriginalIndex assign -1)

            emit(select["classList"]["remove"](jsString("active")))
        }

        val focus = jsFunction("element", name = "focus") { (element) ->
            emit(
                element["focus"](
                    jsObject {
                        this["preventScroll"] = jsBoolean(false)
                    },
                ),
            )
        }

        val navigate = jsFunction("dir", name = "navigate") { (dir) ->
            emit(hideCursor())

            val current = jsConst("current", document["activeElement"])

            jsIf(selectEditing and (current instanceof JsSymbol("HTMLSelectElement"))) {
                jsIf((dir seq "up") or (dir seq "left")) {
                    emit(changeSelect(current, jsNumber(-1)))
                    jsReturn()
                }

                jsIf((dir seq "down") or (dir seq "right")) {
                    emit(changeSelect(current, jsNumber(1)))
                    jsReturn()
                }

                jsReturn()
            }

            val elements = jsConst("elements", getElements())
            val vertical = jsConst("vertical", (dir seq "up") or (dir seq "down"))

            jsIf(!elements["length"]) {
                jsReturn()
            }

            jsIf(!elements["includes"](current)) {
                emit(focus(elements[0]))
                jsReturn()
            }

            val rect = jsConst("rect", current["getBoundingClientRect"]())
            val origin = jsConst("origin", center(rect))

            val directionX = jsLet("directionX", 0)
            val directionY = jsLet("directionY", 0)

            jsSwitch(dir) {
                case("left") {
                    emit(directionX assign -1)
                    jsBreak()
                }

                case("right") {
                    emit(directionX assign 1)
                    jsBreak()
                }

                case("up") {
                    emit(directionY assign -1)
                    jsBreak()
                }

                case("down") {
                    emit(directionY assign 1)
                    jsBreak()
                }
            }

            val frustumCos = jsTernary(
                vertical,
                jsNumber(cos(Math.PI / 2.1)),
                jsNumber(cos(Math.PI / 4)),
            )

            val candidates = jsConst("candidates", jsArray())

            jsForEachConstOf("element", elements) { element ->
                jsIf(element seq current) {
                    jsContinue()
                }

                val candidateRect = jsConst("candidateRect", element["getBoundingClientRect"]())
                val target = jsConst("target", center(candidateRect))

                val dx = jsConst("dx", target["x"] - origin["x"])
                val dy = jsConst("dy", target["y"] - origin["y"])

                val length = jsConst("length", math.hypot(dx, dy))

                jsIf(!length) {
                    jsContinue()
                }

                val forward = jsConst("forward", ((dx * directionX) + (dy * directionY)) / length)

                jsIf(forward lt frustumCos) {
                    jsContinue()
                }

                val forwardDistance = jsLet("forwardDistance")
                val sidewaysDistance = jsLet("sidewaysDistance")

                jsIfElse(
                    vertical,
                    thenBlock = {
                        emit(
                            forwardDistance assign jsTernary(
                                directionY gt 0,
                                candidateRect["top"] - rect["bottom"],
                                rect["top"] - candidateRect["bottom"],
                            ),
                        )
                        emit(
                            sidewaysDistance assign math.max(
                                candidateRect["left"] - rect["right"],
                                rect["left"] - candidateRect["right"],
                                jsNumber(0),
                            ),
                        )
                    },
                    elseBlock = {
                        emit(
                            forwardDistance assign jsTernary(
                                directionX gt 0,
                                candidateRect["left"] - rect["right"],
                                rect["left"] - candidateRect["right"],
                            ),
                        )
                        emit(
                            sidewaysDistance assign math.max(
                                candidateRect["top"] - rect["bottom"],
                                rect["top"] - candidateRect["bottom"],
                                jsNumber(0),
                            ),
                        )
                    },
                )

                emit(forwardDistance assign math.max(jsNumber(0), forwardDistance))

                emit(
                    candidates["push"](
                        jsObject {
                            this["element"] = element
                            this["score"] = forwardDistance + (sidewaysDistance * 2)
                        }
                    ),
                )
            }

            jsIf(!candidates["length"]) {
                jsReturn()
            }

            emit(
                candidates["sort"](
                    jsFunction("a", "b") { (a, b) ->
                        jsReturn(a["score"] - b["score"])
                    },
                ),
            )

            val target = jsConst("target", candidates[0]["element"])

            emit(focus(target))
        }

        val buttonPressed = jsFunction("index", name = "buttonPressed") { (index) ->
            jsReturn(!!(activeGamepad and activeGamepad["buttons"][index] and activeGamepad["buttons"][index]["pressed"]))
        }

        val allButtonsReleased = jsFunction(name = "allButtonsReleased") {
            jsIf(!activeGamepad) {
                jsReturn(true)
            }

            jsReturn(
                activeGamepad["buttons"]["every"](
                    jsFunction("button") { (button) ->
                        jsReturn(!button["pressed"])
                    },
                ),
            )
        }

        val justPressed = jsFunction("index", name = "justPressed") { (index) ->
            jsIf(inputLocked) {
                jsReturn(false)
            }

            val current = jsConst("current", buttonPressed(index))
            val notPrevious = jsConst("notPrevious", !previousButtons[index])

            jsReturn(current and notPrevious)
        }

        val getStickDirection = jsFunction("x", "y", name = "getStickDirection") { (x, y) ->
            jsIf(stickDirection) {
                val stillHeld = jsConst(
                    "stillHeld",
                    jsTernary(
                        stickDirection seq "left",
                        x lt -stickRelease,
                        jsTernary(
                            stickDirection seq "right",
                            x gt stickRelease,
                            jsTernary(
                                stickDirection seq "up",
                                y lt -stickRelease,
                                jsTernary(
                                    stickDirection seq "down",
                                    y gt stickRelease,
                                    jsBoolean(false),
                                ),
                            ),
                        ),
                    ),
                )

                jsIf(stillHeld) {
                    jsReturn(stickDirection)
                }

                emit(stickDirection assign JsNull)
            }

            jsIf((math.abs(x) lt stickStart) and (math.abs(y) lt stickStart)) {
                jsReturn(JsNull)
            }

            jsIfElse(
                math.abs(x) gt math.abs(y),
                thenBlock = {
                    emit(
                        stickDirection assign jsTernary(
                            x lt 0,
                            jsString("left"),
                            jsString("right"),
                        ),
                    )
                },
                elseBlock = {
                    emit(
                        stickDirection assign jsTernary(
                            y lt 0,
                            jsString("up"),
                            jsString("down"),
                        ),
                    )
                },
            )

            jsReturn(stickDirection)
        }

        val getDirection = jsFunction(name = "getDirection") {
            jsIf(!activeGamepad) {
                jsReturn(JsNull)
            }

            val buttons = jsConst("buttons", activeGamepad["buttons"])

            jsIf(buttons[12] and buttons[12]["pressed"]) {
                jsReturn("up")
            }

            jsIf(buttons[13] and buttons[13]["pressed"]) {
                jsReturn("down")
            }

            jsIf(buttons[14] and buttons[14]["pressed"]) {
                jsReturn("left")
            }

            jsIf(buttons[15] and buttons[15]["pressed"]) {
                jsReturn("right")
            }

            val x = jsConst("x", activeGamepad["axes"][0] or jsNumber(0))
            val y = jsConst("y", activeGamepad["axes"][1] or jsNumber(0))

            jsReturn(getStickDirection(x, y))
        }

        val processDirection = jsFunction("now", name = "processDirection") { (now) ->
            val nextDirection = jsConst("nextDirection", getDirection())

            jsIf(!nextDirection) {
                emit(direction assign JsNull)
                jsReturn()
            }

            jsIf(nextDirection sne direction) {
                emit(direction assign nextDirection)
                emit(directionStarted assign now)
                emit(lastRepeat assign now)

                emit(navigate(direction))
                jsReturn()
            }

            jsIf(now - directionStarted lt initialRepeatDelay) {
                jsReturn()
            }

            jsIf(now - lastRepeat gte repeatInterval) {
                emit(lastRepeat assign now)
                emit(navigate(direction))
            }
        }

        val applyDeadzone = jsFunction("value", "deadzone", name = "applyDeadzone") { (value, deadzone) ->
            jsIf(math.abs(value) lt deadzone) {
                jsReturn(0)
            }

            val sign = jsConst("sign", math.sign(value))
            val magnitude = jsConst("magnitude", (math.abs(value) - deadzone) / (1 - deadzone))

            jsReturn(sign * magnitude)
        }

        val updateScroll = jsFunction("gamepad", name = "updateScroll") { (gamepad) ->
            val x = jsLet("x", applyDeadzone(gamepad["axes"][2] or jsNumber(0), scrollDeadzone))
            val y = jsLet("y", applyDeadzone(gamepad["axes"][3] or jsNumber(0), scrollDeadzone))

            jsIf(!x and !y) {
                jsReturn()
            }

            emit(hideCursor())

            emit(x assign (math.sign(x) * x * x))
            emit(y assign (math.sign(y) * y * y))

            emit(
                window.scrollBy(
                    jsObject {
                        this["left"] = x * scrollSpeed
                        this["top"] = y * scrollSpeed
                    }
                )
            )
        }

        val update = jsFunction("now", name = "update") { update, (now) ->
            val pads = jsConst("pads", window.navigator.getGamepads())

            jsIf(activeGamepad) {
                val current = jsConst("current", pads[activeGamepad["index"]])

                jsIf(current) {
                    emit(activeGamepad assign current)
                }
            }

            jsIf(activeGamepad) {
                emit(updateScroll(activeGamepad))

                jsIf(inputLocked) {
                    jsIf(allButtonsReleased()) {
                        emit(inputLocked assign false)

                        emit(
                            previousButtons assign activeGamepad["buttons"]["map"](
                                jsFunction("button") { (button) ->
                                    jsReturn(button["pressed"])
                                },
                            ),
                        )
                    }

                    emit(window.requestAnimationFrame(update))
                    jsReturn()
                }

                emit(processDirection(now))

                jsIf(justPressed(jsNumber(0))) {
                    emit(hideCursor())

                    val current = jsConst("current", document["activeElement"])

                    jsIfElse(
                        current instanceof JsSymbol("HTMLSelectElement"),
                        thenBlock = {
                            jsIfElse(
                                selectEditing,
                                thenBlock = {
                                    emit(submitSelect(current))
                                },
                                elseBlock = {
                                    emit(enterSelect(current))
                                },
                            )
                        },
                        elseBlock = {
                            emit(inputLocked assign true)
                            emit(current["click"]())
                        },
                    )
                }

                jsIf(justPressed(jsNumber(1))) {
                    emit(hideCursor())

                    val current = jsConst("current", document["activeElement"])

                    jsIfElse(
                        selectEditing and (current instanceof JsSymbol("HTMLSelectElement")),
                        thenBlock = {
                            emit(cancelSelect(current))
                        },
                        elseBlock = {
                            emit(inputLocked assign true)
                            emit(window.history.back())
                        },
                    )
                }

                emit(
                    previousButtons assign activeGamepad["buttons"]["map"](
                        jsFunction("button") { (button) ->
                            jsReturn(button["pressed"])
                        },
                    ),
                )
            }

            emit(window.requestAnimationFrame(update))
        }

        emit(window.addEventListener("gamepadconnected", jsFunction("event") { (event) ->
            emit(activeGamepad assign event["gamepad"])

            emit(console.log(jsString("gamepad connected:"), activeGamepad["id"]))

            val first = jsConst("first", getElements()[0])

            jsIf(first and !(document["activeElement"] and document["activeElement"]["matches"](selector))) {
                emit(focus(first))
            }
        }))

        emit(window.addEventListener("gamepaddisconnected", jsFunction("event") { (event) ->
            jsIf(activeGamepad and (activeGamepad["index"] seq event["gamepad"]["index"])) {
                emit(activeGamepad assign JsNull)
                emit(direction assign JsNull)
                emit(stickDirection assign JsNull)
                emit(previousButtons assign jsArray())
            }
        }))

        val overlay = document.getElementById("gamepad-overlay")

        emit(overlay.addEventListener("mousemove", jsFunction {
            emit(showCursor())
        }))

        emit(window.requestAnimationFrame(update))
    }

    fun <T> JsNodeBuilder<T>.emitPlayback(
        resource: String,
        id: Uuid,
        title: String,
        playlist: String = "playlist.m3u8",
    ) {
        val result = window
            .fetch(
                "/resource/$resource/${id}/playback",
                jsObject {
                    this["method"] = jsString("post")
                },
            )
            .then { (response) ->
                jsReturn(response["text"]())
            }
            .then { (text) ->
                val origin = window.location.origin
                val url = jsFormat("", "/resource/playback/", "/$playlist", values = listOf(origin, text))

                jsReturn(url)
            }
            .then { (url) ->
                jsIf(window.navigator.share) {
                    emit(
                        window.navigator.share(
                            jsObject {
                                this["title"] = jsString(title)
                                this["url"] = url
                            },
                        ),
                    )
                    jsReturn()
                }

                jsIf(window.navigator.clipboard) {
                    // TODO: display message popup
                    emit(window.navigator.clipboard.writeText(url))
                    jsReturn()
                }

                emit(window.open(url))
            }

        emit(result)
    }

    @Public
    @Get("/manifest.json", "application/json")
    fun getManifest(): Result {
        val node = jsonObject {
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

        return StringResult(
            contentType = "application/json",
            headers = ParameterList(
                "cache-control" to "public, max-age=86400, immutable",
            ),
            value = node.toJson(),
        )
    }

    @Get("/", "text/html")
    context(principal: Principal)
    fun getDashboardPage(): Result {
        val movies = db {
            Movie
                .all()
                .orderBy(MovieTable.title to SortOrder.ASC)
                .limit(4)
                .toList()
        }

        val shows = db {
            Show
                .all()
                .orderBy(ShowTable.title to SortOrder.ASC)
                .limit(4)
                .toList()
        }

        val others = db {
            Other
                .all()
                .orderBy(OtherTable.title to SortOrder.ASC)
                .limit(4)
                .toList()
        }

        val user = db { User.findById(principal.id) }

        return bundle {
            html {
                head {
                    meta(charset = "utf-8")
                    meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
                    link(rel = "manifest", href = "/manifest.json")
                    title("Dashboard")
                }
                body {
                    div({ this.id = "gamepad-overlay" })

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

            script {
                emitGamepadNavigation()
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
                    div({ this.id = "gamepad-overlay" })

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
                                        booleanData("gamepad", true)
                                        type = HtmlInputElementType.TEXT
                                        name = "username"
                                        autoComplete = "username"
                                        required = true
                                    }
                                }
                                label {
                                    span { +"Password" }
                                    input {
                                        booleanData("gamepad", true)
                                        type = HtmlInputElementType.PASSWORD
                                        name = "password"
                                        autoComplete = "current-password"
                                        required = true
                                    }
                                }
                            }
                            button({
                                booleanData("gamepad", true)
                                type = HtmlButtonElementType.SUBMIT
                            }) {
                                +"Login"
                            }
                        }
                    }
                }
            }

            script {
                emitGamepadNavigation()
            }

            style {
                globalStyle()

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
    fun getMovieListPage(): Result {
        val movies = db {
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
                    div({ this.id = "gamepad-overlay" })

                    +component(::HeaderComponent) {}

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

            script {
                emitGamepadNavigation()
            }

            style {
                globalStyle()
            }
        }.cache()
    }

    @Get("/movie/[id]", "text/html")
    fun getMovieDetailPage(@PathParameter id: Uuid): Result {
        val movie = db { Movie.findById(id) }
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
                    div({ this.id = "gamepad-overlay" })

                    +component(::HeaderComponent) {
                        links = listOf(
                            "/movie" to "Movies",
                        )
                    }

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
                            button({
                                booleanData("gamepad", true)
                                type = HtmlButtonElementType.BUTTON
                            }) {
                                +"Play"

                                on("click") {
                                    emitPlayback(
                                        "movie",
                                        movie.id.value,
                                        movie.title,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            script {
                emitGamepadNavigation()
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

                define("main.content") {
                    marginLeft = "300px"
                }

                define("@media (max-width:768px)") {
                    define(".banner") {
                        define(".poster") {
                            display = CssDisplay.NONE
                        }
                    }

                    define("main.content") {
                        marginLeft = "0"
                    }
                }
            }
        }.cache()
    }

    @Get("/show", "text/html")
    fun getShowListPage(): Result {
        val shows = db {
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
                    div({ this.id = "gamepad-overlay" })

                    +component(::HeaderComponent) {}

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

            script {
                emitGamepadNavigation()
            }

            style {
                globalStyle()
            }
        }.cache()
    }

    @Get("/show/[id]", "text/html")
    fun getShowDetailPage(
        @PathParameter id: Uuid,
        @QueryParameter view: String? = null,
    ): Result {
        val show = db { Show.findById(id) }
            ?: throw NotFoundSignal()

        val viewGroup = view?.let { Uuid.parseOrNull(it) }?.let { db { ParentGroup.findById(it) } }
        if (viewGroup == null && view != null) {
            throw FoundSignal(
                ParameterList(
                    "location" to "/show/$id",
                ),
            )
        }

        val groups = db {
            show.groups.toList()
        }

        val items = when (viewGroup) {
            null -> db {
                val seasons = show.seasons

                seasons.map {
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
            }

            else -> db {
                val groups = viewGroup.groups.filter { !it.episodes.empty() }

                groups.map {
                    MediaListItem(
                        href = "/group/${it.id}",
                        title = it.title,
                        thumbnail = { className, sizes ->
                            component(::ImageComponent) {
                                this.className = className
                                this.sizes = sizes
                            }
                        }
                    )
                }
            }
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
                    div({ this.id = "gamepad-overlay" })

                    +component(::HeaderComponent) {
                        links = listOf(
                            "/show" to "Shows",
                        )
                    }

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

                        p {
                            label({ htmlClass = "view" }) {
                                span { +"Select Episode Order" }
                                select({
                                    booleanData("gamepad", true)
                                    name = "view"
                                }) {
                                    option({
                                        value = ""
                                        selected = viewGroup == null
                                    }) {
                                        +"Default"
                                    }

                                    for (group in groups) {
                                        option({
                                            value = group.id.toString()
                                            selected = group.id == viewGroup?.id
                                        }) {
                                            +group.title
                                        }
                                    }

                                    on("change") { (event) ->
                                        val id = event["target"]["value"]
                                        val url = jsFormat("?view=", "", values = listOf(id))

                                        emit(window.navigation.navigate(url))
                                    }
                                }
                            }
                        }

                        p {
                            if (viewGroup != null) {
                                span { +viewGroup.description }
                            }
                        }

                        when (val description = show.description) {
                            null -> {}
                            else -> {
                                p { +description }
                            }
                        }

                        h2 { +"Seasons" }

                        +component(::MediaListComponent) {
                            this.items = items
                            this.mode =
                                if (viewGroup != null)
                                    MediaListMode.LIST_COMPACT
                                else
                                    MediaListMode.GRID_POSTER
                        }
                    }
                }
            }

            script {
                emitGamepadNavigation()
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

                define("label.view") {
                    display = CssDisplay.INLINE_FLEX
                    flexDirection = CssFlexDirection.ROW
                    flexWrap = CssFlexWrap.NOWRAP
                    alignItems = CssAlignItems.CENTER
                    justifyContent = CssJustifyContent.FLEX_START

                    gap = "var(--space-m)"

                    define("> span") {
                        flexShrink = "0"
                    }

                    define("> select") {
                        flexGrow = "0"
                        width = "100%"
                    }
                }

                define("main.content") {
                    marginLeft = "300px"
                }

                define("@media (max-width:768px)") {
                    define(".banner") {
                        define(".poster") {
                            display = CssDisplay.NONE
                        }
                    }

                    define("label.view") {
                        flexDirection = CssFlexDirection.COLUMN
                        alignItems = CssAlignItems.FLEX_START
                    }

                    define("main.content") {
                        marginLeft = "0"
                    }
                }
            }
        }.cache()
    }

    @Get("/season/[id]", "text/html")
    fun getSeasonDetailPage(@PathParameter id: Uuid): Result {
        val season = db { Season.findById(id) }
            ?: throw NotFoundSignal()

        val show = db { season.show }
        val episodes = db { season.episodes.toList() }

        return bundle {
            html {
                head {
                    meta(charset = "utf-8")
                    meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
                    link(rel = "manifest", href = "/manifest.json")
                    title("${show.title} - ${season.title} | Shows")
                }

                body {
                    div({ this.id = "gamepad-overlay" })

                    +component(::HeaderComponent) {
                        links = listOf(
                            "/show" to "Shows",
                            "/show/${show.id}" to show.title,
                        )
                    }

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
                                    button({
                                        booleanData("gamepad", true)
                                        type = HtmlButtonElementType.BUTTON
                                    }) {
                                        +"Play all"

                                        on("click") {
                                            emitPlayback(
                                                "season",
                                                season.id.value,
                                                season.title,
                                            )
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
                                    title = "Ep. ${it.index}: ${it.title}",
                                    description = it.description,
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

            script {
                emitGamepadNavigation()
            }

            style {
                globalStyle()

                define("section.header") {
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

                        flexShrink = "0"
                    }
                }

                define("@media (max-width:768px)") {
                    define("section.header") {
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

    @Get("/group/[id]", "text/html")
    fun getGroupDetailPage(@PathParameter id: Uuid): Result {
        val group = db { Group.findById(id) }
            ?: throw NotFoundSignal()

        val show = db { group.parent.show }
        val parent = db { group.parent }
        val episodes = db { group.episodes.toList() }

        return bundle {
            html {
                head {
                    meta(charset = "utf-8")
                    meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
                    link(rel = "manifest", href = "/manifest.json")
                    title("${show.title} - ${group.title} | Shows")
                }

                body {
                    div({ this.id = "gamepad-overlay" })

                    +component(::HeaderComponent) {
                        links = listOf(
                            "/show" to "Shows",
                            "/show/${show.id}?view=${parent.id}" to show.title,
                        )
                    }

                    main {
                        section({ htmlClass = "header" }) {
                            div {
                                h1 { +group.title }

                                p {
                                    button({
                                        booleanData("gamepad", true)
                                        type = HtmlButtonElementType.BUTTON
                                    }) {
                                        +"Play all"

                                        on("click") {
                                            emitPlayback(
                                                "group",
                                                group.id.value,
                                                group.title,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        h2 { +"Episodes" }

                        +component(::MediaListComponent) {
                            items = episodes.mapIndexed { index, it ->
                                MediaListItem(
                                    href = "/episode/${it.id}",
                                    title = "Ep. ${index + 1}: ${it.title}",
                                    description = it.description,
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

            script {
                emitGamepadNavigation()
            }

            style {
                globalStyle()

                define("section.header") {
                    display = CssDisplay.FLEX
                    flexDirection = CssFlexDirection.ROW
                    flexWrap = CssFlexWrap.NOWRAP
                    gap = "var(--space-l)"

                    marginBottom = "var(--space-l)"
                }

                define("@media (max-width:768px)") {
                    define("section.header") {
                        flexDirection = CssFlexDirection.COLUMN
                        flexWrap = CssFlexWrap.NOWRAP
                    }
                }
            }
        }.cache()
    }

    @Get("/episode/[id]", "text/html")
    fun getEpisodeDetailPage(@PathParameter id: Uuid): Result {
        val episode = db { Episode.findById(id) }
            ?: throw NotFoundSignal()

        val season = db { episode.season }
        val show = db { season.show }

        return bundle {
            html {
                head {
                    meta(charset = "utf-8")
                    meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
                    link(rel = "manifest", href = "/manifest.json")
                    title("${show.title} - ${season.title} - ${episode.title} | Shows")
                }

                body {
                    div({ this.id = "gamepad-overlay" })

                    +component(::HeaderComponent) {
                        links = listOf(
                            "/show" to "Shows",
                            "/show/${show.id}" to show.title,
                            "/season/${season.id}" to season.title,
                        )
                    }

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
                                    button({
                                        booleanData("gamepad", true)
                                        type = HtmlButtonElementType.BUTTON
                                    }) {
                                        +"Play"

                                        on("click") {
                                            emitPlayback(
                                                "episode",
                                                episode.id.value,
                                                episode.title,
                                                "0/master.m3u8",
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            script {
                emitGamepadNavigation()
            }

            style {
                globalStyle()

                define("section.header") {
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

                define("@media (max-width:768px)") {
                    define("section.header") {
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
    fun getOtherListPage(): Result {
        val others = db {
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
                    div({ this.id = "gamepad-overlay" })

                    +component(::HeaderComponent) {}

                    main {
                        h1 { +"Others" }

                        +component(::MediaListComponent) {
                            items = others.map {
                                MediaListItem(
                                    href = "/other/${it.id}",
                                    title = it.title,
                                )
                            }
                            mode = MediaListMode.LIST_COMPACT
                        }
                    }
                }
            }

            script {
                emitGamepadNavigation()
            }

            style {
                globalStyle()
            }
        }.cache()
    }

    @Get("/other/[id]", "text/html")
    fun getOtherDetailPage(@PathParameter id: Uuid): Result {
        val other = db { Other.findById(id) }
            ?: throw NotFoundSignal()

        return bundle {
            html {
                head {
                    meta(charset = "utf-8")
                    meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
                    link(rel = "manifest", href = "/manifest.json")
                    title("Others")
                }

                body {
                    div({ this.id = "gamepad-overlay" })

                    +component(::HeaderComponent) {
                        links = listOf(
                            "/other" to "Others",
                        )
                    }

                    main {
                        h1 { +other.title }

                        p {
                            button({
                                booleanData("gamepad", true)
                                type = HtmlButtonElementType.BUTTON
                            }) {
                                +"Play"

                                on("click") {
                                    emitPlayback(
                                        "other",
                                        other.id.value,
                                        other.title,
                                        "0/master.m3u8",
                                    )
                                }
                            }
                        }
                    }
                }
            }

            script {
                emitGamepadNavigation()
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

    @Handle(NotFoundSignal::class)
    fun handleNotFoundSignal(request: Request, signal: NotFoundSignal): Result {

        return bundle {
            html {
                head {
                    meta(charset = "utf-8")
                    meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
                    link(rel = "manifest", href = "/manifest.json")
                    title("Not Found")
                }

                body {
                    div({ this.id = "gamepad-overlay" })

                    main {
                        h1 { +"404" }
                        h2 { +"Not Found" }

                        p { +"The requested resource does not exist." }
                        p {
                            a({
                                booleanData("gamepad", true)
                                href = "/"
                            }) {
                                +"Dashboard"
                            }
                        }
                    }
                }
            }

            script {
                emitGamepadNavigation()
            }

            style {
                globalStyle()
            }
        }.nocache(signal.code, signal.text)
    }
}
