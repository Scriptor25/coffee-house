package dev.scriptor.component

import dev.scriptor.model.movie.ImageData
import dev.scriptor.ui.Bundle
import dev.scriptor.ui.Component
import dev.scriptor.ui.dom.Attribute
import dev.scriptor.ui.dom.AttributeValue
import dev.scriptor.ui.dom.Node
import dev.scriptor.ui.html.HtmlElement

class ImageComponent : Component {

    var className: String? = null
    var sizes: String? = null
    var src: List<ImageData> = emptyList()
    var noFallback: Boolean = false

    context(bundle: Bundle)
    override fun build(): Node? {
        val attributes = mutableListOf<Attribute>()

        when (val value = className) {
            null -> Unit
            else -> {
                attributes += Attribute("class", AttributeValue.StringValue(value))
            }
        }

        when (val value = sizes) {
            null -> Unit
            else -> {
                attributes += Attribute("sizes", AttributeValue.StringValue(value))
            }
        }

        if (src.isEmpty()) {
            if (noFallback) {
                return null
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

        attributes += Attribute("srcset", AttributeValue.StringValue(srcset))

        return HtmlElement(
            true,
            "img",
            attributes,
            listOf(),
        )
    }
}
