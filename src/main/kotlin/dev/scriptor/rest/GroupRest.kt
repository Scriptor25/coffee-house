package dev.scriptor.rest

import dev.scriptor.context.PlaybackContext
import dev.scriptor.db
import dev.scriptor.model.PlaybackItem
import dev.scriptor.model.show.Group
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.jvm.annotation.Controller
import dev.scriptor.server.jvm.annotation.PathParameter
import dev.scriptor.server.jvm.annotation.Post
import dev.scriptor.server.security.Principal
import kotlin.uuid.Uuid

@Controller("/resource/group")
class GroupRest {

    @Post("/[id]/playback", "*/*", "text/plain")
    context(principal: Principal, context: PlaybackContext)
    fun createGroupPlayback(@PathParameter id: Uuid): String {
        val group = db { Group.findById(id) }
            ?: throw NotFoundSignal()

        val items = db {
            group.episodes.map {
                PlaybackItem(it.media.id.value, it.title)
            }
        }

        return context.createPlayback(
            principal.id,
            group.title,
            items,
        )
    }
}
