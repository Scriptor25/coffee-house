package dev.scriptor

import java.util.logging.Logger

fun Process.attach(log: Logger) {
    this.errorStream.bufferedReader().useAsync {
        useLines { lines -> lines.forEach { line -> log.info(line) } }
    }
}
