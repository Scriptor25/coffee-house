package dev.scriptor

import dev.scriptor.model.Media
import dev.scriptor.model.Session
import dev.scriptor.model.User
import dev.scriptor.server.http.HTTPServer
import dev.scriptor.server.scan
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.sql.DriverManager
import java.sql.Timestamp
import java.util.logging.*
import kotlin.io.path.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

fun getenv(): Map<String, String> = System.getenv()

@OptIn(ExperimentalUuidApi::class)
fun main() {
    val env = getenv()

    val hostname = env["HOSTNAME"] ?: "0.0.0.0"
    val port = env["PORT"]?.toInt() ?: 8080
    val data = env["DATA"] ?: "/data"
    val username = env["USERNAME"]
    val password = env["PASSWORD"]

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

    SQL().createTable<User>().execute(connection)
    SQL().createTable<Session>().execute(connection)
    SQL().createTable<Media>().execute(connection)

    val extensions = arrayOf("mkv", "mp4")

    connection.prepareStatement(
        """
        insert into media (id, path, title, created_at, modified_at)
        values (?, ?, ?, ?, ?)
        on conflict(path)
        do update set title = excluded.title, created_at = excluded.created_at, modified_at = excluded.modified_at
        """.trimIndent()
    ).use { statement ->
        for (file in Path(data).walk()) {
            if (file.extension !in extensions) continue

            val id = Uuid.generateV7()

            val attributes = Files.readAttributes(file, BasicFileAttributes::class.java)

            val size = attributes.size()

            val createdAt = attributes.creationTime()
            val modifiedAt = attributes.lastModifiedTime()

            statement.setString(1, id.toHexDashString())
            statement.setString(2, file.absolutePathString())
            statement.setString(3, file.nameWithoutExtension)
            statement.setTimestamp(4, Timestamp.from(createdAt.toInstant()))
            statement.setTimestamp(5, Timestamp.from(modifiedAt.toInstant()))

            statement.addBatch()
        }

        statement.executeLargeBatch()
    }

    connection.prepareStatement("select * from media").use { statement ->
        statement.executeQuery().use { result ->
            while (result.next()) {
                val path = Path(result.getString("path"))
                if (path.notExists()) {
                    result.deleteRow()
                }
            }
        }
    }

    HTTPServer(log, hostname, port).use { server ->
        scan(server, "dev.scriptor")

        server.inject("hostname", hostname)
        server.inject("port", port)
        server.inject("data", data)
        server.inject("username", username)
        server.inject("password", password)

        server.inject("log", log)
        server.inject("connection", connection)

        server.start()
    }

    connection.close()
}
