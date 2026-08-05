package dev.scriptor.model

interface Track {
    val type: String

    val index: Int
    val codec: String

    val language: String?
    val title: String?

    val default: Boolean
}