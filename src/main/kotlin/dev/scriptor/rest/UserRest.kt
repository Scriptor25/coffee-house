package dev.scriptor.rest

import dev.scriptor.model.CreateUserBody
import dev.scriptor.model.OffsetLimitBody
import dev.scriptor.model.UpdateUserBody
import dev.scriptor.model.user.User
import dev.scriptor.model.user.UserRole
import dev.scriptor.server.ForbiddenSignal
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.jvm.annotation.*
import dev.scriptor.server.security.Principal
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.uuid.Uuid

@RequireAuth
@Controller("/resource/user")
class UserRest {

    @RequireRole(UserRole.ADMIN)
    @Post("/", "application/json", "application/json")
    context(database: Database)
    fun createUser(@Body body: CreateUserBody): User {
        return transaction(database) {
            User.new {
                this.name = body.username
                this.hash = body.password // TODO: generate password hash
                this.role = body.role
            }
        }
    }

    @Get("/[id]", "application/json")
    context(principal: Principal, database: Database)
    fun getUser(@PathParameter id: Uuid): User {
        if (UserRole.ADMIN !in principal.roles && principal.id != id) {
            throw ForbiddenSignal()
        }

        return transaction(database) { User.findById(id) }
            ?: throw NotFoundSignal()
    }

    @Put("/[id]", "application/json", "application/json")
    context(principal: Principal, database: Database)
    fun updateUser(
        @PathParameter id: Uuid,
        @Body body: UpdateUserBody,
    ): User {
        if (UserRole.ADMIN !in principal.roles && principal.id != id) {
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
    context(principal: Principal, database: Database)
    fun deleteUser(@PathParameter id: Uuid): User {
        if (UserRole.ADMIN !in principal.roles && principal.id != id) {
            throw ForbiddenSignal()
        }

        return transaction(database) {
            User.findByIdAndUpdate(id) {
                it.delete()
            }
        } ?: throw NotFoundSignal()
    }

    @Post("/list", "application/json", "application/json")
    context(principal: Principal, database: Database)
    fun getUserList(@Body body: OffsetLimitBody = OffsetLimitBody()): List<User> {
        return when {
            UserRole.ADMIN in principal.roles -> transaction(database) {
                User
                    .all()
                    .offset(body.offset)
                    .limit(body.limit)
                    .toList()
            }

            else -> {
                val self = transaction(database) { User.findById(principal.id) }
                listOfNotNull(self)
            }
        }
    }
}
