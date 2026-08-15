package dev.scriptor

import java.util.logging.Level
import java.util.logging.Logger

fun Process.attach(log: Logger, level: Level = Level.INFO) {
    errorStream
        .bufferedReader()
        .useLinesAsync { lines -> lines.forEach { line -> log.log(level, line) } }
}

context(parent: Logger)
fun start(vararg command: String, name: String = command[0]): Process {
    return start(command.asList(), name)
}

context(parent: Logger)
fun start(command: List<String>, name: String = command[0]): Process {
    val log = getLogger(name, parent)

    log.fine(command.joinToString("' '", "'", "'"))

    val process = ProcessBuilder(command).start()

    process.attach(log, Level.FINEST)

    return process
}
