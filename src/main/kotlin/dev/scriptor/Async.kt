package dev.scriptor

import java.io.Closeable

inline fun <reified T : Closeable?> T.useAsync(name: String, noinline block: T.() -> Unit): Thread {
    val thread = Thread({ use<T, Unit>(block) }, name)
    thread.start()
    return thread
}

inline fun <reified T : Closeable?> T.useAsync(noinline block: T.() -> Unit): Thread {
    val thread = Thread { use<T, Unit>(block) }
    thread.start()
    return thread
}
