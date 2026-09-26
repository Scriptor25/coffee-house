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

    var links: List<Pair<String, String>> = emptyList()

    context(bundle: Bundle)
    override fun build(): Node {
        return HtmlGenericBuilder(
            false,
            "header",
            listOf(
                Attribute("class", AttributeValue.StringValue("header")),
            ),
        ).apply {
            a({
                booleanData("gamepad", true)
                href = "/"
            }) { +"Dashboard" }
            for (link in links) {
                a({
                    booleanData("gamepad", true)
                    href = link.first
                }) { +link.second }
            }
        }.build()
    }

    override fun style(): List<CssNode> {
        return CssNodesBuilder().apply {
            define("header.header") {
                position = "sticky"
                top = "0"
                left = "0"
                right = "0"

                zIndex = "20"

                display = CssDisplay.FLEX
                flexDirection = CssFlexDirection.ROW
                flexWrap = CssFlexWrap.WRAP
                alignItems = CssAlignItems.CENTER
                justifyContent = CssJustifyContent.FLEX_START

                gap = "var(--space-s)"

                backgroundColor = "var(--color-panel)"

                padding = "var(--space-m)"
            }
        }.build()
    }
}
