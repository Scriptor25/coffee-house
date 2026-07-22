package dev.scriptor

import dev.scriptor.server.scan
import java.sql.DriverManager
import java.util.logging.Logger

fun main() {
    val log = Logger.getLogger("dev.scriptor")

    val connection = DriverManager.getConnection("jdbc:sqlite:./index.db")

    val server = scan(log, "0.0.0.0", 8080, "dev.scriptor")

    server.registerValue("log", log)
    server.registerValue("connection", connection)

    server.start()

    server.close()
    connection.close()
}
