package dev.scriptor.model

import dev.scriptor.Entity

interface Track : Entity {
    val index: Int
    val codec: String

    val language: String?
    val title: String?

    val default: Boolean
}