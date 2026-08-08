package dev.scriptor.model

interface Track {
    val index: Int
    val codec: String
    val language: String?
    val title: String?
    val default: Boolean
}
