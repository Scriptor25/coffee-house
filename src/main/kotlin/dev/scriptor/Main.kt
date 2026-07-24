package dev.scriptor

import dev.scriptor.context.SessionContext
import dev.scriptor.server.scan
import java.lang.System.getenv
import java.sql.DriverManager
import java.sql.Timestamp
import java.util.logging.*
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.walk
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

fun getenv(name: String, default: String): String {
    return getenv(name) ?: default
}

@OptIn(ExperimentalUuidApi::class)
fun main() {
    val hostname = getenv("HOSTNAME", "0.0.0.0")
    val port = getenv("PORT", "8080").toInt()
    val data = getenv("DATA", "/data")

    val log = Logger.getLogger("dev.scriptor")
    log.level = Level.INFO

    val handler = ConsoleHandler()
    handler.level = log.level
    handler.formatter = object : Formatter() {

        override fun format(record: LogRecord?): String? {
            if (record == null) return null

            return "[${record.level}][${record.instant}][${record.sourceClassName}.${record.sourceMethodName}(...)] ${record.message}\n"
        }
    }

    log.useParentHandlers = false
    log.addHandler(handler)

    val connection = DriverManager.getConnection("jdbc:sqlite:index.db")

    val sessions = SessionContext()

    val server = scan(log, hostname, port, "dev.scriptor")

    server.inject("log", log)
    server.inject("connection", connection)
    server.inject("sessions", sessions)

    connection.prepareStatement(
        """
        create table if not exists media (
            id string primary key,
            path string unique not null,
            modified timestamp not null
        )
        """.trimIndent()
    ).use { statement -> statement.execute() }

    val extensions = arrayOf("mkv", "mp4")

    val modified = Timestamp(System.currentTimeMillis())

    connection.prepareStatement(
        """
        insert into media (id, path, modified)
        values (?, ?, ?)
        on conflict(path)
        do update
        set modified = excluded.modified
        """.trimIndent()
    ).use { statement ->
        for (file in Path(data).walk()) {
            if (file.extension !in extensions) continue

            val id = Uuid.generateV7()

            statement.setString(1, id.toHexDashString())
            statement.setString(2, file.absolutePathString())
            statement.setTimestamp(3, modified)

            statement.addBatch()
        }

        statement.executeLargeBatch()
    }

    connection.prepareStatement("delete from media where not modified = ?").use { statement ->
        statement.setTimestamp(1, modified)
        statement.executeUpdate()
    }

    server.start()

    server.close()
    connection.close()
}
