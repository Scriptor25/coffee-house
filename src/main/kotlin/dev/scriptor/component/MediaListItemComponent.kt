package dev.scriptor.component

import dev.scriptor.ui.Bundle
import dev.scriptor.ui.Component
import dev.scriptor.ui.css.CssNode
import dev.scriptor.ui.css.builder.CssDisplay
import dev.scriptor.ui.css.builder.CssFlexDirection
import dev.scriptor.ui.css.builder.CssFlexWrap
import dev.scriptor.ui.css.builder.CssNodesBuilder
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
                    MediaListMode.LIST -> "30vw"
                    else -> "(max-width: 600px) 50vw, 300px"
                },
            )

            a({
                htmlClass = "title"
                href = listItem.href
            }) { +listItem.title }
        }.build()
    }

    override fun style(): List<CssNode> {
        return CssNodesBuilder().apply {
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
        }.build()
    }
}