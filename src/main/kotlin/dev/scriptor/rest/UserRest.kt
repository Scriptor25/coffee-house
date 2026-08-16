package dev.scriptor.rest

import dev.scriptor.JsonNode
import dev.scriptor.context.AuthContext
import dev.scriptor.get
import dev.scriptor.model.Bearer
import dev.scriptor.model.User
import dev.scriptor.model.UserRole
import dev.scriptor.server.ForbiddenSignal
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.UnauthorizedSignal
import dev.scriptor.server.annotation.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.uuid.Uuid

@Controller("/user")
class UserRest {

    @Get("/", result = "application/json")
    context(
        database: Database,
        auth: AuthContext,
    )
    fun getUsers(@Header authorization: Bearer): List<User> {
        val session = auth.auth(authorization.token)
            ?: throw UnauthorizedSignal()

        val current = session.user

        return if (current == null || current.role == UserRole.ADMIN) {
            transaction(database) { User.all().toList() }
        } else {
            listOf(current)
        }
    }

    @Post("/", "application/json", "application/json")
    context(
        database: Database,
        auth: AuthContext,
    )
    fun createUser(@Header authorization: Bearer, @Body value: JsonNode): User {
        val session = auth.auth(authorization.token)
            ?: throw UnauthorizedSignal()

        val current = session.user
        if (current != null && current.role != UserRole.ADMIN) {
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
        database: Database,
        auth: AuthContext,
    )
    fun getUser(@PathParameter id: Uuid, @Header authorization: Bearer): User {
        val session = auth.auth(authorization.token)
            ?: throw UnauthorizedSignal()

        val current = session.user
        if (
            current != null
            && current.role != UserRole.ADMIN
            && current.id.value != id
        ) {
            throw ForbiddenSignal()
        }

        return transaction(database) { User.findById(id) }
            ?: throw NotFoundSignal()
    }

    @Put("/[id]", "application/json", "application/json")
    context(
        database: Database,
        auth: AuthContext,
    )
    fun updateUser(@PathParameter id: Uuid, @Header authorization: Bearer, @Body value: JsonNode): User {
        val session = auth.auth(authorization.token)
            ?: throw UnauthorizedSignal()

        val current = session.user
        if (
            current != null
            && current.role != UserRole.ADMIN
            && current.id.value != id
        ) {
            throw ForbiddenSignal()
        }

        val username: String = value["username"].get()
        val role: String = value["role"].get()

        // TODO: only update role if current.role is higher than user.role
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
        database: Database,
        auth: AuthContext,
    )
    fun deleteUser(@PathParameter id: Uuid, @Header authorization: Bearer): User {
        val session = auth.auth(authorization.token)
            ?: throw UnauthorizedSignal()

        val current = session.user
        if (
            current != null
            && current.role != UserRole.ADMIN
            && current.id.value != id
        ) {
            throw ForbiddenSignal()
        }

        return transaction(database) {
            User.findByIdAndUpdate(id) {
                it.delete()
            }
        } ?: throw NotFoundSignal()
    }
}
