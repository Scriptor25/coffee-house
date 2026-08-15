package dev.scriptor

import java.io.BufferedReader
import java.io.Closeable

inline fun <reified T : Closeable?> T.useAsync(name: String, crossinline block: T.() -> Unit): Thread {
    val thread = Thread({ use(block) }, name)
    thread.start()
    return thread
}

inline fun <reified T : Closeable?> T.useAsync(crossinline block: T.() -> Unit): Thread {
    val thread = Thread { use(block) }
    thread.start()
    return thread
}

inline fun BufferedReader.useLinesAsync(name: String, crossinline block: (Sequence<String>) -> Unit): Thread {
    val thread = Thread({ useLines(block) }, name)
    thread.start()
    return thread
}

inline fun BufferedReader.useLinesAsync(crossinline block: (Sequence<String>) -> Unit): Thread {
    val thread = Thread { useLines(block) }
    thread.start()
    return thread
}
