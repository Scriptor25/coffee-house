package dev.scriptor.rest

import dev.scriptor.context.AuthContext
import dev.scriptor.context.MediaContext
import dev.scriptor.model.Bearer
import dev.scriptor.model.Media
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.UnauthorizedSignal
import dev.scriptor.server.annotation.*
import dev.scriptor.server.http.result.HTTPResult
import dev.scriptor.server.http.result.HTTPResultString
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.io.readBytes
import kotlin.time.Clock.System.now
import kotlin.use

@Endpoint("/")
class DashboardRest {

    @Inject("data")
    lateinit var data: String

    @Inject("auth")
    lateinit var auth: AuthContext

    @Inject("media")
    lateinit var media: MediaContext

    @Resource("/favicon.[]")
    fun getFavicon() {
    }

    @Resource("/[slug+]", result = "text/html")
    fun getDashboard(
        @PathParameter("slug") slug: Array<String>,
        @Header("authorization") bearer: Bearer,
    ): HTTPResult<*> {
        val slug = Path("/", *slug)

        val now = now()
        val session = auth.auth(bearer.token, now)
            ?: throw UnauthorizedSignal()

        session.access = now

        val items = media.getAllMedia()

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

            if (node == null) {
                throw NotFoundSignal(content = "no node for path $slug")
            }

            val entries = node.children.sortedBy { it.name }.map {
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
                .getSystemResourceAsStream("dashboard.html")
                .use { it!!.readBytes() }.decodeToString()

            document = template.format(list)
        } else document = "no media items found."

        return HTTPResultString(value = document)
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
}
