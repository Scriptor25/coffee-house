package dev.scriptor.rest

import dev.scriptor.context.SessionContext
import dev.scriptor.model.Cookie
import dev.scriptor.model.Media
import dev.scriptor.model.Session
import dev.scriptor.model.getMedia
import dev.scriptor.server.annotation.*
import dev.scriptor.server.http.result.HTTPResult
import dev.scriptor.server.http.result.HTTPResultString
import java.net.URI
import java.nio.file.Path
import java.sql.Connection
import java.util.logging.Logger
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.io.readBytes
import kotlin.time.Clock.System.now
import kotlin.use
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Endpoint("/")
class DashboardRest {

    @Inject("data")
    lateinit var data: String

    @Inject("log")
    lateinit var log: Logger

    @Inject("connection")
    lateinit var connection: Connection

    @Inject("sessions")
    lateinit var sessions: SessionContext

    @Resource("/favicon.[]")
    fun getFavicon() {
    }

    @Resource("/[slug+]", result = "text/html")
    @OptIn(ExperimentalUuidApi::class)
    fun getDashboard(@PathParameter("slug") slug: Array<String>, @Header("cookie") cookie: Cookie?): HTTPResult<*> {

        val slug = Path("/", *slug)

        val session: Session
        if (cookie != null && "x-session-id" in cookie) {
            val id = cookie["x-session-id"]!!
            val uid = Uuid.parseHexDash(id)
            session =
                if (uid in sessions) sessions[uid]!!
                else sessions.create(uid)
        } else {
            val uid = Uuid.generateV7()
            session = sessions.create(uid)
        }

        session.access = now()

        val items = connection.prepareStatement("select * from media").use { statement ->
            statement.executeQuery().use {
                val list = mutableListOf<Media>()
                while (it.next()) {
                    list += it.getMedia()
                }
                list
            }
        }

        val document: String
        if (items.isNotEmpty()) {
            val tree = buildTree(items, Path(data))

            var node: DirectoryNode? = tree
            for (segment in slug) {
                if (node == null || segment.name == node.name) break
                node = node.children
                    .filterIsInstance<DirectoryNode>()
                    .find { it.name == segment.name }
            }

            val entries = node?.children.orEmpty().sortedBy { it.name }.map {
                when (it) {
                    is DirectoryNode -> """<li><a href="${
                        URI(
                            null,
                            null,
                            slug.resolve(it.name).toString(),
                            null,
                        ).rawPath
                    }">${it.name}</a></li>"""

                    is MediaNode -> """<li><a href="/media/${it.item.id}?session=${session.id}">${it.name}</a></li>"""
                }
            }

            val list = """<ul><li><a href="${
                URI(
                    null,
                    null,
                    slug.parent?.toString(),
                    null,
                ).rawPath
            }">..</a></li>${entries.joinToString("")}</ul>"""

            val template = ClassLoader
                .getSystemResourceAsStream("list.html")
                .use { it!!.readBytes() }.decodeToString()

            document = template.format(list)
        } else document = "no media items found."

        val headers: MutableMap<String, String> = HashMap()
        headers["set-cookie"] = "x-session-id=${session.id}"

        return HTTPResultString(
            headers = headers,
            value = document,
        )
    }

    private sealed class FileNode(val name: String)

    private class DirectoryNode(name: String) : FileNode(name) {
        val children = mutableListOf<FileNode>()
    }

    private class MediaNode(name: String, val item: Media) : FileNode(name)

    private fun buildTree(items: List<Media>, base: Path): DirectoryNode {
        val tree = DirectoryNode("")

        for (item in items) {
            var current = tree
            val relative = base.relativize(item.path)

            for (i in 0 until relative.nameCount - 1) {
                val part = relative.getName(i).toString()

                var child = current.children
                    .filterIsInstance<DirectoryNode>()
                    .find { it.name == part }

                if (child == null) {
                    child = DirectoryNode(part)
                    current.children += child
                }

                current = child
            }

            current.children += MediaNode(
                relative.fileName.toString(),
                item
            )
        }

        return tree
    }

    private fun commonBasePath(items: List<Media>): Path {
        var common = items.first().path

        for ((_, path, _) in items.drop(1)) {
            var i = 0
            val max = minOf(common.nameCount, path.nameCount)

            while (i < max && common.getName(i) == path.getName(i)) {
                i++
            }

            common = common.root.resolve(
                common.subpath(0, i)
            )

            if (common.nameCount == 0) {
                break
            }
        }

        return common
    }
}
