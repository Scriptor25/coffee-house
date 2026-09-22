package dev.scriptor.component

import dev.scriptor.ui.Bundle
import dev.scriptor.ui.Component
import dev.scriptor.ui.css.CssNode
import dev.scriptor.ui.css.builder.*
import dev.scriptor.ui.dom.Attribute
import dev.scriptor.ui.dom.AttributeValue
import dev.scriptor.ui.dom.Node
import dev.scriptor.ui.html.builder.HtmlGenericBuilder

class MediaListItemComponent : Component {

    lateinit var listItem: MediaListItem
    lateinit var listMode: MediaListMode

    context(bundle: Bundle)
    override fun build(): Node {
        return HtmlGenericBuilder(
            false,
            "li",
            listOf(
                Attribute("class", AttributeValue.StringValue("item")),
                Attribute("data-mode", AttributeValue.StringValue(listMode.value)),
            ),
        ).apply {
            +listItem.thumbnail(
                "thumbnail",
                when (listMode) {
                    MediaListMode.LIST -> "(max-width: 768px) 100vw, 30vw"
                    else -> "(max-width: 600px) 50vw, 300px"
                },
            )

            div({ htmlClass = "content" }) {
                a({
                    htmlClass = "title"
                    href = listItem.href
                }) { +listItem.title }

                when (val description = listItem.description) {
                    null -> Unit
                    else ->
                        p({
                            htmlClass = "description"
                        }) { +description }
                }
            }
        }.build()
    }

    override fun style(): List<CssNode> {
        return CssNodesBuilder().apply {
            define("li.item") {
                position = "relative"

                display = CssDisplay.FLEX
                flexWrap = CssFlexWrap.NOWRAP

                backgroundColor = "var(--color-panel)"

                borderRadius = "var(--border-radius)"
                overflow = "hidden"

                define(".thumbnail") {
                    display = CssDisplay.BLOCK
                    position = "relative"

                    backgroundColor = "#111"

                    objectFit = "cover"
                }

                define("div.content") {
                    display = CssDisplay.FLEX
                    flexDirection = CssFlexDirection.COLUMN
                    flexWrap = CssFlexWrap.NOWRAP

                    gap = "var(--space-m)"

                    padding = "var(--space-m)"

                    overflow = "hidden"
                    textOverflow = "ellipsis"

                    fontSize = "var(--font-size-small)"

                    color = "var(--color-foreground)"
                }

                define("a.title") {
                    overflow = "hidden"

                    textOverflow = "ellipsis"
                    textDecoration = "none"

                    this["display"] = "-webkit-box"
                    this["-webkit-box-orient"] = "vertical"
                    this["-webkit-line-clamp"] = "1"

                    lineClamp = "1"

                    color = "var(--color-foreground)"
                    backgroundColor = "transparent"

                    fontWeight = "bold"

                    define("&::after") {
                        content = "''"
                        position = "absolute"
                        inset = "0"
                    }
                }

                define("&[data-mode='grid']") {
                    flexDirection = CssFlexDirection.COLUMN

                    define(".thumbnail") {
                        maxWidth = "100%"

                        width = "100%"
                        height = "auto"

                        aspectRatio = "5 / 3"
                    }

                    define("div.content") {
                        alignItems = CssAlignItems.CENTER
                        justifyContent = CssJustifyContent.CENTER
                    }
                }

                define("&[data-mode='grid-poster']") {
                    flexDirection = CssFlexDirection.COLUMN

                    define(".thumbnail") {
                        maxWidth = "100%"

                        width = "100%"
                        height = "auto"

                        aspectRatio = "3 / 4"
                    }

                    define("div.content") {
                        alignItems = CssAlignItems.CENTER
                        justifyContent = CssJustifyContent.CENTER
                    }
                }

                define("&[data-mode='list']") {
                    flexDirection = CssFlexDirection.ROW

                    alignItems = CssAlignItems.STRETCH

                    define(".thumbnail") {
                        width = "30vw"
                        height = "100%"

                        aspectRatio = "2 / 1"

                        flexShrink = "0"
                    }

                    define("div.content") {
                        height = "auto"

                        alignItems = CssAlignItems.FLEX_START
                        justifyContent = CssJustifyContent.STRETCH
                    }

                    define("@media (max-width: 768px)") {
                        flexDirection = CssFlexDirection.COLUMN

                        alignItems = CssAlignItems.STRETCH
                        justifyContent = CssJustifyContent.STRETCH

                        define(".thumbnail") {
                            width = "100%"
                            height = "auto"
                        }
                    }
                }

                define("&[data-mode='list-compact']") {
                    flexDirection = CssFlexDirection.ROW

                    alignItems = CssAlignItems.STRETCH

                    define(".thumbnail") {
                        display = CssDisplay.NONE
                    }

                    define("div.content") {
                        height = "auto"

                        alignItems = CssAlignItems.FLEX_START
                        justifyContent = CssJustifyContent.STRETCH
                    }
                }

                define("&:has(.title:is(:hover, :focus-visible))") {
                    backgroundColor = "var(--color-panel-active)"
                    boxShadow = "5px 5px 10px #111"

                    define("a.title") {
                        textDecoration = "underline"
                    }
                }
            }
        }.build()
    }
}