package dev.scriptor.rest

import dev.scriptor.model.Bearer
import dev.scriptor.server.annotation.Endpoint
import dev.scriptor.server.annotation.Header
import dev.scriptor.server.annotation.Resource
import dev.scriptor.server.http.HTTPMethod

@Endpoint("/user")
class UserRest {

    @Resource("/", method = HTTPMethod.POST)
    fun createUser(@Header("authorization") authorization: Bearer) {
    }
}
