package dev.scriptor.component

import dev.scriptor.ui.Bundle
import dev.scriptor.ui.dom.Attribute
import dev.scriptor.ui.dom.AttributeValue
import dev.scriptor.ui.dom.Node
import dev.scriptor.ui.html.HtmlElement

data class MediaListItem(
    val href: String,
    val title: String,
    val thumbnail: (context(Bundle) (className: String?, sizes: String?) -> List<Node>) = { className, _ ->
        listOf(
            HtmlElement(
                false,
                "div",
                buildList {
                    if (className != null) {
                        this += Attribute("class", AttributeValue.StringValue(className))
                    }
                },
                emptyList(),
            ),
        )
    },
)
