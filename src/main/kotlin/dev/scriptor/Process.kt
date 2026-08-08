package dev.scriptor

import java.util.logging.Level
import java.util.logging.Logger

fun Process.attach(log: Logger, level: Level = Level.INFO) {
    this.errorStream.bufferedReader().useAsync {
        useLines { lines -> lines.forEach { line -> log.log(level, line) } }
    }
}
