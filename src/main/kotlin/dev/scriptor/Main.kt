package dev.scriptor

import dev.scriptor.context.SessionContext
import dev.scriptor.model.Media
import dev.scriptor.model.Session
import dev.scriptor.model.User
import dev.scriptor.server.Provider
import dev.scriptor.server.http.Server
import dev.scriptor.server.scan
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.sql.DriverManager
import java.util.logging.*
import kotlin.io.path.*
import kotlin.reflect.full.starProjectedType
import kotlin.time.toKotlinInstant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

fun getEnvironment(): Map<String, String> = System.getenv()

val EXTENSIONS = arrayOf("mkv", "mp4")

@OptIn(ExperimentalUuidApi::class)
fun main() {
    val env = getEnvironment()

    val hostname = env["HOSTNAME"] ?: "0.0.0.0"
    val port = env["PORT"]?.toInt() ?: 8080
    val data = env["DATA"] ?: "/data"
    val username = env["USERNAME"]
    val password = env["PASSWORD"]

    val provider = Provider()

    provider["hostname"] = hostname
    provider["port"] = port
    provider["data"] = data
    provider["username"] = username
    provider["password"] = password

    val log = Logger.getLogger("dev.scriptor")
    log.level = Level.ALL

    val handler = ConsoleHandler()
    handler.level = log.level
    handler.formatter = object : Formatter() {

        override fun format(record: LogRecord?): String? {
            if (record == null) return null

            return "[${record.level}][${record.instant}] ${record.message}\n"
        }
    }

    log.useParentHandlers = false
    log.addHandler(handler)

    provider += log

    val connection = DriverManager.getConnection("jdbc:sqlite:index.db")

    provider += connection

    Server(log, provider, hostname, port).use { server ->
        scan(server, "dev.scriptor")

        context(provider) {

            SQL(connection).create<User>().execute()
            SQL(connection).create<Session>().execute()
            SQL(connection).create<Media>().execute()

            SQL(connection)
                .insert<Media>()
                .conflict(Media::path) { sql ->
                    sql.update(
                        Media::size to excluded("size"),
                        Media::title to excluded("title"),
                        Media::createdAt to excluded("created_at"),
                        Media::modifiedAt to excluded("modified_at"),
                    )
                }
                .batch { submit ->
                    for (path in Path(data).walk()) {
                        if (path.extension !in EXTENSIONS) continue

                        val id = Uuid.generateV7()

                        val attributes = Files.readAttributes(path, BasicFileAttributes::class.java)

                        val size = attributes.size()

                        val createdAt = attributes.creationTime()
                        val modifiedAt = attributes.lastModifiedTime()

                        submit(
                            Media(
                                id,
                                path,
                                size,
                                path.nameWithoutExtension,
                                createdAt.toInstant().toKotlinInstant(),
                                modifiedAt.toInstant().toKotlinInstant(),
                            ),
                        )
                    }
                }

            SQL(connection).select<Media>().prepare().use { (statement) ->
                statement.executeQuery().use { result ->
                    while (result.next()) {
                        val path = Path(result.getString("path"))
                        if (path.notExists()) {
                            result.deleteRow()
                        }
                    }
                }
            }
        }

        server.register("session-reaper", 0L, 10L * 60L * 1000L) {
            val sessions = provider[SessionContext::class.starProjectedType] as SessionContext
            context(provider, connection) {
                sessions.getExpiredSessions().forEach { session -> sessions.deleteSession(session) }
            }
        }

        server.start()
    }

    connection.close()
}
