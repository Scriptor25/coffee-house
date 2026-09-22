package dev.scriptor.component

import dev.scriptor.ui.Bundle
import dev.scriptor.ui.Component
import dev.scriptor.ui.css.CssNode
import dev.scriptor.ui.css.builder.*
import dev.scriptor.ui.dom.Attribute
import dev.scriptor.ui.dom.AttributeValue
import dev.scriptor.ui.dom.Node
import dev.scriptor.ui.html.builder.HtmlGenericBuilder

class HeaderComponent : Component {

    context(bundle: Bundle)
    override fun build(): Node {
        return HtmlGenericBuilder(
            false,
            "header",
            listOf(
                Attribute("class", AttributeValue.StringValue("header")),
            ),
        ).apply {
            a({ href = "/" }) { +"Dashboard" }
        }.build()
    }

    override fun style(): List<CssNode> {
        return CssNodesBuilder().apply {
            define("header.header") {
                position = "sticky"
                top = "0"
                left = "0"
                right = "0"

                zIndex = "1"

                display = CssDisplay.FLEX
                flexDirection = CssFlexDirection.ROW
                flexWrap = CssFlexWrap.NOWRAP
                alignItems = CssAlignItems.CENTER
                justifyContent = CssJustifyContent.SPACE_BETWEEN

                backgroundColor = "var(--color-panel)"

                padding = "var(--space-m)"
            }
        }.build()
    }
}
