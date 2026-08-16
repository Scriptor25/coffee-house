package dev.scriptor.rest

import dev.scriptor.model.Bearer
import dev.scriptor.server.annotation.Controller
import dev.scriptor.server.annotation.Header
import dev.scriptor.server.annotation.Post

@Controller("/user")
class UserRest {

    @Post("/")
    fun createUser(@Header authorization: Bearer) {
    }
}
