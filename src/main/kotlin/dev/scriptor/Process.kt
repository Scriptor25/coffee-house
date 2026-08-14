package dev.scriptor

import java.util.logging.Level
import java.util.logging.Logger

fun Process.attach(log: Logger, level: Level = Level.INFO) {
    errorStream
        .bufferedReader()
        .useLinesAsync { lines -> lines.forEach { line -> log.log(level, line) } }
}
