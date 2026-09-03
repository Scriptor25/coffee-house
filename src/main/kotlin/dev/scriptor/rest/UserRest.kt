package dev.scriptor.rest

import dev.scriptor.JsonNode
import dev.scriptor.context.AuthContext
import dev.scriptor.get
import dev.scriptor.model.Authorization
import dev.scriptor.model.user.User
import dev.scriptor.model.user.UserRole
import dev.scriptor.server.ForbiddenSignal
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.UnauthorizedSignal
import dev.scriptor.server.jvm.annotation.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.logging.Logger
import kotlin.uuid.Uuid

@Suppress("unused")
@Controller("/user")
class UserRest {

    @Get("/", result = "application/json")
    context(
        _: Logger,
        database: Database,
        auth: AuthContext,
    )
    fun getUsers(@Header authorization: Authorization? = null): List<User> {
        val session = auth.auth(authorization)
            ?: throw UnauthorizedSignal()

        return when (session.role) {
            UserRole.ADMIN -> transaction(database) { User.all().toList() }
            UserRole.USER -> {
                val self = transaction(database) { User.findById(session.id!!) }
                listOfNotNull(self)
            }
        }
    }

    @Post("/", "application/json", "application/json")
    context(
        _: Logger,
        database: Database,
        auth: AuthContext,
    )
    fun createUser(
        @Header authorization: Authorization? = null,
        @Body value: JsonNode,
    ): User {
        val session = auth.auth(authorization)
            ?: throw UnauthorizedSignal()

        if (session.role != UserRole.ADMIN) {
            throw ForbiddenSignal()
        }

        val username: String = value["username"].get()
        val password: String = value["password"].get()
        val role: String = value["role"].get()

        return transaction(database) {
            User.new {
                this.name = username
                this.hash = password // TODO: generate password hash
                this.role = UserRole.valueOf(role)
            }
        }
    }

    @Get("/[id]", result = "application/json")
    context(
        _: Logger,
        database: Database,
        auth: AuthContext,
    )
    fun getUser(
        @PathParameter id: Uuid,
        @Header authorization: Authorization? = null,
    ): User {
        val session = auth.auth(authorization)
            ?: throw UnauthorizedSignal()

        if (session.role != UserRole.ADMIN && session.id != id) {
            throw ForbiddenSignal()
        }

        return transaction(database) { User.findById(id) }
            ?: throw NotFoundSignal()
    }

    @Put("/[id]", "application/json", "application/json")
    context(
        _: Logger,
        database: Database,
        auth: AuthContext,
    )
    fun updateUser(
        @PathParameter id: Uuid,
        @Header authorization: Authorization? = null,
        @Body value: JsonNode,
    ): User {
        val session = auth.auth(authorization)
            ?: throw UnauthorizedSignal()

        if (session.role != UserRole.ADMIN && session.id != id) {
            throw ForbiddenSignal()
        }

        val username: String = value["username"].get()
        val role: String = value["role"].get()

        // TODO: separate route for updating password

        return transaction(database) {
            User.findByIdAndUpdate(id) {
                it.name = username
                it.role = UserRole.valueOf(role)
            }
        } ?: throw NotFoundSignal()
    }

    @Delete("/[id]", result = "application/json")
    context(
        _: Logger,
        database: Database,
        auth: AuthContext,
    )
    fun deleteUser(
        @PathParameter id: Uuid,
        @Header authorization: Authorization? = null,
    ): User {
        val session = auth.auth(authorization)
            ?: throw UnauthorizedSignal()

        if (session.role != UserRole.ADMIN && session.id != id) {
            throw ForbiddenSignal()
        }

        return transaction(database) {
            User.findByIdAndUpdate(id) {
                it.delete()
            }
        } ?: throw NotFoundSignal()
    }
}
