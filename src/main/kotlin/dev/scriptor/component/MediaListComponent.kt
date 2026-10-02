package dev.scriptor.component

import dev.scriptor.ui.Bundle
import dev.scriptor.ui.Component
import dev.scriptor.ui.component
import dev.scriptor.ui.css.CssNode
import dev.scriptor.ui.css.builder.CssDisplay
import dev.scriptor.ui.css.builder.CssNodesBuilder
import dev.scriptor.ui.dom.Attribute
import dev.scriptor.ui.dom.AttributeValue
import dev.scriptor.ui.dom.Node
import dev.scriptor.ui.html.builder.HtmlGenericBuilder

class MediaListComponent : Component {

    var items: List<MediaListItem> = emptyList()
    var end: MediaListItem? = null
    var mode: MediaListMode = MediaListMode.GRID

    context(bundle: Bundle)
    override fun build(): Node {
        return HtmlGenericBuilder(
            false,
            "ul",
            listOf(
                Attribute("class", AttributeValue.StringValue("list")),
                Attribute("data-mode", AttributeValue.StringValue(mode.value)),
            ),
        ).apply {
            for (item in items) {
                +component(::MediaListItemComponent) {
                    listItem = item
                    listMode = mode
                }
            }

            when (val item = end) {
                null -> Unit
                else -> {
                    +component(::MediaListItemComponent) {
                        listItem = item
                        listMode = mode
                    }
                }
            }
        }.build()
    }

    override fun style(): List<CssNode> {
        return CssNodesBuilder().apply {
            define(".list") {
                display = CssDisplay.GRID
                gap = "var(--space-m)"

                padding = "0"
                margin = "0"

                listStyle = "none"

                define("&[data-mode='grid'], &[data-mode='grid-poster']") {
                    gridTemplateColumns = "repeat(auto-fill, minmax(300px, 1fr))"
                }

                define("&[data-mode='list']") {
                    gridTemplateColumns = "1fr"
                }
            }
        }.build()
    }
}
