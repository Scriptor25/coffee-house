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

                define(".title") {
                    margin = "var(--space-m)"

                    overflow = "hidden"

                    textOverflow = "ellipsis"

                    this["display"] = "-webkit-box"
                    this["-webkit-box-orient"] = "vertical"
                    this["-webkit-line-clamp"] = "1"

                    lineClamp = "1"

                    fontSize = "var(--font-size-small)"

                    textDecoration = "none"
                    color = "var(--color-foreground)"
                    backgroundColor = "transparent"

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

                    define(".title") {
                        textAlign = "center"
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

                    define(".title") {
                        textAlign = "center"
                    }
                }

                define("&[data-mode='list']") {
                    flexDirection = CssFlexDirection.ROW

                    alignItems = CssAlignItems.CENTER

                    define(".thumbnail") {
                        width = "30vw"
                        height = "100%"

                        aspectRatio = "2 / 1"

                        flexShrink = "0"
                    }
                }

                define("&:has(.title:is(:hover, :focus-visible))") {
                    backgroundColor = "var(--color-panel-active)"
                    boxShadow = "5px 5px 10px #111"

                    define(".title") {
                        textDecoration = "underline"
                    }
                }
            }
        }.build()
    }
}