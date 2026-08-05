package dev.scriptor

import java.util.logging.ConsoleHandler
import java.util.logging.Formatter
import java.util.logging.LogRecord
import java.util.logging.Logger

fun getLogger(name: String, parent: Logger? = null): Logger {
    val log = Logger.getLogger(name)

    if (parent != null) {
        log.parent = parent
        log.useParentHandlers = true
    } else {
        val handler = ConsoleHandler()

        handler.formatter = object : Formatter() {
            override fun format(record: LogRecord): String {
                return "[${record.loggerName}][${record.level}][${record.instant}] ${record.message}\r\n"
            }
        }

        log.addHandler(handler)

        log.useParentHandlers = false
    }

    return log
}
