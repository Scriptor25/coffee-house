package dev.scriptor

import dev.scriptor.server.scan
import java.io.File
import java.sql.DriverManager
import java.sql.Timestamp
import java.util.logging.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun main() {
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

    val server = scan(log, "0.0.0.0", 8080, "dev.scriptor")

    server.inject("log", log)
    server.inject("connection", connection)

    connection.prepareStatement(
        """
        create table if not exists media (
            id string primary key,
            path string unique not null,
            modified timestamp not null
        )
        """.trimIndent()
    ).use { it.execute() }

    val extensions = arrayOf("mkv", "mp4")
    val walk = File("/home/felix/Videos").walkTopDown()

    val modified = Timestamp(System.currentTimeMillis())

    connection.prepareStatement(
        """
        insert into media (id, path, modified)
        values (?, ?, ?)
        on conflict(path)
        do update
        set modified = excluded.modified
        """.trimIndent()
    ).use {
        for (file in walk) {
            if (file.extension !in extensions) continue

            val id = Uuid.generateV7()

            it.setString(1, id.toHexDashString())
            it.setString(2, file.absolutePath)
            it.setTimestamp(3, modified)

            it.addBatch()
        }

        it.executeLargeBatch()
    }

    connection.prepareStatement(
        """
        delete from media
        where not modified = ?
        """.trimIndent()
    ).use {
        it.setTimestamp(1, modified)
        it.executeUpdate()
    }

    connection.prepareStatement(
        """
        select id, path from media
        """.trimIndent()
    ).use { statement ->
        statement.executeQuery().use {
            while (it.next()) {
                val id = it.getString(1)
                val path = it.getString(2)

                log.info("media $id -> $path")
            }
        }
    }

    server.start()

    server.close()
    connection.close()
}
