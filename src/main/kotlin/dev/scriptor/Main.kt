package dev.scriptor

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

fun getenv(name: String): String? = System.getenv(name)
fun getenv(name: String, default: String): String = System.getenv(name) ?: default

@OptIn(ExperimentalUuidApi::class)
fun main() {
    val hostname = getenv("HOSTNAME", "0.0.0.0")
    val port = getenv("PORT", "8080").toInt()
    val data = getenv("DATA", "/data")
    val username = getenv("USERNAME")
    val password = getenv("PASSWORD")

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

    connection.prepareStatement(
        """
        create table if not exists user (
            id uuid not null primary key,
            name string not null,
            hash string not null,
            role string not null
        )
        """.trimIndent()
    ).use { statement -> statement.execute() }

    connection.prepareStatement(
        """
        create table if not exists session (
            id uuid not null primary key,
            user_id uuid not null,
            token string not null,
            created_at timestamp not null,
            expires_at timestamp not null,
            access timestamp null,
            agent string null,
            sequence long not null,
            next long not null,
        
            constraint fk_user
            foreign key (user_id)
            references user(id)
        )
        """.trimIndent()
    ).use { statement -> statement.execute() }

    connection.prepareStatement(
        """
        create table if not exists media (
            id uuid not null primary key,
            path string unique not null,
            title string not null,
            created_at timestamp not null,
            modified_at timestamp not null
        )
        """.trimIndent()
    ).use { statement -> statement.execute() }

    val extensions = arrayOf("mkv", "mp4")

    val modified = Timestamp(System.currentTimeMillis())

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
