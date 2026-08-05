package dev.scriptor

import java.util.logging.*

fun getLogger(name: String, parent: Logger? = null): Logger {
    val log = Logger.getLogger(name)

    if (parent != null) {
        log.parent = parent
        log.useParentHandlers = true
        log.level = null
    } else {
        val handler = ConsoleHandler()

        handler.level = Level.ALL

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
