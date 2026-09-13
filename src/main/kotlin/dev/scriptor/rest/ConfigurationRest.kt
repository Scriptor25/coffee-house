package dev.scriptor.rest

import dev.scriptor.context.AuthContext
import dev.scriptor.context.Configuration
import dev.scriptor.context.TmdbContext
import dev.scriptor.model.AuthorizationHeader
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.Provider
import dev.scriptor.server.UnauthorizedSignal
import dev.scriptor.server.jvm.annotation.Controller
import dev.scriptor.server.jvm.annotation.Get
import dev.scriptor.server.jvm.annotation.Header
import org.jetbrains.exposed.v1.jdbc.Database
import java.util.logging.Logger

@Controller("/configuration")
class ConfigurationRest {

    @Get("/tmdb")
    context(
        _: Logger,
        _: Provider,
        _: Database,
        auth: AuthContext,
        tmdb: TmdbContext,
    )
    fun getTmdb(@Header authorization: AuthorizationHeader? = null): Configuration {
        auth.auth(authorization)
            ?: throw UnauthorizedSignal()

        return tmdb.getConfiguration()
            ?: throw NotFoundSignal()
    }
}
