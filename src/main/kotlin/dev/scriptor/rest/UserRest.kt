package dev.scriptor.rest

import dev.scriptor.context.AuthContext
import dev.scriptor.model.AuthorizationHeader
import dev.scriptor.model.CreateUserBody
import dev.scriptor.model.OffsetLimitBody
import dev.scriptor.model.UpdateUserBody
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

    @Post("/", "application/json", "application/json")
    context(
        _: Logger,
        database: Database,
        auth: AuthContext,
    )
    fun createUser(
        @Header authorization: AuthorizationHeader? = null,
        @Body body: CreateUserBody,
    ): User {
        val session = auth.auth(authorization)
            ?: throw UnauthorizedSignal()

        if (session.role != UserRole.ADMIN) {
            throw ForbiddenSignal()
        }

        return transaction(database) {
            User.new {
                this.name = body.username
                this.hash = body.password // TODO: generate password hash
                this.role = body.role
            }
        }
    }

    @Get("/[id]", "application/json")
    context(
        _: Logger,
        database: Database,
        auth: AuthContext,
    )
    fun getUser(
        @PathParameter id: Uuid,
        @Header authorization: AuthorizationHeader? = null,
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
        @Header authorization: AuthorizationHeader? = null,
        @Body body: UpdateUserBody,
    ): User {
        val session = auth.auth(authorization)
            ?: throw UnauthorizedSignal()

        if (session.role != UserRole.ADMIN && session.id != id) {
            throw ForbiddenSignal()
        }

        // TODO: route for updating password

        return transaction(database) {
            User.findByIdAndUpdate(id) {
                it.name = body.username
                it.role = body.role
            }
        } ?: throw NotFoundSignal()
    }

    @Delete("/[id]", "application/json")
    context(
        _: Logger,
        database: Database,
        auth: AuthContext,
    )
    fun deleteUser(
        @PathParameter id: Uuid,
        @Header authorization: AuthorizationHeader? = null,
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

    @Post("/list", "application/json", "application/json")
    context(
        _: Logger,
        database: Database,
        auth: AuthContext,
    )
    fun getUserList(
        @Header authorization: AuthorizationHeader? = null,
        @Body body: OffsetLimitBody = OffsetLimitBody(),
    ): List<User> {
        val session = auth.auth(authorization)
            ?: throw UnauthorizedSignal()

        return when (session.role) {
            UserRole.ADMIN -> transaction(database) {
                User
                    .all()
                    .offset(body.offset)
                    .limit(body.limit)
                    .toList()
            }

            UserRole.USER -> {
                val self = transaction(database) { User.findById(session.id!!) }
                listOfNotNull(self)
            }
        }
    }
}
